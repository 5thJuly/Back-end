package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.LessonResource;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lesson-resource")
@Validated
public class LessonResourceController {
    @Autowired
    private LessonResourceService lessonResourceService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private UserActivityLogService userActivityLogService;
    @Autowired
    private StudentService studentService;
    @Autowired
    private StudentLessonProgressService studentLessonProgressService;
    @Autowired
    private LessonService lessonService;
    @Autowired
    private StudentCourseProgressService studentCourseProgressService;

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<ApiResponse> getLessonResourcesByLessonId(
            @PathVariable Integer lessonId,
            @RequestHeader("Authorization") String authorizationHeader) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 1 || roleId == 2) {
                    List<LessonResource> lessonResourceList = lessonResourceService.getLessonResourceListByLessonId(lessonId);
                    if (lessonResourceList.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("Danh sách LessonResources trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get All LessonResources By Lesson")
                                .data(lessonResourceList)
                                .build());
                    }
                } else {
                    Integer studentId = extractStudentId(authorizationHeader);
                    List<LessonResourceDTO> dto = lessonResourceService.getActiveLessonResourceListByLessonId(studentId, lessonId);
                    studentLessonProgressService.trackStudentLessonProgress(studentId, lessonId);
                    Integer courseId = lessonService.getCourseIdByLessonId(lessonId);
                    studentCourseProgressService.trackStudentCourseProgress(studentId, courseId, null);
                    if (dto.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("Danh sách Active LessonResources trống!!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get All Active LessonResources By Lesson")
                                .data(dto)
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
                            .message("Lấy danh sách LessonResources thất bại: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> viewLessonResource(@PathVariable Integer id,
                                                          @RequestHeader("Authorization") String authorizationHeader) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                LessonResource lessonResource;
                if (roleId == 1 || roleId == 2) {
                    lessonResource = lessonResourceService.getLessonResourceById(id);
                }
                else{
                    lessonResource = lessonResourceService.getActiveLessonResourceById(id);
                }
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get LessonResource")
                        .data(lessonResource)
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
                            .message("Get LessonResource failed: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> createLessonResource(
            @RequestBody CreateLessonResourceDTO createLessonResourceDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest){
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                LessonResource lessonResource = lessonResourceService.createLessonResource(createLessonResourceDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.CREATE,
                        ContentType.LessonResource,
                        lessonResource.getLessonResourceId(),
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Thêm LessonResource thành công!")
                        .data(lessonResource)
                        .build());
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
                            .message("Thêm LessonResource thất bại: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> updateLessonResource(
            @PathVariable Integer id,
            @RequestBody UpdateLessonResourceDTO updateLessonResourceDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                lessonResourceService.updateLessonResource(id, updateLessonResourceDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.UPDATE,
                        ContentType.LessonResource,
                        id,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("LessonResource updated successfully")
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
                            .message("Failed to update LessonResource: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update-order/{lessonId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updateLessonResourceOrder(@PathVariable Integer lessonId,
                                                         @RequestBody UpdateLessonResourceOrderDTO updateLessonResourceOrderDTO) {
        try{
            lessonResourceService.updateLessonResourceOrder(lessonId, updateLessonResourceOrderDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("LessonResources updated successfully")
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
                            .message("Failed to update LessonResources: " + e.getMessage())
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
    public ResponseEntity<ApiResponse> deleteLessonResource(
            @PathVariable Integer id,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                lessonResourceService.deleteLessonResource(id);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.DELETE,
                        ContentType.LessonResource,
                        id,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("LessonResource removed successfully!")
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
                            .message("Failed to delete LessonResource: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/study-plan")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> getLessonResourcesByDefaultStudyPlan(
            @RequestParam(defaultValue = "") Integer studyPlanId,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String type,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "lessonResourceId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        try{
            PageResponseDTO<LessonResource> lessonResourceList = lessonResourceService
                    .getLessonResourcesByDefaultStudyPlanPaginated(studyPlanId, keyword, type, pageNo, pageSize, sortBy, sortDir);
            if (lessonResourceList.getContent().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("List LessonResources is empty!")
                        .build());
            } else {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .data(lessonResourceList)
                        .message("LessonResources retrieve successfully!")
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
                            .message("Failed to get LessonResources: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/set-order/{lessonId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> setLessonResourceOrder(@PathVariable Integer lessonId,
                                                              @RequestParam Integer lessonResourceId,
                                                              @RequestParam Integer newOrder) {
        try{
            lessonResourceService.setLessonResourceOrder(lessonId, lessonResourceId, newOrder);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("LessonResources updated successfully")
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
                            .message("Failed to update LessonResources: " + e.getMessage())
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
}
