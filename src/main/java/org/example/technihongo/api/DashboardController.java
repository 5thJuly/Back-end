package org.example.technihongo.api;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.AdminOverviewDTO;
import org.example.technihongo.dto.LearningStatsDTO;
import org.example.technihongo.dto.QuizStatsDTO;
import org.example.technihongo.dto.StudentSpendingDTO;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.DashboardService;
import org.example.technihongo.services.interfaces.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StudentService studentService;

    @GetMapping("/student/spending")
    public ResponseEntity<ApiResponse> getStudentSpending(@RequestHeader("Authorization") String authorizationHeader) {
        try{
            Integer studentId = extractStudentId(authorizationHeader);
            StudentSpendingDTO dto = dashboardService.getStudentSpending(studentId);
            if(dto == null){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Spending is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Student Spending")
                        .data(dto)
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

    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public ResponseEntity<ApiResponse> getAdminOverview(@RequestHeader("Authorization") String authorizationHeader) {
        try{
            AdminOverviewDTO dto = dashboardService.getAdminOverview();
            if(dto == null){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Overview is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Admin Overview")
                        .data(dto)
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

    @GetMapping("/student/learning")
    public ResponseEntity<ApiResponse> getStudentLearningStats(@RequestHeader("Authorization") String authorizationHeader) {
        try{
            Integer studentId = extractStudentId(authorizationHeader);
            LearningStatsDTO dto = dashboardService.getLearningStats(studentId);
            if(dto == null){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Learning stats is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Student Learning Stats")
                        .data(dto)
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

    @GetMapping("/student/quiz")
    public ResponseEntity<ApiResponse> getStudentQuizStats(@RequestHeader("Authorization") String authorizationHeader) {
        try{
            Integer studentId = extractStudentId(authorizationHeader);
            List<QuizStatsDTO> dto = dashboardService.getWeeklyQuizStats(studentId);
            if(dto.isEmpty()){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Quiz stats is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Student Quiz Stats")
                        .data(dto)
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

    private Integer extractStudentId(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            Integer userId = jwtUtil.extractUserId(token);
            return studentService.getStudentIdByUserId(userId);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid.");
    }

}
