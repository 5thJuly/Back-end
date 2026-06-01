package org.example.technihongo.api;

import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.entities.StudentLessonProgress;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.StudentLessonProgressService;
import org.example.technihongo.services.interfaces.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lesson-progress")
public class StudentLessonProgressController {
    private final StudentLessonProgressService studentLessonProgressService;
    private final StudentService studentService;
    private final JwtUtil jwtUtil;

    @GetMapping("/view")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> viewAllStudentLessonProgressInStudyPlan(
            @RequestParam Integer studyPlanId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                List<StudentLessonProgress> progressList = studentLessonProgressService.viewAllStudentLessonProgressInStudyPlan(studentId, studyPlanId);

                if (progressList.isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("No lesson progress found for this student")
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Lesson progress retrieved successfully")
                            .data(progressList)
                            .build());
                }
            }  else {
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
                            .message("Failed to retrieve lesson progress: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/track")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> trackStudentLessonProgress(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Integer lessonId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                studentLessonProgressService.trackStudentLessonProgress(studentId, lessonId);

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Lesson Progress tracked successfully")
                        .data(null)
                        .build());
            } else {
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
                            .message("Failed to track lesson progress: " + e.getMessage())
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
