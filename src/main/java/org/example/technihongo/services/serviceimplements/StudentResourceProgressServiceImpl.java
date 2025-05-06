package org.example.technihongo.services.serviceimplements;

import org.example.technihongo.entities.LearningResource;
import org.example.technihongo.entities.Student;
import org.example.technihongo.entities.StudentDailyLearningLog;
import org.example.technihongo.entities.StudentResourceProgress;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.CompletionStatus;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.StudentResourceProgressService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class StudentResourceProgressServiceImpl implements StudentResourceProgressService {
    @Autowired
    private StudentResourceProgressRepository studentResourceProgressRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LearningResourceRepository learningResourceRepository;
    @Autowired
    private StudentDailyLearningLogRepository dailyLogRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @Override
    public void trackLearningResourceProgress(Integer studentId, Integer resourceId, String notes) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
        LearningResource learningResource = learningResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("LearningResource not found with ID: " + resourceId));

        Optional<StudentResourceProgress> existingProgressOpt = studentResourceProgressRepository
                .findByStudent_StudentIdAndLearningResource_ResourceId(studentId, resourceId);

        StudentResourceProgress progress;
        if (existingProgressOpt.isEmpty()) {
            progress = new StudentResourceProgress();
            progress.setStudent(student);
            progress.setLearningResource(learningResource);
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            progress.setLastStudied(LocalDateTime.now());
        } else {
            progress = existingProgressOpt.get();
            progress.setLastStudied(LocalDateTime.now());
        }

        studentResourceProgressRepository.save(progress);
    }

    @Override
    public void completeLearningResourceProgress(Integer studentId, Integer resourceId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
        learningResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("LearningResource not found with ID: " + resourceId));

        Optional<StudentResourceProgress> existingProgressOpt = studentResourceProgressRepository
                .findByStudent_StudentIdAndLearningResource_ResourceId(studentId, resourceId);

        StudentResourceProgress progress;
        if (existingProgressOpt.isEmpty()) {
            throw new RuntimeException("Learning Resource Progress not found!");
        } else {
            progress = existingProgressOpt.get();
            progress.setLastStudied(LocalDateTime.now());

            if (progress.getCompletionStatus() != CompletionStatus.COMPLETED) {
                progress.setCompletionStatus(CompletionStatus.COMPLETED);
                StudentDailyLearningLog dailyLog = dailyLogRepository.findByStudentStudentIdAndLogDate(studentId, LocalDate.now()).get();
                dailyLog.setCompletedResources(dailyLog.getCompletedResources() + 1);
                dailyLogRepository.save(dailyLog);
                userActivityLogService.trackUserActivityLog(userRepository.findByStudentStudentId(studentId).getUserId(),
                        ActivityType.COMPLETE, ContentType.LearningResource, resourceId, null, null);
            }
        }

        studentResourceProgressRepository.save(progress);
    }

    @Override
    public List<StudentResourceProgress> getAllStudentResourceProgress(Integer studentId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        return studentResourceProgressRepository.findByStudent_StudentId(studentId);
    }

    @Override
    public StudentResourceProgress viewStudentResourceProgress(Integer studentId, Integer resourceId) {
        studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));

        learningResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("LearningResource not found with ID: " + resourceId));

        return studentResourceProgressRepository
                .findByStudent_StudentIdAndLearningResource_ResourceId(studentId, resourceId)
                .orElseThrow(() -> new RuntimeException("Progress not found for student ID: " + studentId + " and resource ID: " + resourceId));
    }

    @Override
    public StudentResourceProgress writeNoteForLearningResource(Integer studentId, Integer resourceId, String note) {
        if(resourceId == null){
            throw new RuntimeException("LearningResource ID không thể null");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Student với ID: " + studentId));
        LearningResource learningResource = learningResourceRepository.findById(resourceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy LearningResource với ID: " + resourceId));

        Optional<StudentResourceProgress> existingProgressOpt = studentResourceProgressRepository
                .findByStudent_StudentIdAndLearningResource_ResourceId(studentId, resourceId);

        StudentResourceProgress progress;
        if (existingProgressOpt.isEmpty()) {
            progress = new StudentResourceProgress();
            progress.setStudent(student);
            progress.setLearningResource(learningResource);
            progress.setCompletionStatus(CompletionStatus.IN_PROGRESS);
            progress.setLastStudied(LocalDateTime.now());
        } else {
            progress = existingProgressOpt.get();
            progress.setLastStudied(LocalDateTime.now());

            if (note != null) {
                progress.setNotes(note);
            }
        }

        return studentResourceProgressRepository.save(progress);
    }
}
