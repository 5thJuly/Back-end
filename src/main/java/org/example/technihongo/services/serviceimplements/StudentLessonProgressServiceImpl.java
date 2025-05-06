package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudentLessonProgressService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Component
public class StudentLessonProgressServiceImpl implements StudentLessonProgressService {
    @Autowired
    private StudentLessonProgressRepository studentLessonProgressRepository;
    @Autowired
    private LessonResourceRepository lessonResourceRepository;
    @Autowired
    private StudentQuizAttemptRepository studentQuizAttemptRepository;
    @Autowired
    private StudentFlashcardSetProgressRepository studentFlashcardSetProgressRepository;
    @Autowired
    private StudentResourceProgressRepository studentResourceProgressRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private SystemFlashcardSetRepository systemFlashcardSetRepository;
    @Autowired
    private LearningResourceRepository learningResourceRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private StudentDailyLearningLogRepository dailyLogRepository;
    @Autowired
    private StudentLearningStatisticsRepository statisticsRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private StudyPlanRepository studyPlanRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @Override
    public void trackStudentLessonProgress(Integer studentId, Integer lessonId) {
        StudentLessonProgress progress = studentLessonProgressRepository
                .findByStudentStudentIdAndLessonLessonId(studentId, lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson progress not found"));

//        if (!progress.getCompletionStatus().equals(CompletionStatus.IN_PROGRESS)) {
//            throw new IllegalStateException("Lesson is not in progress!");
//        }

        if (progress.getCompletionStatus().equals(CompletionStatus.COMPLETED)) {
            progress.setLastStudied(LocalDateTime.now());
            studentLessonProgressRepository.save(progress);
            return;
        }

        List<LessonResource> resources = lessonResourceRepository.findByLesson_LessonIdAndActiveOrderByTypeOrderAsc(lessonId, true);
        int totalItems = resources.size();
        int completedItems = 0;

        for (LessonResource resource : resources) {
            if (resource.getQuiz() != null) {
                Quiz quiz = quizRepository.findById(resource.getQuiz().getQuizId()).get();
                if (quiz.isPublic() && studentQuizAttemptRepository
                        .existsByStudentStudentIdAndQuizQuizIdAndIsPassedAndIsCompleted(studentId, resource.getQuiz().getQuizId(), true, true)) {
                    completedItems++;
                }
            } else if (resource.getSystemFlashCardSet() != null) {
                SystemFlashcardSet set = systemFlashcardSetRepository.findById(resource.getSystemFlashCardSet().getSystemSetId()).get();
                if (set.isPublic() && studentFlashcardSetProgressRepository
                        .existsByStudentStudentIdAndSystemFlashcardSetSystemSetIdAndCompletionStatus(studentId, resource.getSystemFlashCardSet().getSystemSetId(), CompletionStatus.COMPLETED)) {
                    completedItems++;
                }
            } else if (resource.getLearningResource() != null) {
                LearningResource lr = learningResourceRepository.findById(resource.getLearningResource().getResourceId()).get();
                if (lr.isPublic() && studentResourceProgressRepository
                        .existsByStudentStudentIdAndLearningResourceResourceIdAndCompletionStatus(studentId, resource.getLearningResource().getResourceId(), CompletionStatus.COMPLETED)) {
                    completedItems++;
                }
            }
        }

        progress.setCompletedItems(completedItems);
        BigDecimal percentage = BigDecimal.valueOf(completedItems)
                .divide(BigDecimal.valueOf(totalItems), 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        progress.setCompletionPercentage(percentage);
        progress.setLastStudied(LocalDateTime.now());

        if (percentage.compareTo(BigDecimal.valueOf(100)) == 0
                && !progress.getCompletionStatus().equals(CompletionStatus.COMPLETED)) {

            progress.setCompletionStatus(CompletionStatus.COMPLETED);

            StudentDailyLearningLog dailyLog = dailyLogRepository.findByStudentStudentIdAndLogDate(studentId, LocalDate.now()).get();
            dailyLog.setCompletedLessons(dailyLog.getCompletedLessons() + 1);
            dailyLogRepository.save(dailyLog);

            userActivityLogService.trackUserActivityLog(userRepository.findByStudentStudentId(studentId).getUserId(), ActivityType.COMPLETE, ContentType.Lesson, lessonId, null, null);

            StudentLearningStatistics statistics = statisticsRepository.findByStudentStudentId(studentId).get();
            statistics.setTotalCompletedLessons(statistics.getTotalCompletedLessons() + 1);
            statisticsRepository.save(statistics);

            StudyPlan studyPlan = progress.getLesson().getStudyPlan();
            if (!studyPlan.isDefault()) {
                Lesson nextLesson = lessonRepository.findFirstByStudyPlanStudyPlanIdAndLessonOrderGreaterThanOrderByLessonOrderAsc(
                        studyPlan.getStudyPlanId(), progress.getLesson().getLessonOrder());
                if (nextLesson != null) {
                    StudentLessonProgress nextProgress = studentLessonProgressRepository
                            .findByStudentStudentIdAndLessonLessonId(studentId, nextLesson.getLessonId())
                            .orElseThrow(() -> new RuntimeException("Next lesson progress not found"));
                    if (nextProgress.getCompletionStatus().equals(CompletionStatus.NOT_STARTED)) {
                        nextProgress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
                        studentLessonProgressRepository.save(nextProgress);
                    }
                }
            }
        }

        studentLessonProgressRepository.save(progress);
    }

    @Override
    public List<StudentLessonProgress> viewAllStudentLessonProgressInStudyPlan(Integer studentId, Integer studyPlanId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found!"));

        studyPlanRepository.findById(studyPlanId)
                .orElseThrow(() -> new RuntimeException("Study plan not found!"));

        return studentLessonProgressRepository.findByStudentStudentIdAndLesson_StudyPlanStudyPlanId(studentId, studyPlanId);
    }
}
