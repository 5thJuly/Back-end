package org.example.technihongo.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.technihongo.core.security.JwtUtil;
import org.example.technihongo.dto.*;
import org.example.technihongo.enums.ActivityType;
import org.example.technihongo.enums.ContentType;
import org.example.technihongo.exception.ResourceNotFoundException;
import org.example.technihongo.exception.UnauthorizedAccessException;
import org.example.technihongo.repositories.StudentFlashcardSetRepository;
import org.example.technihongo.repositories.UserRepository;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.StudentFlashcardSetProgressService;
import org.example.technihongo.services.interfaces.StudentFlashcardSetService;
import org.example.technihongo.services.interfaces.StudentService;
import org.example.technihongo.services.interfaces.UserActivityLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/student-flashcard-set")
public class StudentFlashcardSetController {
    @Autowired
    private StudentFlashcardSetService studentFlashcardSetService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private StudentService studentService;
    @Autowired
    private StudentFlashcardSetProgressService studentFlashcardSetProgressService;
    @Autowired
    private UserActivityLogService userActivityLogService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentFlashcardSetRepository studentFlashcardSetRepository;

    @PostMapping("/create")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> createFlashcardSet(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @Valid @RequestBody FlashcardSetRequestDTO request) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            FlashcardSetResponseDTO response = studentFlashcardSetService.createFlashcardSet(studentId, request);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.CREATE,
                    ContentType.StudentFlashcardSet,
                    response.getStudentSetId(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.builder()
                            .success(true)
                            .message("Flashcard set created successfully")
                            .data(response)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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

    @PatchMapping("/update/{setId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> updateFlashcardSet(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @PathVariable("setId") Integer flashcardSetId,
            @Valid @RequestBody FlashcardSetRequestDTO request) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            FlashcardSetResponseDTO response = studentFlashcardSetService.updateFlashcardSet(studentId, flashcardSetId, request);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.UPDATE,
                    ContentType.StudentFlashcardSet,
                    flashcardSetId,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard set updated successfully")
                    .data(response)
                    .build());
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

    @PatchMapping("/updateOrder/{flashcardSetId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> updateFlashcardOrder(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Integer flashcardSetId,
            @Valid @RequestBody UpdateFlashcardOrderDTO requestDTO,
            HttpServletRequest httpRequest) {
        try {
            Integer userId = extractUserId(authorizationHeader);
            Integer studentId = studentService.getStudentIdByUserId(userId);
            if (studentId == null) {
                throw new UnauthorizedAccessException("No student profile found for this user");
            }
            studentFlashcardSetService.updateFlashcardOrder(studentId, flashcardSetId, requestDTO);
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    studentId,
                    ActivityType.UPDATE,
                    ContentType.Flashcard,
                    flashcardSetId,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Student Flashcard Set order updated successfully")
                    .data(null)
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
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Invalid request: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @DeleteMapping("/delete/{setId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> deleteFlashcardSet(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @PathVariable("setId") Integer flashcardSetId) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            studentFlashcardSetService.deleteFlashcardSet(studentId, flashcardSetId);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.DELETE,
                    ContentType.StudentFlashcardSet,
                    flashcardSetId,
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard set marked as deleted successfully")
                    .build());
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

    @GetMapping("/getUserFlashcard/{setId}/{studentId}")
    public ResponseEntity<ApiResponse> getFlashcardSet(
            @PathVariable("setId") Integer flashcardSetId,
            @PathVariable("studentId") Integer studentId) {
        try {
            FlashcardSetResponseDTO response = studentFlashcardSetService.getAllFlashcardsInSet(studentId, flashcardSetId);

            if(studentId != null){
                studentFlashcardSetProgressService.trackFlashcardSetProgress(studentId, flashcardSetId, false, null);
            }

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard set retrieved successfully")
                    .data(response)
                    .build());
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

    @PatchMapping("/updateVisibility/{setId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> updateFlashcardSetVisibility(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("setId") Integer flashcardSetId,
            @RequestParam(required = true) Boolean isPublic) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            FlashcardSetResponseDTO response = studentFlashcardSetService.updateFlashcardSetVisibility(studentId, flashcardSetId, isPublic);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard set visibility updated successfully")
                    .data(response)
                    .build());
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

    @GetMapping("/getAllFlashcardOfSet/{setId}")

    public ResponseEntity<ApiResponse> getAllFlashcardsInSet(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("setId") Integer flashcardSetId) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            FlashcardSetResponseDTO response = studentFlashcardSetService.getAllFlashcardsInSet(studentId, flashcardSetId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcards retrieved successfully")
                    .data(response)
                    .build());
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

    @GetMapping("/getStudentFlashcardSet/{studentId}")
    public ResponseEntity<ApiResponse> getPublicFlashcardSetsByStudentId(
            @PathVariable Integer studentId)
    {
        try {

            List<FlashcardSetResponseDTO> response = studentFlashcardSetService.getFlashcardSetsByStudentId(studentId);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Public flashcard sets retrieved successfully")
                    .data(response)
                    .build());
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

    @GetMapping("/publicFlashcardSet")
    public ResponseEntity<ApiResponse> getAllPublicFlashcardSets() {
        try {
            List<FlashcardSetResponseDTO> flashcardSets = studentFlashcardSetService.getFlashcardSetsByPublicStatus();

            if (flashcardSets.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("No public flashcard sets available")
                        .data(Collections.emptyList())
                        .build());
            }

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Public flashcard sets retrieved successfully")
                    .data(flashcardSets)
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
                            .message("Failed to retrieve public flashcard sets: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> getAllStudentFlashcardSet(
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            List<FlashcardSetResponseDTO> studentFlashcardList = studentFlashcardSetService.studentFlashcardList(studentId);
            if (studentFlashcardList.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Student has no active flashcard sets")
                        .data(Collections.emptyList())
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Retrieved all flashcard sets successfully")
                    .data(studentFlashcardList)
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

    @GetMapping("/searchTitle")
    public ResponseEntity<ApiResponse> searchTitle(@RequestParam String keyword) {
        try {
            List<FlashcardSetResponseDTO> responseDTO = studentFlashcardSetService.searchTitle(keyword);
            if (responseDTO.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("No active flashcard sets found for keyword: " + keyword)
                        .data(Collections.emptyList())
                        .build());
            }
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Found flashcard sets for keyword: " + keyword)
                    .data(responseDTO)
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
                            .message("Failed to search flashcard sets: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/from-resource")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> createFlashcardSetFromResource(
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest,
            @Valid @RequestBody CreateFlashcardSetFromResourceDTO request) {
        try {
            Integer studentId = extractStudentId(authorizationHeader);
            FlashcardSetResponseDTO responseDTO = studentFlashcardSetService.createFlashcardSetFromResource(studentId, request);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    extractUserId(authorizationHeader),
                    ActivityType.CREATE,
                    ContentType.StudentFlashcardSet,
                    responseDTO.getStudentSetId(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.builder()
                            .success(true)
                            .message("Flashcard set created successfully from learning resource")
                            .data(responseDTO)
                            .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
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

    @PostMapping("/clone/{studentSetId}")
    @PreAuthorize("hasRole('ROLE_Student')")
    public ResponseEntity<ApiResponse> cloneFlashcardSet(
            @PathVariable Integer studentSetId,
            @RequestHeader("Authorization") String authorizationHeader,
            HttpServletRequest httpRequest) {
        try {
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Missing or invalid Authorization header")
                                .build());
            }

            String token = authorizationHeader.substring(7);
            Integer userId = jwtUtil.extractUserId(token);
            Integer studentId = studentService.getStudentIdByUserId(userId);
            if (studentId == null) {
                throw new UnauthorizedAccessException("No student profile found for this user");
            }

            FlashcardSetResponseDTO clonedSet = studentFlashcardSetService.cloneStudentFlashcardSet(studentId, studentSetId);

            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            userActivityLogService.trackUserActivityLog(
                    studentId,
                    ActivityType.CREATE,
                    ContentType.StudentFlashcardSet,
                    clonedSet.getStudentSetId(),
                    ipAddress,
                    userAgent
            );

            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Flashcard set cloned successfully")
                    .data(clonedSet)
                    .build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
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
        } catch (IllegalArgumentException e) {
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

    @PatchMapping("/set-violated/{setId}")
    @PreAuthorize("hasRole('ROLE_Administrator')")
    public ResponseEntity<ApiResponse> setViolatedFlashcardSet(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable("setId") Integer flashcardSetId) {
        try {
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring(7);
                Integer userId = jwtUtil.extractUserId(token);

                FlashcardSetViolationResponseDTO response = studentFlashcardSetService.setViolatedFlashcardSet(flashcardSetId, userId);

                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message(response.getMessage())
                        .data(response)
                        .build());
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.builder()
                                .success(false)
                                .message("Unauthorized")
                                .build());
            }
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

    private Integer extractStudentId(String authorizationHeader) throws Exception {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            Integer userId = jwtUtil.extractUserId(token);
            return studentService.getStudentIdByUserId(userId);
        }
        throw new Exception("Authorization failed!");
    }

    private Integer extractUserId(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            String token = authorizationHeader.substring(7);
            return jwtUtil.extractUserId(token);
        }
        throw new IllegalArgumentException("Authorization header is missing or invalid.");
    }
}