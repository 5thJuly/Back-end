package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.AdminOverviewDTO;
import org.example.technihongo.dto.LearningStatsDTO;
import org.example.technihongo.dto.QuizStatsDTO;
import org.example.technihongo.dto.StudentSpendingDTO;
import org.example.technihongo.entities.StudentDailyLearningLog;
import org.example.technihongo.entities.StudentQuizAttempt;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentSubscriptionRepository subscriptionRepository;
    @Autowired
    private StudentDailyLearningLogRepository studentDailyLearningLogRepository;
    @Autowired
    private StudentQuizAttemptRepository studentQuizAttemptRepository;

    @Override
    @Transactional(readOnly = true)
    public StudentSpendingDTO getStudentSpending(Integer studentId) {
        Double totalSpent = paymentTransactionRepository.sumByStudentId(studentId);
        return new StudentSpendingDTO(studentId, totalSpent);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOverviewDTO getAdminOverview() {
        AdminOverviewDTO dto = new AdminOverviewDTO();

        dto.setTotalStudents(studentRepository.count());
        dto.setTotalActiveCourses(courseRepository.countActiveCourses());
        dto.setTotalSubscriptionsSold(subscriptionRepository.count());

        LocalDateTime weekStart = LocalDate.now().minusDays(6).atStartOfDay();
        List<Object[]> dailyResults = paymentTransactionRepository.sumByDay(weekStart);
        Map<LocalDate, Double> dailyRevenueMap = dailyResults.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row[1] instanceof BigDecimal ? ((BigDecimal) row[1]).doubleValue() : ((Number) row[1]).doubleValue()
                ));

        List<AdminOverviewDTO.DailyRevenueDTO> weeklyRevenue = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            Double revenue = dailyRevenueMap.getOrDefault(date, 0.0);
            weeklyRevenue.add(new AdminOverviewDTO.DailyRevenueDTO(date.toString(), revenue));
        }
        weeklyRevenue.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        dto.setWeeklyRevenue(weeklyRevenue);

        LocalDateTime monthStart = LocalDate.now().minusWeeks(3).atStartOfDay();
        List<Object[]> weeklyResults = paymentTransactionRepository.sumByWeek(monthStart);
        Map<LocalDate, Double> weeklyRevenueMap = weeklyResults.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row[1] instanceof BigDecimal ? ((BigDecimal) row[1]).doubleValue() : ((Number) row[1]).doubleValue()
                ));

        List<AdminOverviewDTO.WeeklyRevenueDTO> monthlyRevenue = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 4; i++) {
            LocalDate weekEnd = LocalDate.now().minusWeeks(i);
            LocalDate weekBegin = weekEnd.minusDays(6);
            Double revenue = weeklyRevenueMap.getOrDefault(weekBegin, 0.0);
            String weekLabel = weekBegin.format(formatter) + " to " + weekEnd.format(formatter);
            monthlyRevenue.add(new AdminOverviewDTO.WeeklyRevenueDTO(weekLabel, revenue));
        }
        monthlyRevenue.sort((a, b) -> b.getWeek().compareTo(a.getWeek()));
        dto.setMonthlyRevenue(monthlyRevenue);

        LocalDateTime yearStart = LocalDate.now().minusMonths(11).atStartOfDay();
        List<Object[]> monthlyResults = paymentTransactionRepository.sumByMonth(yearStart);
        Map<LocalDate, Double> monthlyRevenueMap = monthlyResults.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row[1] instanceof BigDecimal ? ((BigDecimal) row[1]).doubleValue() : ((Number) row[1]).doubleValue()
                ));

        List<AdminOverviewDTO.MonthlyRevenueDTO> yearlyRevenue = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate month = LocalDate.now().minusMonths(i);
            LocalDate monthStartDate = month.withDayOfMonth(1);
            Double revenue = monthlyRevenueMap.getOrDefault(monthStartDate, 0.0);
            String monthLabel = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            yearlyRevenue.add(new AdminOverviewDTO.MonthlyRevenueDTO(monthLabel, revenue));
        }
        yearlyRevenue.sort((a, b) -> b.getMonth().compareTo(a.getMonth()));
        dto.setYearlyRevenue(yearlyRevenue);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public LearningStatsDTO getLearningStats(Integer studentId) {
        LearningStatsDTO dto = new LearningStatsDTO();

        LocalDate weekStart = LocalDate.now().minusDays(6);
        List<StudentDailyLearningLog> weeklyLogs = studentDailyLearningLogRepository.findByStudentIdAndDateRange(studentId, weekStart);
        Map<LocalDate, StudentDailyLearningLog> weeklyLogMap = weeklyLogs.stream()
                .collect(Collectors.toMap(StudentDailyLearningLog::getLogDate, log -> log));

        List<LearningStatsDTO.DailyStatsDTO> weeklyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            StudentDailyLearningLog log = weeklyLogMap.get(date);
            weeklyStats.add(new LearningStatsDTO.DailyStatsDTO(
                    date.toString(),
                    log != null ? log.getStudyTime() : 0,
                    log != null ? log.getCompletedLessons() : 0,
                    log != null ? log.getCompletedQuizzes() : 0,
                    log != null ? log.getCompletedResources() : 0,
                    log != null ? log.getCompletedFlashcardSets() : 0,
                    log != null && log.isDailyGoalAchieved()
            ));
        }
        weeklyStats.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        dto.setWeeklyStats(weeklyStats);

        LocalDate monthStart = LocalDate.now().minusWeeks(3);
        List<Object[]> weeklyResults = studentDailyLearningLogRepository.sumByWeek(studentId, monthStart);
        Map<LocalDate, Object[]> weeklyStatsMap = weeklyResults.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row
                ));

        List<LearningStatsDTO.WeeklyStatsDTO> monthlyStats = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = 0; i < 4; i++) {
            LocalDate weekEnd = LocalDate.now().minusWeeks(i);
            LocalDate weekBegin = weekEnd.minusDays(6);
            Object[] stats = weeklyStatsMap.get(weekBegin);
            String weekLabel = weekBegin.format(formatter) + " to " + weekEnd.format(formatter);
            monthlyStats.add(new LearningStatsDTO.WeeklyStatsDTO(
                    weekLabel,
                    stats != null ? ((Number) stats[1]).intValue() : 0,
                    stats != null ? ((Number) stats[2]).intValue() : 0,
                    stats != null ? ((Number) stats[3]).intValue() : 0,
                    stats != null ? ((Number) stats[4]).intValue() : 0,
                    stats != null ? ((Number) stats[5]).intValue() : 0
            ));
        }
        monthlyStats.sort((a, b) -> b.getWeek().compareTo(a.getWeek()));
        dto.setMonthlyStats(monthlyStats);

        LocalDate yearStart = LocalDate.now().minusMonths(11).withDayOfMonth(1);
        List<Object[]> monthlyResults = studentDailyLearningLogRepository.sumByMonth(studentId, yearStart);
        Map<LocalDate, Object[]> monthlyStatsMap = monthlyResults.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row
                ));

        List<LearningStatsDTO.MonthlyStatsDTO> yearlyStats = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDate month = LocalDate.now().minusMonths(i);
            LocalDate monthStartDate = month.withDayOfMonth(1);
            Object[] stats = monthlyStatsMap.get(monthStartDate);
            String monthLabel = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            yearlyStats.add(new LearningStatsDTO.MonthlyStatsDTO(
                    monthLabel,
                    stats != null ? ((Number) stats[1]).intValue() : 0,
                    stats != null ? ((Number) stats[2]).intValue() : 0,
                    stats != null ? ((Number) stats[3]).intValue() : 0,
                    stats != null ? ((Number) stats[4]).intValue() : 0,
                    stats != null ? ((Number) stats[5]).intValue() : 0
            ));
        }
        yearlyStats.sort((a, b) -> b.getMonth().compareTo(a.getMonth()));
        dto.setYearlyStats(yearlyStats);

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuizStatsDTO> getWeeklyQuizStats(Integer studentId) {
        LocalDateTime startDate = LocalDate.now().minusDays(6).atStartOfDay();
        List<Object[]> results = studentQuizAttemptRepository.findAverageScoreByDay(studentId, startDate);

        Map<LocalDate, Double> scoreMap = results.stream()
                .collect(Collectors.toMap(
                        row -> row[0] instanceof Timestamp ? ((Timestamp) row[0]).toLocalDateTime().toLocalDate() : ((Date) row[0]).toLocalDate(),
                        row -> row[1] instanceof BigDecimal ? ((BigDecimal) row[1]).doubleValue() : ((Number) row[1]).doubleValue()
                ));

        List<QuizStatsDTO> weeklyStats = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate date = LocalDate.now().minusDays(i);
            Double avgScore = scoreMap.getOrDefault(date, 0.0);
            weeklyStats.add(new QuizStatsDTO(date.toString(), avgScore));
        }

        weeklyStats.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return weeklyStats;
    }
}
