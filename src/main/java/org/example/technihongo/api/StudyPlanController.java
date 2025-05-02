package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.CreateStudyPlanDTO;
import org.example.technihongo.dto.StudyPlanStatusDTO;
import org.example.technihongo.dto.UpdateStudyPlanDTO;
import org.example.technihongo.entities.StudyPlan;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.StudyPlanService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/study-plan")
@RequiredArgsConstructor
public class StudyPlanController {
    @Autowired
    private StudyPlanService studyPlanService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse> getAllStudyPlansByCourseId(@RequestHeader("Authorization") String authorizationHeader,
                                                        @PathVariable Integer courseId) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 1 || roleId == 2) {
                    List<StudyPlan> studyPlanList = studyPlanService.getStudyPlanListByCourseId(courseId);
                    if (studyPlanList.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách StudyPlans trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get StudyPlans")
                                .data(studyPlanList)
                                .build());
                    }
                } else {
                    List<StudyPlan> studyPlanList = studyPlanService.getActiveStudyPlanListByCourseId(courseId);
                    if (studyPlanList.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách StudyPlans trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get Active StudyPlans")
                                .data(studyPlanList)
                                .build());
                    }
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
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Lấy danh sách StudyPlan thất bại: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> viewStudyPlan(@PathVariable Integer id,
                                                  @RequestHeader("Authorization") String authorizationHeader) throws Exception {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 1 || roleId == 2) {
                    StudyPlan StudyPlan = studyPlanService.getStudyPlanById(id);
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get StudyPlan")
                            .data(StudyPlan)
                            .build());
                    }
                else{
                    StudyPlan StudyPlan = studyPlanService.getActiveStudyPlanById(id);
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get StudyPlan")
                            .data(StudyPlan)
                            .build());
                }
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
                        .message("Failed to get StudyPlan: " + e.getMessage())
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
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> createStudyPlan(
            @RequestBody CreateStudyPlanDTO createStudyPlanDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try {
                StudyPlan studyPlan = studyPlanService.createStudyPlan(createStudyPlanDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        extractUserId(authorizationHeader),
                        ActivityType.CREATE,
                        ContentType.StudyPlan,
                        studyPlan.getStudyPlanId(),
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("StudyPlan created successfully!")
                        .data(studyPlan)
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
                            .message("Failed to create StudyPlan: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update/{id}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updateStudyPlan(
            @PathVariable Integer id,
            @RequestBody UpdateStudyPlanDTO updateStudyPlanDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            studyPlanService.updateStudyPlan(id, updateStudyPlanDTO);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.UPDATE,
                    ContentType.StudyPlan,
                    id,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("StudyPlan updated successfully!")
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
                            .message("Failed to update StudyPlan: " + e.getMessage())
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
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> deleteStudyPlan(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            studyPlanService.deleteStudyPlan(id);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.DELETE,
                    ContentType.StudyPlan,
                    id,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("StudyPlan deleted successfully!")
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
                            .message("Failed to delete StudyPlan: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update-status/{id}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updateStudyPlanStatus(
            @PathVariable Integer id,
            @RequestBody StudyPlanStatusDTO studyPlanStatusDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            studyPlanService.updateStudyPlanStatus(id, studyPlanStatusDTO);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.UPDATE,
                    ContentType.StudyPlan,
                    id,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("StudyPlan updated successfully!")
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
                            .message("Failed to update StudyPlan: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    private Integer extractUserId(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtUtil.extractUserId(token);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid.");
    }
}
