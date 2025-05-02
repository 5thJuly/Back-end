package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.Course;
import org.example.technihongo.entities.StudentCourseProgress;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {
    @Autowired
    private CourseService courseService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StudentService studentService;
    @Autowired
    private StudentCourseProgressService studentCourseProgressService;
    @Autowired
    private UserActivityLogService userActivityLogService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponse> getAllCourses(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);
                Integer loginUserId = jwtUtil.extractUserId(token);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.VIEW,
                        ContentType.Course,
                        null,
                        ipAddress,
                        userAgent
                );

                if (roleId == 1 || roleId == 2) {
                    List<Course> courseList = courseService.courseList();
                    if (courseList.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("List courses is empty!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get All Courses")
                                .data(courseList)
                                .build());
                    }
                } else {
                    List<CoursePublicDTO> courseList = courseService.getPublicCourses();
                    if (courseList.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("List courses is empty!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get All Public Courses")
                                .data(courseList)
                                .build());
                    }
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
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> viewCourse(@PathVariable Integer id,
                                                  @RequestHeader(value = "Authorization") String authorizationHeader,
                                                  HttpServletRequest httpRequest){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);
                Integer loginUserId = jwtUtil.extractUserId(token);

                if (roleId == 1 || roleId == 2) {
                    Optional<Course> course = courseService.getCourseById(id);

                    String ipAddress = httpRequest.getRemoteAddr();
                    String userAgent = httpRequest.getHeader("User-Agent");
                    userActivityLogService.trackUserActivityLog(
                            loginUserId,
                            ActivityType.VIEW,
                            ContentType.Course,
                            course.map(Course::getCourseId).orElse(null),
                            ipAddress,
                            userAgent
                    );

                    if (course.isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("Course not found!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get Course")
                                .data(course)
                                .build());
                    }
                }
                else{
                    Integer studentId = studentService.getStudentIdByUserId(loginUserId);
                    studentCourseProgressService.trackStudentCourseProgress(studentId, id, null);
                    Optional<CoursePublicDTO> course = courseService.getPublicCourseById(id);

                    String ipAddress = httpRequest.getRemoteAddr();
                    String userAgent = httpRequest.getHeader("User-Agent");
                    userActivityLogService.trackUserActivityLog(
                            loginUserId,
                            ActivityType.VIEW,
                            ContentType.Course,
                            course.map(CoursePublicDTO::getCourseId).orElse(null),
                            ipAddress,
                            userAgent
                    );

                    if(course.isEmpty()){
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(false)
                                .message("Course not found!")
                                .build());
                    }else{
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get Course")
                                .data(course)
                                .build());
                    }
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

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> createCourse(
            @RequestBody CreateCourseDTO createCourseDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);

                Course course = courseService.createCourse(userId, createCourseDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        userId,
                        ActivityType.CREATE,
                        ContentType.Course,
                        course.getCourseId(),
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Tạo khóa học thành công!")
                        .data(course)
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
                            .message("Tạo khóa học thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update/{courseId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updateCourse(
            @PathVariable Integer courseId,
            @RequestBody UpdateCourseDTO updateCourseDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                courseService.updateCourse(courseId, updateCourseDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.UPDATE,
                        ContentType.Course,
                        courseId,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Cập nhật khóa học thành công")
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
                            .message("Cập nhật khóa học thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/search/{keyword}")
    public ResponseEntity<ApiResponse> searchCourseByTitle(@PathVariable String keyword) throws Exception {
        try{
            List<Course> course = courseService.searchCourseByTitle(keyword);
            if(course.isEmpty()){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("List courses is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Courses List By Keyword")
                        .data(course)
                        .build());
            }
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

    @GetMapping("/creator")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> getCourseListByCreator(@RequestHeader("Authorization") String authorizationHeader) throws Exception {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);

                List<Course> course = courseService.getListCoursesByCreatorId(userId);
                if (course.isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(false)
                            .message("List courses is empty!")
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get Courses List By Creator")
                            .data(course)
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
                            .message("Failed to get courses: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/all/paginated")
    public ResponseEntity<ApiResponse> getAllCoursesPaginated(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "courseId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") Integer domainId,
            @RequestParam(defaultValue = "") Integer difficultyLevelId){
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                int roleId = jwtUtil.extractUserRoleId(token);

                if (roleId == 1 || roleId == 2) {
                    PageResponseDTO<Course> courseList = courseService.courseListPaginated(keyword, domainId, difficultyLevelId, pageNo, pageSize, sortBy, sortDir);
                    if (courseList.getContent().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách khóa học trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Lấy danh sách khóa học")
                                .data(courseList)
                                .build());
                    }
                } else {
                    PageResponseDTO<Course> courseList = courseService.getPublicCoursesPaginated(keyword, domainId, difficultyLevelId, pageNo, pageSize, sortBy, sortDir);
                    if (courseList.getContent().isEmpty()) {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Danh sách khóa học trống!")
                                .build());
                    } else {
                        return ResponseEntity.ok(ApiResponse.builder()
                                .success(true)
                                .message("Get All Public Courses")
                                .data(courseList)
                                .build());
                    }
                }
            }
            else {
                PageResponseDTO<Course> courseList = courseService.getPublicCoursesPaginated(keyword, domainId, difficultyLevelId, pageNo, pageSize, sortBy, sortDir);
                if (courseList.getContent().isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Danh sách khóa học trống!")
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get All Public Courses")
                            .data(courseList)
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
                            .message("Lấy danh sách khóa học thất bại: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/search/paginated/{keyword}")
    public ResponseEntity<ApiResponse> searchCourseByTitlePaginated(
            @PathVariable String keyword,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "courseId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) throws Exception {
        try{
            PageResponseDTO<Course> courseList = courseService.searchCourseByTitlePaginated(keyword, pageNo, pageSize, sortBy, sortDir);
            if(courseList.getContent().isEmpty()){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("List courses is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Courses List By Keyword")
                        .data(courseList)
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
                            .message("Failed to get courses: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/creator/paginated")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> getCourseListByCreatorPaginated(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "courseId") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") Integer domainId,
            @RequestParam(defaultValue = "") Integer difficultyLevelId) throws Exception {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);

                PageResponseDTO<Course> courseList = courseService.getListCoursesByCreatorIdPaginated(keyword, userId, domainId, difficultyLevelId, pageNo, pageSize, sortBy, sortDir);
                if (courseList.getContent().isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(false)
                            .message("List courses is empty!")
                            .build());
                } else {
                    return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Get Courses List By Creator")
                            .data(courseList)
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
                            .message("Failed to get courses: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/domain/{parentDomainId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> getCourseListByParentDomainId(
            @PathVariable Integer parentDomainId,
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "100") int pageSize,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        try {
            PageResponseDTO<Course> courses = courseService.getCourseListByParentDomainId(parentDomainId, pageNo, pageSize, sortBy, sortDir);

            if (courses.getContent().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("No courses found for this parent domain!")
                        .build());
            } else {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Course list retrieved successfully")
                        .data(courses)
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
                            .message("Failed to get course list: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update-status/{courseId}")
    @PreAuthorize("hasRole('ROLE_Content Manager')")
    public ResponseEntity<ApiResponse> updateCourseStatus(
            @PathVariable Integer courseId,
            @RequestBody CourseStatusDTO courseStatusDTO,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try{
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer loginUserId = jwtUtil.extractUserId(token);

                courseService.updateCourseStatus(courseId, courseStatusDTO);

                String ipAddress = httpRequest.getRemoteAddr();
                String userAgent = httpRequest.getHeader("User-Agent");
                userActivityLogService.trackUserActivityLog(
                        loginUserId,
                        ActivityType.UPDATE,
                        ContentType.Course,
                        courseId,
                        ipAddress,
                        userAgent
                );

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Cập nhật khóa học thành công")
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
                            .message("Cập nhật khóa học thất bại: " + e.getMessage())
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
