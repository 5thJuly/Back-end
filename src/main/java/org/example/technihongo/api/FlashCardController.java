package org.example.technihongo.api;

import org.example.technihongo.dto.FlashcardRequestDTO;
import org.example.technihongo.dto.FlashcardResponseDTO;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.FlashcardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/flashcard")
public class FlashCardController {

    @Autowired
    private FlashcardService flashcardService;

    @PostMapping("/{studentId}/{setId}/create")
    public ResponseEntity<ApiResponse> createFlashcard(
            @PathVariable Integer studentId,
            @PathVariable("setId") Integer flashcardSetId,
            @RequestBody FlashcardRequestDTO request) {
        try {
            FlashcardResponseDTO response = flashcardService.createFlashcard(studentId, flashcardSetId, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard created successfully")
                    .data(response)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to create flashcard: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }
    @PatchMapping("/{studentId}/{flashcardId}/update")
    public ResponseEntity<ApiResponse> updateFlashcard(
            @PathVariable Integer studentId,
            @PathVariable Integer flashcardId,
            @RequestBody FlashcardRequestDTO request) {
        try {
            FlashcardResponseDTO response = flashcardService.updateFlashcard(studentId, flashcardId, request);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard updated successfully")
                    .data(response)
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to update flashcard: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }
    @DeleteMapping("/delete/{studentId}/{flashcardId}")
    public ResponseEntity<ApiResponse> deleteFlashcard(
            @PathVariable Integer studentId,
            @PathVariable Integer flashcardId) {
        try {
            flashcardService.deleteFlashcard(studentId, flashcardId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard deleted successfully")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to delete flashcard: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }
    @GetMapping("/getFlashcard/{flashcardId}")
    public ResponseEntity<ApiResponse> getFlashcard(
            @PathVariable Integer flashcardId) {
        try {
            FlashcardResponseDTO responseDTO = flashcardService.getFlashcardById(flashcardId);
            return ResponseEntity.ok(ApiResponse.builder()
                            .success(true)
                            .message("Flashcard retrieved successfully")
                            .data(responseDTO)
                            .build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to retrieve flashcard: " + e.getMessage())
                            .build());
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }





}
