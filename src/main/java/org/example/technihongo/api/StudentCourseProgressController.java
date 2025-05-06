package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.CourseStatisticsDTO;
import org.example.technihongo.dto.EnrollStudyPlanRequest;
import org.example.technihongo.dto.StudentStudyPlanDTO;
import org.example.technihongo.entities.StudentCourseProgress;
import org.example.technihongo.entities.StudentStudyPlan;
import org.example.technihongo.entities.StudyPlan;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course-progress")
public class StudentCourseProgressController {
    @Autowired
    private StudentCourseProgressService courseProgressService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StudentService studentService;
    @Autowired
    private UserActivityLogService userActivityLogService;
    @Autowired
    private StudyPlanService studyPlanService;
    @Autowired
    private StudentStudyPlanService studentStudyPlanService;

    @GetMapping("/view/{studentId}")
    public ResponseEntity<ApiResponse> getStudentCourseProgress(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer studentId,
            @RequestParam Integer courseId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer loginStudentId = studentService.getStudentIdByUserId(userId);

                StudentCourseProgress progress;
                if(loginStudentId != null && loginStudentId.equals(studentId)){
                    progress = courseProgressService.getStudentCourseProgress(studentId, courseId);
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Student course progress retrieved successfully")
                            .data(progress)
                            .build());
                }
                else if(loginStudentId != null){
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.builder()
                            .success(false)
                            .message("Student can not view others' progress!")
                            .build());
                }
                else {
                    progress = courseProgressService.getStudentCourseProgress(studentId, courseId);
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Student course progress retrieved successfully")
                            .data(progress)
                            .build());
                }
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
                            .message("Failed to retrieve student course progress: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/all/{studentId}")
    public ResponseEntity<ApiResponse> getAllStudentCourseProgress(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer studentId) {
        try {
            List<StudentCourseProgress> progressList = courseProgressService.getAllStudentCourseProgress(studentId);

            if (progressList.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("No course progress found for this student")
                        .build());
            } else {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Student course progress retrieved successfully")
                        .data(progressList)
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
                            .message("Failed to retrieve student course progress: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/statistics/{courseId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> viewCourseStatistics(
            @PathVariable Integer courseId) {
        try {
            CourseStatisticsDTO statistics = courseProgressService.viewCourseStatistics(courseId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Course statistics retrieved successfully")
                    .data(statistics)
                    .build());
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
                            .message("Failed to retrieve course statistics: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> enrollCourse(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @RequestParam Integer courseId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                courseProgressService.enrollCourse(studentId, courseId);
                StudyPlan studyPlan = studyPlanService.getDefaultStudyPlanByCourseId(courseId);
                StudentStudyPlanDTO dto = studentStudyPlanService.enrollStudentInStudyPlan(
                        new EnrollStudyPlanRequest(studentId, studyPlan.getStudyPlanId()));
                courseProgressService.trackStudentCourseProgress(studentId, courseId, null);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        userId,
                        ActivityType.ENROLL,
                        ContentType.Course,
                        courseId,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Đăng kí tham gia khóa học thành công")
                        .data(null)
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Không có quyền")
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
                            .message("Đăng kí tham gia khóa học thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/check-enroll")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> checkStudentCourseEnrollment(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Integer courseId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                Boolean enroll = courseProgressService.checkStudentCourseEnrollment(studentId, courseId);
                if(enroll) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Student has already enrolled in the course!")
                            .data(true)
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Student has NOT yet enrolled in the course!")
                            .data(false)
                            .build());
                }
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
                            .message("Failed to check student's enrollment: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> trackStudentCourseProgress(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Integer courseId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                courseProgressService.trackStudentCourseProgress(studentId, courseId, null);

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Course Progress tracked successfully")
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
                            .message("Failed to track course progress: " + e.getMessage())
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
