package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.StudentCourseRatingDTO;
import org.example.technihongo.dto.StudentCourseRatingRequest;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.StudentCourseRatingService;
import org.example.technihongo.services.interfaces.StudentService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.hibernate.query.sqm.UnknownPathException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/student-course-rating")
public class StudentCourseRatingController {
    @Autowired
    private StudentCourseRatingService studentCourseRatingService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StudentService studentService;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @PostMapping("/createRating")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> createRating(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @Valid @RequestBody StudentCourseRatingRequest request) {
        try {
            if(request.getRating() == null || request.getReview() == null){
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Review hoặc rating không thể null")
                                .build());
            }

            Integer studentId = extractStudentId(authorizationHeader);
            StudentCourseRatingDTO response = studentCourseRatingService.createRating(studentId, request);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.CREATE,
                    ContentType.StudentCourseRating,
                    response.getRatingId(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.builder()
                            .success(true)
                            .message("Đánh giá thành công")
                            .data(response)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/getRating/{ratingId}")
    public ResponseEntity<ApiResponse> getRatingById(@PathVariable Integer ratingId) {
        try {
            StudentCourseRatingDTO response = studentCourseRatingService.getRatingById(ratingId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Rating retrieved successfully")
                    .data(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/allRating")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> getAllRatings() {
        try {
            List<StudentCourseRatingDTO> response = studentCourseRatingService.getAllRatings();
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("All ratings retrieved successfully")
                    .data(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update/{ratingId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> updateRating(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @PathVariable Integer ratingId,
            @Valid @RequestBody StudentCourseRatingRequest request) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            StudentCourseRatingDTO response = studentCourseRatingService.updateRating(ratingId, studentId, request);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.UPDATE,
                    ContentType.StudentCourseRating,
                    ratingId,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Rating updated successfully")
                    .data(response)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{ratingId}")
    @PreAuthorize("hasAnyRole('ROLE_Student', 'ROLE_Administrator')")
    public ResponseEntity<ApiResponse> deleteRating(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @PathVariable Integer ratingId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 3) {
                    Integer studentId = extractStudentId(authorizationHeader);
                    StudentCourseRatingDTO rating = studentCourseRatingService.getRatingById(ratingId);
                    if (!studentId.equals(rating.getStudentId())) {
                        throw new UnauthorizedAccessException("Bạn chỉ có thể xóa đánh giá của bạn.");
                    }

                    studentCourseRatingService.deleteRating(ratingId);

                    String ipAddress = httpRequest.getRemoteAddr();
                    String userAgent = httpRequest.getHeader("User-Agent");
                    userActivityLogService.trackUserActivityLog(
                            extractUserId(authorizationHeader),
                            ActivityType.DELETE,
                            ContentType.StudentCourseRating,
                            ratingId,
                            ipAddress,
                            userAgent
                    );

                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Xóa đánh giá thành công")
                            .build());
                } else if (roleId == 1) {
                    studentCourseRatingService.deleteRating(ratingId);
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Xóa đánh giá thành công")
                            .build());
                }
                else {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(ApiResponse.builder()
                                    .success(false)
                                    .message("Không có quyền")
                                    .build());
                }
            }
            else {
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
        } catch (UnauthorizedAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/average/{courseId}")
    public ResponseEntity<ApiResponse> getAverageRatingForCourse(@PathVariable Integer courseId) {
        try {
            BigDecimal averageRating = studentCourseRatingService.getAverageRatingForCourse(courseId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Tính đánh giá trung bình thành công")
                    .data(averageRating)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/course/ratings/{courseId}")
    public ResponseEntity<ApiResponse> getAllRatingsForCourse(
            @PathVariable Integer courseId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "rating") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            PageResponseDTO<StudentCourseRatingDTO> ratings = studentCourseRatingService.getAllRatingsForCourse(courseId, pageNo, pageSize, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(ratings.getContent().isEmpty() ? "Danh sách đánh giá trống" : "Truy xuất danh danh sách đánh giá thành công")
                    .data(ratings)
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
                            .message(e.getMessage())
                            .build());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/course/reviews/{courseId}")
    public ResponseEntity<ApiResponse> getAllReviewsForCourse(
            @PathVariable Integer courseId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "ratingId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try {
            PageResponseDTO<String> reviews = studentCourseRatingService.getAllReviewsForCourse(courseId, pageNo, pageSize, sortBy, sortDir);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(reviews.getContent().isEmpty() ? "No reviews found for this course" : "Reviews retrieved successfully")
                    .data(reviews)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/student-rating/course/{courseId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> getMyRatingForCourse(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer courseId) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            StudentCourseRatingDTO rating = studentCourseRatingService.getRatingByStudentAndCourse(studentId, courseId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Rating retrieved successfully")
                    .data(rating)
                    .build());
        } catch (IllegalStateException e) {
            return  ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message(e.getMessage())
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }
    private Integer extractStudentId(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            Integer userId = jwtUtil.extractUserId(token);
            return studentService.getStudentIdByUserId(userId);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid.");
    }

    private Integer extractUserId(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtUtil.extractUserId(token);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid.");
    }
}
