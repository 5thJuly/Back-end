package org.example.technihongo.services.serviceimplements;

import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.PaymentMethod;
import org.example.technihongo.entities.PaymentTransaction;
import org.example.technihongo.entities.StudentSubscription;
import org.example.technihongo.entities.SubscriptionPlan;
import org.example.technihongo.enums.PaymentMethodCode;
import org.example.technihongo.enums.PaymentMethodType;
import org.example.technihongo.enums.TransactionStatus;
import org.example.technihongo.repositories.PaymentMethodRepository;
import org.example.technihongo.repositories.PaymentTransactionRepository;
import org.example.technihongo.repositories.StudentSubscriptionRepository;
import org.example.technihongo.repositories.SubscriptionPlanRepository;
import org.example.technihongo.services.interfaces.MomoService;
import org.example.technihongo.services.interfaces.StudentSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional
public class StudentSubscriptionServiceImpl implements StudentSubscriptionService  {

    private static final Logger log = LoggerFactory.getLogger(StudentSubscriptionServiceImpl.class);

    private final StudentSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MomoService momoService;
    private final JavaMailSender mailSender;


    @Override
    public RenewSubscriptionResponseDTO initiateRenewal(Integer studentId, RenewSubscriptionRequestDTO request) {
        // Kiểm tra số lần gia hạn
        List<StudentSubscription> allSubscriptions = subscriptionRepository
                .findAllByStudent_StudentIdAndIsActiveTrueOrEndDateAfter(studentId, LocalDateTime.now(), Pageable.unpaged());
        long renewalCount = allSubscriptions.size() - 1;
        if (renewalCount >= 3) {
            throw new RuntimeException("Hãy dành thời gian và học thật kỹ khóa học trước khi gia hạn thêm nhé");
        }

        StudentSubscription currentSubscription = subscriptionRepository
                .findByStudent_StudentIdAndIsActiveTrue(studentId);
        if (currentSubscription == null) {
            throw new RuntimeException("No active subscription found for student ID: " + studentId);
        }

        List<PaymentTransaction> pendingTransactions = paymentTransactionRepository
                .findBySubscription_SubscriptionIdAndTransactionStatus(
                        currentSubscription.getSubscriptionId(), TransactionStatus.PENDING);
        if (!pendingTransactions.isEmpty()) {
            throw new RuntimeException("Đang có giao dịch đang chờ xử lý cho gói đăng ký này. Vui lòng kiểm tra lại sau.");
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getSubPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + request.getSubPlanId()));

        PaymentMethod momoMethod = paymentMethodRepository.findByCode(PaymentMethodCode.MOMO_QR);
        if (momoMethod == null || !momoMethod.getName().equals(PaymentMethodType.MomoPay) || !momoMethod.isActive()) {
            throw new IllegalStateException("MoMo payment method is not available or inactive");
        }

        String orderId = "RENEW-" + System.currentTimeMillis() + "-" + request.getSubPlanId();

        PaymentTransaction transaction = PaymentTransaction.builder()
                .subscription(currentSubscription)
                .paymentMethod(momoMethod)
                .transactionAmount(plan.getPrice())
                .transactionStatus(TransactionStatus.PENDING)
                .externalOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        paymentTransactionRepository.save(transaction);

        String orderInfo = "Gia hạn gói: " + plan.getName();
        CreateMomoResponse momoResponse = momoService.createPaymentQR(orderId, orderInfo, plan.getPrice().longValue());

        return RenewSubscriptionResponseDTO.builder()
                .payUrl(momoResponse.getPayUrl())
                .transactionId(transaction.getTransactionId())
                .build();
    }

    @Override
    public void handleRenewalMoMo(MomoCallbackDTO callback, Map<String, String> requestParams) {
        String orderId = callback.getOrderId();
        log.info("Handling MoMo renewal callback for orderId: {}", orderId);

        PaymentTransaction transaction = paymentTransactionRepository.findByExternalOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Transaction not found for orderId: " + orderId));

        if (transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            log.warn("Transaction expired before callback: orderId={}", orderId);
            return;
        }

        if ("0".equals(callback.getResultCode())) {
            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
            transaction.setPaymentDate(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            StudentSubscription currentSubscription = transaction.getSubscription();
            log.info("Current subscription found: ID={}, EndDate={}",
                    currentSubscription.getSubscriptionId(), currentSubscription.getEndDate());

            // Trích xuất subPlanId từ externalOrderId
            String[] orderIdParts = orderId.split("-");
            if (orderIdParts.length < 3) {
                throw new RuntimeException("Invalid externalOrderId format: " + orderId);
            }
            Integer subPlanId;
            try {
                subPlanId = Integer.parseInt(orderIdParts[orderIdParts.length - 1]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid subPlanId in externalOrderId: " + orderId);
            }

            // Lấy SubscriptionPlan từ subPlanId
            SubscriptionPlan plan = subscriptionPlanRepository.findById(subPlanId)
                    .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + subPlanId));
            log.info("Using SubscriptionPlan with subPlanId: {}", subPlanId);

            LocalDateTime latestEndDate = findLatestEndDate(currentSubscription.getStudent().getStudentId());
            StudentSubscription newSubscription = StudentSubscription.builder()
                    .student(currentSubscription.getStudent())
                    .subscriptionPlan(plan) // Sử dụng plan từ subPlanId
                    .startDate(latestEndDate)
                    .endDate(latestEndDate.plusDays(plan.getDurationDays()))
                    .isActive(false)
                    .build();

            StudentSubscription savedSubscription = subscriptionRepository.save(newSubscription);

            log.info("New subscription created: ID={}, StudentID={}, PlanID={}, StartDate={}, EndDate={}, IsActive={}",
                    savedSubscription.getSubscriptionId(),
                    savedSubscription.getStudent().getStudentId(),
                    savedSubscription.getSubscriptionPlan().getSubPlanId(),
                    savedSubscription.getStartDate(),
                    savedSubscription.getEndDate(),
                    savedSubscription.getIsActive());

            transaction.setSubscription(savedSubscription);
            paymentTransactionRepository.save(transaction);

            if (currentSubscription.getEndDate().isBefore(LocalDateTime.now())) {
                currentSubscription.setIsActive(false);
                subscriptionRepository.save(currentSubscription);

                savedSubscription.setIsActive(true);
                subscriptionRepository.save(savedSubscription);

                log.info("Old subscription deactivated, new subscription activated for student: {}",
                        currentSubscription.getStudent().getStudentId());
            } else {
                log.info("New subscription created (pending) for student: {}",
                        currentSubscription.getStudent().getStudentId());
            }
        } else {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            log.warn("Payment failed or canceled for transaction: {}. ResultCode: {}, Message: {}",
                    transaction.getTransactionId(), callback.getResultCode(), callback.getMessage());
        }
    }

    private LocalDateTime findLatestEndDate(Integer studentId) {
        List<StudentSubscription> allSubscriptions = subscriptionRepository.findAllByStudent_StudentId(studentId, Pageable.unpaged()).getContent();
        return allSubscriptions.stream()
                .map(StudentSubscription::getEndDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    @Override
    public PageResponseDTO<SubscriptionHistoryDTO> getSubscriptionHistory(Integer studentId, int pageNo, int pageSize, String sortBy, String sortDir) {
        if (studentId == null) {
            throw new IllegalArgumentException("Student ID must not be null.");
        }

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(pageNo, pageSize, sort);

        Page<StudentSubscription> subscriptions = subscriptionRepository.findAllByStudent_StudentId(studentId, pageable);

        List<SubscriptionHistoryDTO> result = subscriptions.getContent().stream()
                .map(sub -> {
                    List<PaymentTransaction> transactions = paymentTransactionRepository
                            .findAllBySubscription_SubscriptionIdAndTransactionStatusOrderByPaymentDateDesc(sub.getSubscriptionId(), TransactionStatus.COMPLETED);
                    PaymentTransaction latestTransaction = transactions.isEmpty() ? null : transactions.get(0);

                    String groupStatus = determineGroupStatus(sub, latestTransaction);

                    return SubscriptionHistoryDTO.builder()
                            .subscriptionId(sub.getSubscriptionId())
                            .planName(sub.getSubscriptionPlan().getName())
                            .startDate(sub.getStartDate())
                            .endDate(sub.getEndDate())
                            .amount(latestTransaction != null ? latestTransaction.getTransactionAmount() : BigDecimal.ZERO)
                            .paymentMethod(latestTransaction != null ? latestTransaction.getPaymentMethod().getName().name() : "N/A")
                            .status(sub.getIsActive())
                            .groupStatus(groupStatus)
                            .build();
                })
                .collect(Collectors.toList());

        return PageResponseDTO.<SubscriptionHistoryDTO>builder()
                .content(result)
                .pageNo(subscriptions.getNumber())
                .pageSize(subscriptions.getSize())
                .totalElements(subscriptions.getTotalElements())
                .totalPages(subscriptions.getTotalPages())
                .last(subscriptions.isLast())
                .build();
    }

    @Override
    public StudentSubscription getCurrentSubscriptionByStudentId(Integer studentId) {
        List<StudentSubscription> activeSubscriptions = subscriptionRepository.findAllByStudent_StudentIdAndIsActiveTrue(studentId);

        if (activeSubscriptions.isEmpty()) {
            return null;
        }
        return activeSubscriptions.stream()
                .max(Comparator.comparing(StudentSubscription::getEndDate))
                .orElse(null);
    }

    private String determineGroupStatus(StudentSubscription sub, PaymentTransaction latestTransaction) {
        LocalDateTime now = LocalDateTime.now();
        if (sub.getIsActive()) {
            return "Đang hoạt động";
        } else if (latestTransaction == null) {
            return "Không hoạt động/Hết hạn";
        } else if (sub.getStartDate().isAfter(now)) {
            return "Sắp mở";
        } else {
            return "Không hoạt động/Hết hạn";
        }
    }




    @Override
    @Scheduled(cron = "0 0 9 * * *")
    public void sendExpirationReminders() {
        LocalDateTime threshold = LocalDateTime.now().plusDays(7);
        List<StudentSubscription> expiringSubscriptions = subscriptionRepository.findSubscriptionsExpiringSoon(threshold);

        for (StudentSubscription sub : expiringSubscriptions) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(sub.getStudent().getUser().getEmail());
                helper.setSubject("Thông báo gia hạn gói đăng ký");
                helper.setText(
                        String.format(
                                "Gói %s của bạn sẽ hết hạn vào %s. Hãy gia hạn để tiếp tục cùng TechNihongo nhé",
                                sub.getSubscriptionPlan().getName(),
                                sub.getEndDate(),
                                sub.getStudent().getStudentId(),
                                sub.getSubscriptionPlan().getSubPlanId()
                        ), true);
                mailSender.send(message);
                log.info("Sent expiration reminder to student: {}", sub.getStudent().getStudentId());
            } catch (Exception e) {
                log.error("Failed to send reminder to student {}: {}", sub.getStudent().getStudentId(), e.getMessage());
            }
        }
    }

    @Override
    public SubscriptionPlanStatisticsDTO getMostPopularSubscriptionPlan() {
        log.info("Fetching the most popular subscription plan");

        // Get all subscriptions and group by subscription plan
        List<StudentSubscription> subscriptions = subscriptionRepository.findAll();
        if (subscriptions.isEmpty()) {
            log.info("No subscriptions found");
            return SubscriptionPlanStatisticsDTO.builder()
                    .subPlanId(0)
                    .planName("N/A")
                    .purchaseCount(0L)
                    .totalRevenue(BigDecimal.ZERO)
                    .build();
        }

        // Count purchases per plan
        Map<SubscriptionPlan, Long> purchaseCountMap = subscriptions.stream()
                .collect(Collectors.groupingBy(StudentSubscription::getSubscriptionPlan, Collectors.counting()));

        // Find the plan with the most purchases
        Map.Entry<SubscriptionPlan, Long> mostPopularEntry = purchaseCountMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElseThrow(() -> new RuntimeException("Unable to determine most popular plan"));

        SubscriptionPlan mostPopularPlan = mostPopularEntry.getKey();
        Long purchaseCount = mostPopularEntry.getValue();

        // Calculate total revenue for the most popular plan
        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findAllBySubscription_SubscriptionPlanAndTransactionStatus(mostPopularPlan, TransactionStatus.COMPLETED);
        BigDecimal totalRevenue = transactions.stream()
                .map(PaymentTransaction::getTransactionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return SubscriptionPlanStatisticsDTO.builder()
                .subPlanId(mostPopularPlan.getSubPlanId())
                .planName(mostPopularPlan.getName())
                .purchaseCount(purchaseCount)
                .totalRevenue(totalRevenue)
                .build();
    }

    @Override
    public List<SubscriptionPlanStatisticsDTO> getRevenueBySubscriptionPlan() {
        log.info("Fetching revenue statistics for all subscription plans");

        List<SubscriptionPlan> plans = subscriptionPlanRepository.findAll();
        if (plans.isEmpty()) {
            log.info("No subscription plans found");
            return List.of();
        }

        return plans.stream().map(plan -> {
            // Count purchases for this plan
            Long purchaseCount = subscriptionRepository.countBySubscriptionPlan(plan);

            // Calculate total revenue for this plan
            List<PaymentTransaction> transactions = paymentTransactionRepository
                    .findAllBySubscription_SubscriptionPlanAndTransactionStatus(plan, TransactionStatus.COMPLETED);
            BigDecimal totalRevenue = transactions.stream()
                    .map(PaymentTransaction::getTransactionAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return SubscriptionPlanStatisticsDTO.builder()
                    .subPlanId(plan.getSubPlanId())
                    .planName(plan.getName())
                    .purchaseCount(purchaseCount)
                    .totalRevenue(totalRevenue)
                    .build();
        }).collect(Collectors.toList());
    }


    public List<RevenueByPeriodDTO> getRevenueByPeriod(String periodType) {
        log.info("Fetching revenue for period: {}", periodType);

        if (!List.of("WEEK", "MONTH", "QUARTER", "YEAR").contains(periodType.toUpperCase())) {
            throw new IllegalArgumentException("Invalid period type. Must be WEEK, MONTH, QUARTER, or YEAR.");
        }

        List<PaymentTransaction> transactions = paymentTransactionRepository
                .findAllByTransactionStatus(TransactionStatus.COMPLETED);
        if (transactions.isEmpty()) {
            log.info("No completed transactions found");
            return List.of();
        }

        // Filter transactions with non-null payment dates
        transactions = transactions.stream()
                .filter(tx -> tx.getPaymentDate() != null)
                .toList();

        if (transactions.isEmpty()) {
            log.info("No transactions with valid payment_date found");
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();
        List<RevenueByPeriodDTO> result = new ArrayList<>();

        switch (periodType.toUpperCase()) {
            case "WEEK":
                LocalDateTime weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).with(LocalTime.MIN);
                for (int i = 0; i < 7; i++) {
                    LocalDateTime dayStart = weekStart.plusDays(i).with(LocalTime.MIN);
                    LocalDateTime dayEnd = dayStart.with(LocalTime.MAX);
                    String periodLabel = dayStart.toLocalDate().toString(); // Format: YYYY-MM-DD
                    BigDecimal revenue = transactions.stream()
                            .filter(tx -> !tx.getPaymentDate().isBefore(dayStart) && !tx.getPaymentDate().isAfter(dayEnd))
                            .map(PaymentTransaction::getTransactionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(RevenueByPeriodDTO.builder()
                            .period(periodLabel)
                            .totalRevenue(revenue)
                            .build());
                }
                break;
            case "MONTH":
                LocalDateTime monthStart = now.withDayOfMonth(1).with(LocalTime.MIN);
                LocalDateTime monthEnd = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                LocalDateTime cursor = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                int weekIndex = 1;
                while (!cursor.isAfter(monthEnd)) {
                    final LocalDateTime weekStartCursor = cursor;
                    LocalDateTime weekEnd = weekStartCursor.plusDays(6).with(LocalTime.MAX);
                    if (weekEnd.isAfter(monthEnd)) {
                        weekEnd = monthEnd;
                    }
                    final LocalDateTime weekEndCursor = weekEnd;
                    String periodLabel = now.getYear() + "-M" + now.getMonthValue() + "-W" + weekIndex;
                    BigDecimal revenue = transactions.stream()
                            .filter(tx -> !tx.getPaymentDate().isBefore(weekStartCursor) && !tx.getPaymentDate().isAfter(weekEndCursor))
                            .map(PaymentTransaction::getTransactionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(RevenueByPeriodDTO.builder()
                            .period(periodLabel)
                            .totalRevenue(revenue)
                            .build());
                    cursor = weekEndCursor.plusSeconds(1).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
                    weekIndex++;
                }
                break;
            case "QUARTER":
                int quarter = (now.getMonthValue() - 1) / 3 + 1;
                int startMonth = (quarter - 1) * 3 + 1;
                for (int month = startMonth; month < startMonth + 3; month++) {
                    LocalDateTime quarterMonthStart = LocalDateTime.of(now.getYear(), month, 1, 0, 0);
                    LocalDateTime quarterMonthEnd = quarterMonthStart.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                    String periodLabel = now.getYear() + "-M" + month;
                    BigDecimal revenue = transactions.stream()
                            .filter(tx -> !tx.getPaymentDate().isBefore(quarterMonthStart) && !tx.getPaymentDate().isAfter(quarterMonthEnd))
                            .map(PaymentTransaction::getTransactionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(RevenueByPeriodDTO.builder()
                            .period(periodLabel)
                            .totalRevenue(revenue)
                            .build());
                }
                break;
            case "YEAR":
                for (int month = 1; month <= 12; month++) {
                    LocalDateTime yearMonthStart = LocalDateTime.of(now.getYear(), month, 1, 0, 0);
                    LocalDateTime yearMonthEnd = yearMonthStart.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);
                    String periodLabel = now.getYear() + "-M" + month;
                    BigDecimal revenue = transactions.stream()
                            .filter(tx -> !tx.getPaymentDate().isBefore(yearMonthStart) && !tx.getPaymentDate().isAfter(yearMonthEnd))
                            .map(PaymentTransaction::getTransactionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    result.add(RevenueByPeriodDTO.builder()
                            .period(periodLabel)
                            .totalRevenue(revenue)
                            .build());
                }
                break;
            default:
                throw new IllegalStateException("Unexpected period type: " + periodType);
        }

        return result;
    }

    @Scheduled(fixedRate = 86400000)
    @Transactional
    public void activatePendingSubscriptions() {
        List<StudentSubscription> activeSubscriptions = subscriptionRepository.findAllByIsActiveTrue();
        for (StudentSubscription sub : activeSubscriptions) {
            if (sub.getEndDate().isBefore(LocalDateTime.now())) {
                StudentSubscription pendingSub = subscriptionRepository.findByStudent_StudentIdAndIsActiveFalse(sub.getStudent().getStudentId());
                if (pendingSub != null) {
                    sub.setIsActive(false);
                    pendingSub.setIsActive(true);
                    subscriptionRepository.save(sub);
                    subscriptionRepository.save(pendingSub);
                    log.info("Activated pending subscription {} for student {}", pendingSub.getSubscriptionId(), sub.getStudent().getStudentId());
                }
            }
        }
    }
}



