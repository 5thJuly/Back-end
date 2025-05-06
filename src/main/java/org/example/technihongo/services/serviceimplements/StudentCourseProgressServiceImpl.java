package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.CourseStatisticsDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.enums.StudyPlanStatus;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.AchievementService;
import org.example.technihongo.services.interfaces.StudentCourseProgressService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Component
public class
StudentCourseProgressServiceImpl implements StudentCourseProgressService {
    @Autowired
    private StudentCourseProgressRepository studentCourseProgressRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private StudentSubscriptionRepository studentSubscriptionRepository;
    @Autowired
    private StudentStudyPlanRepository studentStudyPlanRepository;
    @Autowired
    private StudentLessonProgressRepository studentLessonProgressRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private StudentLearningStatisticsRepository statisticsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserActivityLogService userActivityLogService;
    private final AchievementService achievementService;

    @Override
    public StudentCourseProgress getStudentCourseProgress(Integer studentId, Integer courseId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        return studentCourseProgressRepository
                .findByStudent_StudentIdAndCourse_CourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Progress not found for student ID: " + studentId + " and course ID: " + courseId));
    }

    @Override
    public List<StudentCourseProgress> getAllStudentCourseProgress(Integer studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
        return studentCourseProgressRepository.findByStudent_StudentId(studentId);
    }

    @Override
    public CourseStatisticsDTO viewCourseStatistics(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        List<StudentCourseProgress> progresses = studentCourseProgressRepository.findByCourse_CourseId(courseId);

        long completedCount = studentCourseProgressRepository
                .countByCourse_CourseIdAndCompletionStatus(courseId, CompletionStatus.COMPLETED);

        int totalEnrollments = course.getEnrollmentCount();

        BigDecimal averageCompletionPercentage;
        if (totalEnrollments == 0) {
            averageCompletionPercentage = BigDecimal.ZERO;
        } else {
            BigDecimal totalPercentage = progresses.stream()
                    .map(StudentCourseProgress::getCompletionPercentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            averageCompletionPercentage = totalPercentage.divide(BigDecimal.valueOf(totalEnrollments), 2, RoundingMode.HALF_UP);
        }

        BigDecimal completedPercentage;
        if (totalEnrollments == 0) {
            completedPercentage = BigDecimal.ZERO;
        } else {
            completedPercentage = BigDecimal.valueOf(completedCount)
                    .divide(BigDecimal.valueOf(totalEnrollments), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return CourseStatisticsDTO.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .domainName(course.getDomain().getName())
                .difficultyLevelTag(String.valueOf(course.getDifficultyLevel().getTag()))
                .estimatedDuration(course.getEstimatedDuration())
                .enrollmentCount(course.getEnrollmentCount())
                .publicStatus(course.isPublicStatus())
                .isPremium(course.isPremium())
                .createdAt(course.getCreatedAt())
                .completedCount((int) completedCount)
                .averageCompletionPercentage(averageCompletionPercentage)
                .completedPercentage(completedPercentage)
                .build();
    }

    @Override
    public void enrollCourse(Integer studentId, Integer courseId) {
        if(courseId == null){
            throw new RuntimeException("Course ID không thể null");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Student với ID: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học với ID: " + courseId));

        if (!course.isPublicStatus()) {
            throw new RuntimeException("Course đang bị khóa, không thể đăng kí tham gia!");
        }

        if (course.isPremium()) {
            boolean hasActiveSubscription = studentSubscriptionRepository
                    .existsByStudent_StudentIdAndIsActive(studentId, true);
            if (!hasActiveSubscription) {
                throw new RuntimeException("Bạn phải mua gói để có thể đăng kí tham gia khóa học cao cấp!");
            }
        }

        Optional<StudentCourseProgress> existingProgressOpt = studentCourseProgressRepository
                .findByStudent_StudentIdAndCourse_CourseId(studentId, courseId);

        if (existingProgressOpt.isEmpty()) {
            StudentCourseProgress progress = new StudentCourseProgress();
            progress.setStudent(student);
            progress.setCourse(course);
            progress.setCompletionPercentage(BigDecimal.ZERO);
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            progress.setCompletedLessons(0);
            progress.setTotalStudyDate(1);
            progress.setCurrentLesson(null);
            progress.setCompletedDate(null);

            studentCourseProgressRepository.save(progress);

            course.setEnrollmentCount(course.getEnrollmentCount() + 1);
            courseRepository.save(course);
        }

    }

    @Override
    @Transactional
    public void trackStudentCourseProgress(Integer studentId, Integer courseId, Integer currentLessonId) {
        StudentCourseProgress progress = studentCourseProgressRepository
                .findByStudent_StudentIdAndCourse_CourseId(studentId, courseId)
                .orElseThrow(() -> new RuntimeException("Student not yet enroll in this course!"));

        if (progress.getCompletionStatus().equals(CompletionStatus.COMPLETED)) {
            if (currentLessonId != null) {
                Lesson currentLesson = lessonRepository.findById(currentLessonId)
                        .orElseThrow(() -> new RuntimeException("Lesson not found"));
                progress.setCurrentLesson(currentLesson);
            }
            studentCourseProgressRepository.save(progress);
            return;
        }

        StudentStudyPlan activePlan = studentStudyPlanRepository.findActiveStudyPlanByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("No active study plan found"));
        if (!activePlan.getStudyPlan().getCourse().getCourseId().equals(courseId)) {
            throw new IllegalStateException("Active study plan does not match the course!");
        }

        List<StudentLessonProgress> lessonProgresses = studentLessonProgressRepository
                .findByStudentStudentIdAndLesson_StudyPlanStudyPlanId(studentId, activePlan.getStudyPlan().getStudyPlanId());
        int totalLessons = lessonRepository.countLessonByStudyPlan(activePlan.getStudyPlan());
        int completedLessons = (int) lessonProgresses.stream()
                .filter(p -> p.getCompletionStatus().equals(CompletionStatus.COMPLETED))
                .count();

        progress.setCompletedLessons(completedLessons);

        BigDecimal totalLessonPercentage = lessonProgresses.stream()
                .map(StudentLessonProgress::getCompletionPercentage)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completionPercentage = totalLessonPercentage
                .divide(BigDecimal.valueOf(totalLessons), 2, RoundingMode.HALF_UP);
        progress.setCompletionPercentage(completionPercentage);

        if (currentLessonId != null) {
            Lesson currentLesson = lessonRepository.findById(currentLessonId)
                    .orElseThrow(() -> new RuntimeException("Lesson not found"));
            progress.setCurrentLesson(currentLesson);
        }
        else {
            StudentLessonProgress latestInProgress = lessonProgresses.stream()
                    .filter(p -> p.getCompletionStatus().equals(CompletionStatus.IN_PROGRESS))
                    .min(Comparator.comparing(p -> p.getLesson().getLessonOrder()))
                    .orElse(null);

            if (latestInProgress != null) {
                progress.setCurrentLesson(latestInProgress.getLesson());
            } else {
                Lesson firstLesson = lessonRepository
                        .findByStudyPlan_StudyPlanIdOrderByLessonOrderAsc(activePlan.getStudyPlan().getStudyPlanId())
                        .stream()
                        .findFirst()
                        .orElse(null);
                progress.setCurrentLesson(firstLesson);
            }
        }

        LocalDateTime lastUpdate = progress.getUpdatedAt();
        LocalDateTime now = LocalDateTime.now();
        if (lastUpdate == null || !lastUpdate.toLocalDate().equals(now.toLocalDate())) {
            progress.setTotalStudyDate(progress.getTotalStudyDate() + 1);
        }

        if (completedLessons == totalLessons) {
            StudentStudyPlan studentStudyPlan = studentStudyPlanRepository.findActiveStudyPlanByStudentIdAndCourseId(studentId, courseId).get();
            studentStudyPlan.setStatus(StudyPlanStatus.COMPLETED);
            studentStudyPlanRepository.save(studentStudyPlan);

            progress.setCompletionStatus(CompletionStatus.COMPLETED);
            progress.setCompletedDate(LocalDateTime.now());

            StudentLearningStatistics statistics = statisticsRepository.findByStudentStudentId(studentId).get();
            statistics.setTotalCompletedCourses(statistics.getTotalCompletedCourses() + 1);
            statisticsRepository.save(statistics);

            userActivityLogService.trackUserActivityLog(userRepository.findByStudentStudentId(studentId).getUserId(), ActivityType.COMPLETE, ContentType.Course, courseId, null, null);
            achievementService.checkAndAssignCourseAchievements(studentId);
        }

        studentCourseProgressRepository.save(progress);
    }

    @Override
    public Boolean checkStudentCourseEnrollment(Integer studentId, Integer courseId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
        courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));

        return studentCourseProgressRepository.existsByStudentStudentIdAndCourseCourseId(studentId, courseId);
    }
}
