package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.entities.StudentDailyLearningLog;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.StudentDailyLearningLogService;
import org.example.technihongo.services.interfaces.StudentService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/learning-log")
public class StudentDailyLearningLogController {
    @Autowired
    private StudentDailyLearningLogService studentDailyLearningLogService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StudentService studentService;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @PostMapping("/track")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> trackStudentDailyLearningLog(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "0") Integer studyTime){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                studentDailyLearningLogService.trackStudentDailyLearningLog(studentId, studyTime);
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Daily Learning Log Recorded Successfully!")
                        .build());
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Unauthorized")
                                .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Recorded Daily Learning Log Failed: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/view")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> getStudentDailyLearningLog(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                StudentDailyLearningLog log = studentDailyLearningLogService.getStudentDailyLearningLog(studentId);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        userId,
                        ActivityType.VIEW,
                        ContentType.StudentDailyLearningLog,
                        log.getLogId(),
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .data(log)
                        .message("Nhật ký học tập hằng ngày truy xuất thành công!")
                        .build());
            }
            else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Unauthorized")
                                .build());
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Get Daily Learning Log Failed: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }
}
