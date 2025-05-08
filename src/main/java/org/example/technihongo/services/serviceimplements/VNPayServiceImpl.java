package org.example.technihongo.services.serviceimplements;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.PaymentRequestDTO;
import org.example.technihongo.dto.PaymentResponseDTO;
import org.example.technihongo.dto.RenewSubscriptionRequestDTO;
import org.example.technihongo.dto.RenewSubscriptionResponseDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.PaymentMethodCode;
import org.example.technihongo.enums.PaymentMethodType;
import org.example.technihongo.enums.TransactionStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.AchievementService;
import org.example.technihongo.services.interfaces.VNPayService;
import org.example.technihongo.services.interfaces.VNPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class VNPayServiceImpl implements VNPayService {

    private static final Logger logger = LoggerFactory.getLogger(VNPayServiceImpl.class);

    private final StudentRepository studentRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final StudentSubscriptionRepository studentSubscriptionRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final AchievementService achievementService;

    @Value("${payment.vnPay.url}")
    private String vnp_PayUrl;
    @Value("${payment.vnPay.returnUrl}")
    private String vnp_ReturnUrl;
    @Value("${payment.vnPay.tmnCode}")
    private String vnp_TmnCode;
    @Value("${payment.vnPay.secretKey}")
    private String secretKey;
    @Value("${payment.vnPay.version}")
    private String vnp_Version;
    @Value("${payment.vnPay.command}")
    private String vnp_Command;
    @Value("${payment.vnPay.orderType}")
    private String orderType;

    @Override
    public PaymentResponseDTO initiateVNPayPayment(Integer studentId, PaymentRequestDTO requestDTO, HttpServletRequest request) {
        logger.info("Initiating VNPay payment for studentId: {}, subPlanId: {}", studentId, requestDTO.getSubPlanId());

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        SubscriptionPlan plan = subscriptionPlanRepository.findById(requestDTO.getSubPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Subscription plan not found"));

        List<StudentSubscription> activeSubscriptions = studentSubscriptionRepository.findByStudentStudentIdAndIsActive(studentId, true);
        if (!activeSubscriptions.isEmpty()) {
            throw new IllegalStateException("Student already has an active subscription. Please renew or cancel existing subscription.");
        }

        List<PaymentTransaction> pendingTransactions = paymentTransactionRepository
                .findBySubscription_Student_StudentIdAndTransactionStatus(studentId, TransactionStatus.PENDING);
        if (!pendingTransactions.isEmpty()) {
            throw new IllegalStateException("Student has pending payment transactions. Please complete or cancel existing payment.");
        }

        // Create subscription (inactive until payment is confirmed)
        StudentSubscription subscription = StudentSubscription.builder()
                .student(student)
                .subscriptionPlan(plan)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(plan.getDurationDays()))
                .isActive(false)
                .build();
        subscription = studentSubscriptionRepository.save(subscription);

        PaymentMethod vnpayMethod = paymentMethodRepository.findByCode(PaymentMethodCode.VNPay_Bank);
        if (vnpayMethod == null || !vnpayMethod.getName().equals(PaymentMethodType.VNPay) || !vnpayMethod.isActive()) {
            throw new IllegalStateException("VNPay payment method is not available or inactive");
        }

        // Create transaction
        PaymentTransaction transaction = PaymentTransaction.builder()
                .subscription(subscription)
                .paymentMethod(vnpayMethod)
                .transactionAmount(plan.getPrice())
                .currency("VND")
                .transactionStatus(TransactionStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        transaction = paymentTransactionRepository.save(transaction);

        // Generate VNPay parameters
        Map<String, String> vnpParams = getVNPayConfig(vnp_ReturnUrl);
        String orderId = UUID.randomUUID().toString();
        vnpParams.put("vnp_Amount", String.valueOf(plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue()));
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Thanh toán SubscriptionPlan: " + plan.getName());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // Generate payment URL
        String queryUrl = VNPayUtil.getPaymentURL(vnpParams, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParams, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(secretKey, hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnp_PayUrl + "?" + queryUrl;

        transaction.setExternalOrderId(orderId);
        paymentTransactionRepository.save(transaction);

        return PaymentResponseDTO.builder()
                .transactionId(transaction.getTransactionId())
                .orderId(orderId)
                .payUrl(paymentUrl)
                .build();
    }

    @Override
    public void handleVNPayCallback(Map<String, String> requestParams) {
        logger.info("Handling VNPay callback for TxnRef: {}", requestParams.get("vnp_TxnRef"));

        // Find transaction by external order ID
        PaymentTransaction transaction = paymentTransactionRepository.findByExternalOrderId(requestParams.get("vnp_TxnRef"))
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for TxnRef: " + requestParams.get("vnp_TxnRef")));

        // Check if transaction is expired
        if (transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            logger.warn("Transaction expired for TxnRef: {}", requestParams.get("vnp_TxnRef"));
            return;
        }

        // Verify signature
        String vnpSecureHash = requestParams.get("vnp_SecureHash");
        requestParams.remove("vnp_SecureHash");
        String hashData = VNPayUtil.getPaymentURL(requestParams, false);
        String calculatedHash = VNPayUtil.hmacSHA512(secretKey, hashData);

        if (!calculatedHash.equals(vnpSecureHash)) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            logger.warn("Invalid signature for transactionId: {}", transaction.getTransactionId());
            throw new SecurityException("Invalid signature from VNPay callback");
        }

        TransactionStatus newStatus;
        String responseCode = requestParams.get("vnp_ResponseCode");
        try {
            if ("00".equals(responseCode)) {
                newStatus = TransactionStatus.COMPLETED;
                transaction.setPaymentDate(LocalDateTime.now());
                StudentSubscription subscription = transaction.getSubscription();
                subscription.setIsActive(true);
                subscription.setStartDate(LocalDateTime.now());
                subscription.setEndDate(LocalDateTime.now().plusDays(subscription.getSubscriptionPlan().getDurationDays()));
                studentSubscriptionRepository.save(subscription);
                logger.info("Subscription activated for transactionId: {}", transaction.getTransactionId());
                achievementService.checkAndAssignFirstPaymentAchievement(subscription.getStudent().getStudentId());
            } else {
                newStatus = TransactionStatus.FAILED;
                logger.warn("Payment failed. ResponseCode: {}, Message: {}", responseCode, requestParams.get("vnp_OrderInfo"));
            }
        } catch (Exception e) {
            newStatus = TransactionStatus.FAILED;
            logger.error("Error processing callback: {}", e.getMessage());
        }

        transaction.setTransactionStatus(newStatus);
        paymentTransactionRepository.save(transaction);
        logger.info("VNPay callback handled for transactionId: {}, new status: {}", transaction.getTransactionId(), newStatus);
    }

    @Override
    public RenewSubscriptionResponseDTO initiateRenewalVNPay(Integer studentId, RenewSubscriptionRequestDTO requestDTO, HttpServletRequest request) {
        logger.info("Initiating VNPay renewal for studentId: {}, subPlanId: {}", studentId, requestDTO.getSubPlanId());

        // Lấy tất cả subscriptions của student
        List<StudentSubscription> allSubscriptions = studentSubscriptionRepository
                .findAllByStudentStudentId(studentId, Pageable.unpaged());
        logger.info("Found {} subscriptions for studentId: {}", allSubscriptions.size(), studentId);

        // Tìm subscription đầu tiên (dựa trên startDate sớm nhất)
        Optional<StudentSubscription> firstSubscription = allSubscriptions.stream()
                .min(Comparator.comparing(StudentSubscription::getStartDate));
        if (firstSubscription.isEmpty()) {
            throw new RuntimeException("No subscriptions found for student ID: " + studentId);
        }

        // Đếm số lần gia hạn (subscriptions sau subscription đầu tiên)
        long renewalCount = allSubscriptions.stream()
                .filter(sub -> sub.getStartDate().isAfter(firstSubscription.get().getStartDate()))
                .count();
        logger.info("Calculated renewalCount: {} for studentId: {}", renewalCount, studentId);

        if (renewalCount >= 3) {
            throw new RuntimeException("Hãy dành thời gian và học thật kỹ khóa học trước khi gia hạn thêm nhé");
        }

        // Kiểm tra subscription hiện tại
        StudentSubscription currentSubscription = studentSubscriptionRepository
                .findByStudent_StudentIdAndIsActiveTrue(studentId);
        if (currentSubscription == null) {
            throw new RuntimeException("No active subscription found for student ID: " + studentId);
        }

        // Kiểm tra giao dịch đang chờ xử lý
        List<PaymentTransaction> pendingTransactions = paymentTransactionRepository
                .findBySubscription_Student_StudentIdAndTransactionStatus(studentId, TransactionStatus.PENDING);
        if (!pendingTransactions.isEmpty()) {
            throw new RuntimeException("There is already a pending renewal transaction for this subscription");
        }

        // Lấy subscription plan
        SubscriptionPlan plan = subscriptionPlanRepository.findById(requestDTO.getSubPlanId())
                .orElseThrow(() -> new RuntimeException("Subscription plan not found: " + requestDTO.getSubPlanId()));

        // Kiểm tra phương thức thanh toán
        PaymentMethod vnpayMethod = paymentMethodRepository.findByCode(PaymentMethodCode.VNPay_Bank);
        if (vnpayMethod == null || !vnpayMethod.getName().equals(PaymentMethodType.VNPay) || !vnpayMethod.isActive()) {
            throw new IllegalStateException("VNPay payment method is not available or inactive");
        }

        String orderId;
        do {
            orderId = "RENEW-" + UUID.randomUUID().toString();
        } while (paymentTransactionRepository.existsByExternalOrderId(orderId));

        // Tạo giao dịch
        PaymentTransaction transaction = PaymentTransaction.builder()
                .subscription(currentSubscription)
                .paymentMethod(vnpayMethod)
                .transactionAmount(plan.getPrice())
                .currency("VND")
                .transactionStatus(TransactionStatus.PENDING)
                .externalOrderId(orderId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
        transaction = paymentTransactionRepository.save(transaction);

        // Tạo tham số VNPay
        Map<String, String> vnpParams = getVNPayConfig(vnp_ReturnUrl);
        vnpParams.put("vnp_Amount", String.valueOf(plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue()));
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", "Gia hạn gói: " + plan.getName());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // Tạo URL thanh toán
        String queryUrl = VNPayUtil.getPaymentURL(vnpParams, true);
        String hashData = VNPayUtil.getPaymentURL(vnpParams, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(secretKey, hashData);
        queryUrl += "&vnp_SecureHash=" + vnpSecureHash;
        String paymentUrl = vnp_PayUrl + "?" + queryUrl;

        return RenewSubscriptionResponseDTO.builder()
                .payUrl(paymentUrl)
                .transactionId(transaction.getTransactionId())
                .build();
    }

    @Override
    public void handleRenewalVNPay(Map<String, String> requestParams) {
        String orderId = requestParams.get("vnp_TxnRef");
        logger.info("Handling VNPay renewal callback for orderId: {}", orderId);

        PaymentTransaction transaction = paymentTransactionRepository.findByExternalOrderId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found for orderId: " + orderId));

        if (transaction.getExpiresAt().isBefore(LocalDateTime.now())) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            logger.warn("Transaction expired before callback: orderId={}", orderId);
            return;
        }

        String vnpSecureHash = requestParams.get("vnp_SecureHash");
        requestParams.remove("vnp_SecureHash");
        String hashData = VNPayUtil.getPaymentURL(requestParams, false);
        String calculatedHash = VNPayUtil.hmacSHA512(secretKey, hashData);

        if (!calculatedHash.equals(vnpSecureHash)) {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            logger.warn("Invalid signature for transactionId: {}", transaction.getTransactionId());
            throw new SecurityException("Invalid signature from VNPay callback");
        }

        String responseCode = requestParams.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            transaction.setTransactionStatus(TransactionStatus.COMPLETED);
            transaction.setPaymentDate(LocalDateTime.now());
            paymentTransactionRepository.save(transaction);

            StudentSubscription currentSubscription = transaction.getSubscription();
            logger.info("Current subscription found: ID={}, EndDate={}",
                    currentSubscription.getSubscriptionId(), currentSubscription.getEndDate());

            // Tìm SubscriptionPlan dựa trên transaction_amount
            BigDecimal transactionAmount = transaction.getTransactionAmount();
            List<SubscriptionPlan> matchingPlans = subscriptionPlanRepository.findByPriceAndIsActiveTrue(transactionAmount);

            if (matchingPlans.isEmpty()) {
                logger.warn("No active subscription plan found for transaction amount: {}, falling back to current subscription plan", transactionAmount);
                matchingPlans = List.of(currentSubscription.getSubscriptionPlan());
            }
            if (matchingPlans.size() > 1) {
                throw new RuntimeException("Multiple subscription plans found for transaction amount: " + transactionAmount);
            }

            SubscriptionPlan plan = matchingPlans.get(0);
            logger.info("Using SubscriptionPlan with subPlanId: {}, price: {}",
                    plan.getSubPlanId(), plan.getPrice());

            LocalDateTime latestEndDate = findLatestEndDate(currentSubscription.getStudent().getStudentId());
            StudentSubscription newSubscription = StudentSubscription.builder()
                    .student(currentSubscription.getStudent())
                    .subscriptionPlan(plan) // Sử dụng plan từ transaction_amount
                    .startDate(latestEndDate)
                    .endDate(latestEndDate.plusDays(plan.getDurationDays()))
                    .isActive(false)
                    .build();

            StudentSubscription savedSubscription = studentSubscriptionRepository.save(newSubscription);

            logger.info("New subscription created: ID={}, StudentID={}, PlanID={}, StartDate={}, EndDate={}, IsActive={}",
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
                studentSubscriptionRepository.save(currentSubscription);

                savedSubscription.setIsActive(true);
                studentSubscriptionRepository.save(savedSubscription);

                logger.info("Old subscription deactivated, new subscription activated for student: {}",
                        currentSubscription.getStudent().getStudentId());
            } else {
                logger.info("New subscription created (pending) for student: {}",
                        currentSubscription.getStudent().getStudentId());
            }
        } else {
            transaction.setTransactionStatus(TransactionStatus.FAILED);
            paymentTransactionRepository.save(transaction);
            logger.warn("Payment failed or canceled for transaction: {}. ResponseCode: {}, Message: {}",
                    transaction.getTransactionId(), responseCode, requestParams.get("vnp_OrderInfo"));
        }
    }

    private LocalDateTime findLatestEndDate(Integer studentId) {
        List<StudentSubscription> allSubscriptions = studentSubscriptionRepository.findAllByStudent_StudentId(studentId, Pageable.unpaged()).getContent();
        return allSubscriptions.stream()
                .map(StudentSubscription::getEndDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
    }

    private Map<String, String> getVNPayConfig(String returnUrl) {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnp_Version);
        vnpParams.put("vnp_Command", vnp_Command);
        vnpParams.put("vnp_TmnCode", vnp_TmnCode);
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_OrderType", orderType);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);
        calendar.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", vnp_ExpireDate);

        return vnpParams;
    }
}