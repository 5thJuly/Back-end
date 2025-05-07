package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.StudentAchievementDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.*;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.AchievementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final StudentAchievementRepository studentAchievementRepository;
    private final StudentAchievementProgressRepository progressRepository;
    private final StudentDailyLearningLogRepository dailyLearningLogRepository;
    private final UserActivityLogRepository userActivityLogRepository;
    private final StudentFlashcardSetProgressRepository setProgressRepo;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final StudentSubscriptionRepository studentSubscriptionRepository;
    private final StudentFlashcardSetRepository studentFlashcardSetRepository;
    private final StudentCourseProgressRepository studentCourseProgressRepository;
    private final StudentFavoriteRepository studentFavoriteRepository;
    private final StudentLearningStatisticsRepository learningStatisticsRepository;

    private static final LocalTime NIGHT_OWL_TIME = LocalTime.of(22, 30);
    private static final LocalTime EARLY_BIRD_TIME = LocalTime.of(7, 30);

    @Override
    public List<Achievement> achievementList() {
        return achievementRepository.findAll();
    }

    private boolean lacksAchievement(Integer studentId, Integer achievementId) {
        return studentAchievementRepository
                .findByStudent_StudentIdAndAchievement_AchievementId(studentId, achievementId)
                .isEmpty();
    }

    @Override
    public void assignAchievementsToStudent(Integer studentId, LocalDateTime loginTime) {
        Optional<Achievement> nightOwlOpt = achievementRepository.findByBadgeName("Cú đêm thức khuya");
        if (nightOwlOpt.isPresent()) {
            Achievement nightOwl = nightOwlOpt.get();
            if (lacksAchievement(studentId, nightOwl.getAchievementId()) && isNightOwlTime(loginTime)) {
                awardAchievement(studentId, nightOwl.getAchievementId());
            }
        }

        Optional<Achievement> earlyBirdOpt = achievementRepository.findByBadgeName("Chim sâu dậy sớm");
        if (earlyBirdOpt.isPresent()) {
            Achievement earlyBird = earlyBirdOpt.get();
            if (lacksAchievement(studentId, earlyBird.getAchievementId()) && isEarlyBirdTime(loginTime)) {
                awardAchievement(studentId, earlyBird.getAchievementId());
            }
        }
    }

    private boolean isNightOwlTime(LocalDateTime loginTime) {
        LocalTime time = loginTime.toLocalTime();
        return time.isAfter(NIGHT_OWL_TIME) || time.equals(NIGHT_OWL_TIME);
    }

    private boolean isEarlyBirdTime(LocalDateTime loginTime) {
        LocalTime time = loginTime.toLocalTime();
        return time.isBefore(EARLY_BIRD_TIME.plusMinutes(30)) && time.isAfter(EARLY_BIRD_TIME.minusMinutes(30));
    }

    @Override
    public void trackAchievementProgress(Integer studentId) {
        Optional<UserActivityLog> lastLoginOpt = userActivityLogRepository
                .findTopByUser_UserIdAndActivityTypeOrderByCreatedAtDesc(studentId, ActivityType.LOGIN);
        if (lastLoginOpt.isPresent()) {
            LocalDateTime loginTime = lastLoginOpt.get().getCreatedAt();
            assignAchievementsToStudent(studentId, loginTime);
        }
    }

    @Override
    public StudentAchievementDTO awardAchievement(Integer studentId, Integer achievementId) {
        StudentAchievement studentAchievement = StudentAchievement.builder()
                .student(Student.builder().studentId(studentId).build())
                .achievement(Achievement.builder().achievementId(achievementId).build())
                .achievedAt(LocalDateTime.now())
                .build();

        StudentAchievement saved = studentAchievementRepository.save(studentAchievement);

        Achievement achievement = achievementRepository.findById(achievementId).orElse(null);
        if (achievement != null) {
            UserActivityLog activityLog = UserActivityLog.builder()
                    .user(User.builder().userId(studentId).build())
                    .activityType(ActivityType.ACHIEVEMENT_UNLOCKED)
                    .contentType(ContentType.StudentAchievement)
                    .contentId(achievementId)
                    .description("Đã mở khóa thành tựu: " + achievement.getBadgeName())
                    .createdAt(LocalDateTime.now())
                    .build();
            userActivityLogRepository.save(activityLog);

            StudentLearningStatistics statistics = learningStatisticsRepository.findByStudentStudentId(studentId).get();
            statistics.setTotalAchievementsUnlocked(statistics.getTotalAchievementsUnlocked() + 1);
            learningStatisticsRepository.save(statistics);
        }

        return new StudentAchievementDTO(
                saved.getStudentAchievementId(),
                saved.getStudent().getStudentId(),
                saved.getAchievement().getAchievementId(),
                saved.getAchievedAt()
        );
    }

    @Override
    public void checkAndAssignStreakAchievements(Integer studentId) {
        Optional<StudentDailyLearningLog> latestLogOpt = dailyLearningLogRepository
                .findTopByStudent_StudentIdOrderByLogDateDesc(studentId);
        if (latestLogOpt.isEmpty() || latestLogOpt.get().getStudyTime() <= 0) {
            return;
        }

        int currentStreak = latestLogOpt.get().getStreak();
        List<Achievement> streakAchievements = achievementRepository.findAll().stream()
                .filter(a -> a.getCategory() == Category.Streak && a.getConditionType() == ConditionType.DAYS_STREAK)
                .toList();

        for (Achievement achievement : streakAchievements) {
            Optional<StudentAchievementProgress> progressOpt = progressRepository
                    .findByStudent_StudentIdAndAchievement_AchievementId(studentId, achievement.getAchievementId());

            StudentAchievementProgress progress;
            if (progressOpt.isEmpty()) {
                progress = StudentAchievementProgress.builder()
                        .student(Student.builder().studentId(studentId).build())
                        .achievement(achievement)
                        .currentValue(currentStreak)
                        .requiredValue(achievement.getConditionValue())
                        .lastUpdated(LocalDateTime.now())
                        .build();
            } else {
                progress = progressOpt.get();
                progress.setCurrentValue(currentStreak);
                progress.setLastUpdated(LocalDateTime.now());
            }

            progressRepository.save(progress);

            if (lacksAchievement(studentId, achievement.getAchievementId()) && progress.getCurrentValue() >= progress.getRequiredValue()) {
                awardAchievement(studentId, achievement.getAchievementId());
            }
        }
    }

    @Override
    public void checkAndAssignFirstFlashcardAchievement(Integer studentId, Integer studentSetId) {
        Optional<Achievement> flashcardAchievementOpt = achievementRepository.findByBadgeName("Tập sự ghép thẻ");
        if (flashcardAchievementOpt.isEmpty()) {
            return;
        }

        Achievement flashcardAchievement = flashcardAchievementOpt.get();

        if (lacksAchievement(studentId, flashcardAchievement.getAchievementId())) {
            Optional<StudentFlashcardSetProgress> progressOpt = setProgressRepo.findByStudentStudentIdAndStudentFlashcardSet_StudentSetId(studentId, studentSetId);

            if (progressOpt.isPresent()) {
                StudentFlashcardSetProgress progress = progressOpt.get();
                if (progress.getCardStudied() > 0) {
                    awardAchievement(studentId, flashcardAchievement.getAchievementId());
                }
            }
        }
    }

    public void checkAndAssignFirstPaymentAchievement(Integer studentId) {
        Optional<Achievement> achievementOpt = achievementRepository.findByBadgeName("Khám phá TechNihongo");
        if (achievementOpt.isEmpty()) {
            return;
        }

        Achievement achievement = achievementOpt.get();

        if (lacksAchievement(studentId, achievement.getAchievementId())) {
            List<PaymentTransaction> completedTransactions = paymentTransactionRepository
                    .findBySubscription_Student_StudentIdAndTransactionStatus(studentId, TransactionStatus.COMPLETED);

            List<StudentSubscription> activeSubscriptions = studentSubscriptionRepository
                    .findByStudentStudentIdAndIsActive(studentId, true);

            if (completedTransactions.isEmpty() && activeSubscriptions.isEmpty()) {

                awardAchievement(studentId, achievement.getAchievementId());
            }
        }
    }

    public void checkAndAssignFirstFlashcardSetAchievement(Integer studentId) {
        Optional<Achievement> achievementOpt = achievementRepository.findByBadgeName("Nhà tạo học phần");
        if (achievementOpt.isEmpty()) {
            return;
        }
        Achievement achievement = achievementOpt.get();
        if (lacksAchievement(studentId, achievement.getAchievementId())) {
            long activeSetCount = studentFlashcardSetRepository.countByCreatorStudentIdAndIsDeletedFalse(studentId);
            if (activeSetCount <= 1) {
                awardAchievement(studentId, achievement.getAchievementId());
            }
        }
    }

    @Override
    public void checkAndAssignFlashcardAchievements(Integer studentId) {
        List<Achievement> flashcardAchievements = achievementRepository.findAll().stream()
                .filter(a -> a.getConditionType() == ConditionType.FLASHCARD_COMPLETED)
                .toList();

        long completedSets = setProgressRepo.countByStudentStudentIdAndStudentFlashcardSetNotNullAndCompletionStatus(
                studentId, CompletionStatus.COMPLETED);

        for (Achievement achievement : flashcardAchievements) {
            if (lacksAchievement(studentId, achievement.getAchievementId())) {
                // Lấy hoặc tạo tiến trình
                Optional<StudentAchievementProgress> progressOpt = progressRepository
                        .findByStudent_StudentIdAndAchievement_AchievementId(studentId, achievement.getAchievementId());

                StudentAchievementProgress progress;
                if (progressOpt.isEmpty()) {
                    progress = StudentAchievementProgress.builder()
                            .student(Student.builder().studentId(studentId).build())
                            .achievement(achievement)
                            .currentValue((int) completedSets)
                            .requiredValue(achievement.getConditionValue())
                            .lastUpdated(LocalDateTime.now())
                            .build();
                } else {
                    progress = progressOpt.get();
                    progress.setCurrentValue((int) completedSets);
                    progress.setLastUpdated(LocalDateTime.now());
                }

                progressRepository.save(progress);

                // Trao thành tựu nếu đạt yêu cầu
                if (progress.getCurrentValue() >= progress.getRequiredValue()) {
                    awardAchievement(studentId, achievement.getAchievementId());
                }
            }
        }
    }

    @Override
    public void checkAndAssignCourseAchievements(Integer studentId) {
        // Đếm số khóa học đã hoàn thành
        long completedCourses = studentCourseProgressRepository
                .countByStudent_StudentIdAndCompletionStatus(studentId, CompletionStatus.COMPLETED);

        // Lấy tất cả thành tựu COURSE_COMPLETE
        List<Achievement> courseAchievements = achievementRepository
                .findByConditionType(ConditionType.COURSE_COMPLETE);

        for (Achievement achievement : courseAchievements) {
            // Kiểm tra xem đã trao thành tựu chưa
            boolean lacksAchievement = lacksAchievement(studentId, achievement.getAchievementId());
            if (!lacksAchievement) {
                continue; // Bỏ qua nếu đã trao
            }

            // Lấy hoặc tạo StudentAchievementProgress
            Optional<StudentAchievementProgress> progressOpt = progressRepository
                    .findByStudent_StudentIdAndAchievement_AchievementId(studentId, achievement.getAchievementId());

            StudentAchievementProgress progress;
            if (progressOpt.isEmpty()) {
                progress = StudentAchievementProgress.builder()
                        .student(Student.builder().studentId(studentId).build())
                        .achievement(achievement)
                        .currentValue((int) completedCourses)
                        .requiredValue(achievement.getConditionValue())
                        .lastUpdated(LocalDateTime.now())
                        .build();
            } else {
                progress = progressOpt.get();
                progress.setCurrentValue((int) completedCourses);
                progress.setLastUpdated(LocalDateTime.now());
            }

            progressRepository.save(progress);

            // Trao thành tựu nếu đủ điều kiện
            if (progress.getCurrentValue() >= progress.getRequiredValue()) {
                awardAchievement(studentId, achievement.getAchievementId());
            }
        }
    }

    @Override
    public void checkAndAssignFirstFavoriteAchievement(Integer studentId) {
        Optional<Achievement> favoriteAchievementOpt = achievementRepository.findByBadgeName("Yêu từ cái nhìn đầu tiên");
        if (favoriteAchievementOpt.isEmpty()) {
            return;
        }

        Achievement favoriteAchievement = favoriteAchievementOpt.get();

        if (lacksAchievement(studentId, favoriteAchievement.getAchievementId())) {
            long favoriteCount = studentFavoriteRepository.countByStudent_StudentId(studentId);
            if (favoriteCount >= 1) {
                awardAchievement(studentId, favoriteAchievement.getAchievementId());
            }
        }
    }

    @Override
    public List<StudentAchievement> getStudentAchievements(Integer studentId) {
        return studentAchievementRepository.findByStudent_StudentId(studentId);
    }
}
