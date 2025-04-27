package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.CreatePathCourseDTO;
import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.UpdatePathCourseOrderDTO;
import org.example.technihongo.entities.PathCourse;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.LearningPathService;
import org.example.technihongo.services.interfaces.PathCourseService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/path-course")
@RequiredArgsConstructor
public class PathCourseController {
    @Autowired
    private PathCourseService pathCourseService;
    @Autowired
    private LearningPathService learningPathService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @GetMapping("/learning-path/{pathId}")
    public ResponseEntity<ApiResponse> getPathCourseListByLearningPathId(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @PathVariable Integer pathId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 1 || roleId == 2) {
                    PageResponseDTO<PathCourse> pathCourses = pathCourseService.getPathCoursesByLearningPathId(pathId, pageNo, pageSize, sortBy, sortDir);
                    if (pathCourses.getContent().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách PathCourse trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get PathCourse List")
                                .data(pathCourses)
                                .build());
                    }
                }
                else{
                    PageResponseDTO<PathCourse> pathCourses = pathCourseService.getPublicPathCourseListByLearningPathId(pathId, pageNo, pageSize, sortBy, sortDir);
                    if (pathCourses.getContent().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách PathCourse trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get Public PathCourse List")
                                .data(pathCourses)
                                .build());
                    }
                }
            }  else {
                PageResponseDTO<PathCourse> pathCourses = pathCourseService.getPublicPathCourseListByLearningPathId(pathId, pageNo, pageSize, sortBy, sortDir);
                if (pathCourses.getContent().isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Danh sách PathCourse trống!")
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get Public PathCourse List")
                            .data(pathCourses)
                            .build());
                }
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
                            .message("Lấy danh sách PathCourse thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getPathCourseById(@PathVariable Integer id) throws Exception {
        try{
            PathCourse pathCourse = pathCourseService.getPathCourseById(id);
            if(pathCourse == null){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("PathCourse not found!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get PathCourse")
                        .data(pathCourse)
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
                            .message("Failed to get PathCourse: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createPathCourse(
            @RequestBody CreatePathCourseDTO createPathCourseDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest){
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                PathCourse pathCourse = pathCourseService.createPathCourse(createPathCourseDTO);
                learningPathService.updateTotalCourses(createPathCourseDTO.getPathId());

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.CREATE,
                        ContentType.PathCourse,
                        pathCourse.getPathCourseId(),
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("PathCourse created successfully!")
                        .data(pathCourse)
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
                            .message("Failed to create PathCourse: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update-order/{pathId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updatePathCourseOrder(@PathVariable Integer pathId,
                                                         @RequestBody UpdatePathCourseOrderDTO updatePathCourseOrderDTO) {
        try{
            pathCourseService.updatePathCourseOrder(pathId, updatePathCourseOrderDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Cập nhật PathCourse thành công")
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
                            .message("Cập nhật PathCourse thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse> deletePathCourse(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                Integer pathId = pathCourseService.getPathCourseById(id).getLearningPath().getPathId();
                pathCourseService.deletePathCourse(id);
                learningPathService.updateTotalCourses(pathId);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.REMOVE,
                        ContentType.PathCourse,
                        id,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("PathCourse removed successfully!")
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
                            .message("Failed to delete PathCourse: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/set-order/{pathId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> setPathCourseOrder(@PathVariable Integer pathId,
                                                          @RequestParam Integer pathCourseId,
                                                          @RequestParam Integer newOrder) {
        try{
            pathCourseService.setPathCourseOrder(pathId, pathCourseId, newOrder);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("PathCourse updated successfully")
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
                            .message("Failed to update PathCourse: " + e.getMessage())
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
