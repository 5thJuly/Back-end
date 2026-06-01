package org.example.technihongo.api;


import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.FlashcardProgressDTO;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.FlashcardProgressService;
import org.example.technihongo.services.interfaces.StudentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/flashcard-progress")
@RequiredArgsConstructor
public class FlashcardProgressController {
    
    private final FlashcardProgressService flashcardProgressService;
    
    private final JwtUtil jwtUtil;
    
    private final StudentService studentService;

    @GetMapping("/starred")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> getStarredFlashcards(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Integer setId,
            @RequestParam(defaultValue = "false") boolean isSystemSet) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                List<FlashcardProgressDTO> progressList = flashcardProgressService.getStarredFlashcards(studentId, setId, isSystemSet);

                if(progressList.isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.builder()
                                    .success(true)
                                    .message("No starred flashcards found for this set")
                                    .build());
                }
                else return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Starred flashcards retrieved successfully")
                        .data(progressList)
                        .build());
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
                            .message("Failed to retrieve flashcard progress: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }


    // starred and is learned
    @PatchMapping("/update")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> updateFlashcardProgress(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam Integer flashcardId,
            @RequestParam(defaultValue = "false") boolean isStarred) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);
                Integer studentId = studentService.getStudentIdByUserId(userId);

                flashcardProgressService.updateFlashcardProgress(studentId, flashcardId, isStarred);

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Flashcard progress updated successfully")
                        .data(null)
                        .build());
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
                            .message("Failed to update flashcard progress: " + e.getMessage())
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
