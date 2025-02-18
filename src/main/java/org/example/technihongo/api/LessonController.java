package org.example.technihongo.api;

import lombok.RequiredArgsConstructor;
import org.example.technihongo.dto.*;
import org.example.technihongo.entities.Course;
import org.example.technihongo.entities.Lesson;
import org.example.technihongo.response.ApiResponse;
import org.example.technihongo.services.interfaces.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/lesson")
@RequiredArgsConstructor
public class LessonController {
    @Autowired
    private LessonService lessonService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getLessonById(@PathVariable Integer id) throws Exception {
        try{
            Optional<Lesson> lesson = lessonService.getLessonById(id);
            if(lesson.isEmpty()){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("Lesson not found!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Lesson")
                        .data(lesson)
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/csp/{id}")
    public ResponseEntity<ApiResponse> getLessonListByCourseStudyPlanId(@PathVariable Integer id) throws Exception {
        try{
            List<Lesson> lessonList = lessonService.getLessonListByCourseStudyPlanId(id);
            if(lessonList.isEmpty()){
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(false)
                        .message("List lessons is empty!")
                        .build());
            }else{
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Get Lesson List")
                        .data(lessonList)
                        .build());
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse> createLesson(@RequestBody CreateLessonDTO createLessonDTO){
        try {
                Lesson lesson = lessonService.createLesson(createLessonDTO);
                return ResponseEntity.ok(ApiResponse.builder()
                        .success(true)
                        .message("Lesson created successfully!")
                        .data(lesson)
                        .build());

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to create lesson: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update/{lessonId}")
    public ResponseEntity<ApiResponse> updateLesson(@PathVariable Integer lessonId,
                                                    @RequestBody UpdateLessonDTO updateLessonDTO) {
        try{
            lessonService.updateLesson(lessonId, updateLessonDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Lesson updated successfully")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to update lesson: " + e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Internal Server Error: " + e.getMessage())
                            .build());
        }
    }

    @PatchMapping("/update-order/{coursePlanId}")
    public ResponseEntity<ApiResponse> updateLessonOrder(@PathVariable Integer coursePlanId,
                                                    @RequestBody UpdateLessonOrderDTO updateLessonOrderDTO) {
        try{
            lessonService.updateLessonOrder(coursePlanId, updateLessonOrderDTO);
            return ResponseEntity.ok(ApiResponse.builder()
                    .success(true)
                    .message("Lesson updated successfully")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.builder()
                            .success(false)
                            .message("Failed to update lesson: " + e.getMessage())
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
