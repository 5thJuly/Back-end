package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.StudentDailyLearningLog;
import org.example.technihongo.entities.StudentLearningStatistics;
import org.example.technihongo.repositories.StudentDailyLearningLogRepository;
import org.example.technihongo.repositories.StudentLearningStatisticsRepository;
import org.example.technihongo.repositories.StudentRepository;
import org.example.technihongo.services.interfaces.StudentDailyLearningLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Component
public class StudentDailyLearningLogServiceImpl implements StudentDailyLearningLogService {
    @Autowired
    private StudentDailyLearningLogRepository dailyLogRepository;
    @Autowired
    private StudentLearningStatisticsRepository statsRepository;
    @Autowired
    private StudentRepository studentRepository;

    @Transactional
    @Override
    public void trackStudentDailyLearningLog(Integer studentId, Integer studyTimeInput) {
        LocalDate today = LocalDate.now();

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found!"));

        Optional<StudentDailyLearningLog> existingLogOpt = dailyLogRepository
                .findByStudentStudentIdAndLogDate(studentId, today);

        StudentDailyLearningLog dailyLog;
        boolean isNewLog = false;
        if (existingLogOpt.isPresent()) {
            dailyLog = existingLogOpt.get();
            updateDailyLog(dailyLog, studyTimeInput, student.getDailyGoal());
        } else {
            dailyLog = createNewDailyLog(student, today);
            updateDailyLog(dailyLog, studyTimeInput, student.getDailyGoal());
            isNewLog = true;
        }

        dailyLogRepository.save(dailyLog);

        updateLearningStatistics(student, dailyLog, isNewLog);
    }

    @Override
    public StudentDailyLearningLog getStudentDailyLearningLog(Integer studentId) {
        return dailyLogRepository.findByStudentStudentIdAndLogDate(studentId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("Student ID not found!"));
    }

    private StudentDailyLearningLog createNewDailyLog(Student student, LocalDate today) {
        StudentDailyLearningLog dailyLog = new StudentDailyLearningLog();
        dailyLog.setStudent(student);
        dailyLog.setLogDate(today);
        dailyLog.setStudyTime(0);
        dailyLog.setCompletedLessons(0);
        dailyLog.setCompletedQuizzes(0);
        dailyLog.setCompletedResources(0);
        dailyLog.setCompletedFlashcardSets(0);
        dailyLog.setDailyGoalAchieved(false);

        int streak = calculateStreak(student, today);
        dailyLog.setStreak(streak);

        return dailyLog;
    }

    private void updateDailyLog(StudentDailyLearningLog dailyLog, Integer studyTimeInput, Integer dailyGoal) {
        int newStudyTime = dailyLog.getStudyTime() + studyTimeInput;
        dailyLog.setStudyTime(newStudyTime);
        dailyLog.setDailyGoalAchieved(newStudyTime >= dailyGoal);
    }

    private int calculateStreak(Student student, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        Optional<StudentDailyLearningLog> yesterdayLogOpt = dailyLogRepository
                .findByStudentStudentIdAndLogDate(student.getStudentId(), yesterday);

        if (yesterdayLogOpt.isPresent() && yesterdayLogOpt.get().getStudyTime() > 0) {
            return yesterdayLogOpt.get().getStreak() + 1;
        }
        return 1;
    }

    private void updateLearningStatistics(Student student, StudentDailyLearningLog dailyLog, boolean isNewLog) {
        StudentLearningStatistics stats = statsRepository.findByStudentStudentId(student.getStudentId())
                .orElseGet(() -> {
                    StudentLearningStatistics newStats = new StudentLearningStatistics();
                    newStats.setStudent(student);
                    newStats.setTotalStudyTime(0);
                    newStats.setTotalCompletedCourses(0);
                    newStats.setTotalCompletedLessons(0);
                    newStats.setTotalCompletedQuizzes(0);
                    newStats.setActiveDaysCount(0);
                    newStats.setMaxDaysStreak(0);
                    newStats.setTotalAchievementsUnlocked(0);
                    return newStats;
                });

        stats.setTotalStudyTime(stats.getTotalStudyTime() + dailyLog.getStudyTime());
        if (isNewLog && dailyLog.getStudyTime() > 0) {
            stats.setActiveDaysCount(stats.getActiveDaysCount() + 1);
        }

        int currentStreak = dailyLog.getStreak();
        stats.setMaxDaysStreak(Math.max(stats.getMaxDaysStreak(), currentStreak));
        stats.setLastStudyDate(LocalDateTime.now());

        statsRepository.save(stats);
    }
}