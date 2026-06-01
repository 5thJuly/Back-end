package org.example.technihongo.services.serviceimplements;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.UserActivityLogDTO;
import org.example.technihongo.entities.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.repositories.*;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserActivityLogServiceImpl implements UserActivityLogService {
    private final UserActivityLogRepository userActivityLogRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final QuizRepository quizRepository;
    private final SystemFlashcardSetRepository systemFlashcardSetRepository;
    private final StudentFlashcardSetRepository studentFlashcardSetRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    @Async
    @Override
    public void trackUserActivityLog(Integer userId, ActivityType activityType, ContentType contentType, Integer contentId, String ipAddress, String userAgent) {
        if (userId == null || activityType == null) {
            throw new IllegalArgumentException("User ID and Activity Type must not be null.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        String description = generateSpecificDescription(activityType, contentType, contentId);

        UserActivityLog log = UserActivityLog.builder()
                .user(user)
                .activityType(activityType)
                .contentType(contentType)
                .contentId(contentId)
                .description(description)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        userActivityLogRepository.save(log);
    }

    @Override
    public List<UserActivityLogDTO> getUserActivityLogs(Integer userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null.");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserActivityLog> logs = userActivityLogRepository.findByUser_UserId(userId, pageable);

        return logs.stream()
                .map(log -> UserActivityLogDTO.builder()
                        .logId(log.getLogId())
                        .activityType(log.getActivityType())
                        .contentType(log.getContentType())
                        .contentId(log.getContentId())
                        .description(log.getDescription())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<UserActivityLogDTO> getStudentActivityLogs(Integer userId, int page, int size) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không thể null.");
        }

        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + userId));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<ActivityType> allowedTypes = Arrays.asList(ActivityType.LOGIN, ActivityType.COMPLETE);
        Page<UserActivityLog> logs = userActivityLogRepository.findByUser_UserIdAndActivityTypeIn(userId, allowedTypes, pageable);

        return logs.stream()
                .map(log -> UserActivityLogDTO.builder()
                        .logId(log.getLogId())
                        .activityType(log.getActivityType())
                        .contentType(log.getContentType())
                        .contentId(log.getContentId())
                        .description(log.getDescription())
                        .createdAt(log.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private String generateSpecificDescription(ActivityType activityType, ContentType contentType, Integer contentId) {
        LocalDateTime now = LocalDateTime.now();
        String timeStr = now.format(TIME_FORMATTER);

        if (activityType == ActivityType.LOGIN && contentType == null) {
            return "Bạn đã đăng nhập lúc " + timeStr;
        }

        if (activityType == ActivityType.COMPLETE && contentId != null) {
            switch (contentType) {
                case Course:
                    Course course = courseRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("Course not found with ID: " + contentId));
                    return "Bạn đã hoàn thành khóa học " + course.getTitle() + " lúc " + timeStr;
                case Lesson:
                    Lesson lesson = lessonRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("Lesson not found with ID: " + contentId));
                    return "Bạn đã hoàn thành bài học " + lesson.getTitle() + " lúc " + timeStr;
                case LearningResource:
                    LearningResource resource = learningResourceRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("LearningResource not found with ID: " + contentId));
                    return "Bạn đã hoàn thành tài liệu học " + resource.getTitle() + " lúc " + timeStr;
                case Quiz:
                    Quiz quiz = quizRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("Quiz not found with ID: " + contentId));
                    return "Bạn đã hoàn thành bài Quiz " + quiz.getTitle() + " lúc " + timeStr;
                case SystemFlashcardSet:
                    SystemFlashcardSet systemSet = systemFlashcardSetRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("SystemFlashcardSet not found with ID: " + contentId));
                    return "Bạn đã hoàn thành bộ flashcard " + systemSet.getTitle() + " lúc " + timeStr;
                case StudentFlashcardSet:
                    StudentFlashcardSet studentSet = studentFlashcardSetRepository.findById(contentId)
                            .orElseThrow(() -> new RuntimeException("StudentFlashcardSet not found with ID: " + contentId));
                    return "Bạn đã hoàn thành bộ flashcard " + studentSet.getTitle() + " lúc " + timeStr;
                default:
                    break;
            }
        }

        StringBuilder desc = new StringBuilder("Người dùng đã thực hiện hành động ");
        desc.append(activityType.toString().toUpperCase().replace("_", " "));
        if (contentType != null && contentId != null) {
            desc.append(" trên ")
                    .append(contentType)
                    .append(" với ID: ")
                    .append(contentId);
        }
        else if (contentType != null) {
            desc.append(" trên ").append(contentType);
        }
        desc.append(" lúc ").append(timeStr);
        return desc.toString();
    }
}
