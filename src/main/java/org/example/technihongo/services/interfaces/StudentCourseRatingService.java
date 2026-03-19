package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.dto.StudentCourseRatingDTO;
import org.example.technihongo.dto.StudentCourseRatingRequest;
import java.math.BigDecimal;
import java.util.List;

public interface StudentCourseRatingService {
    StudentCourseRatingDTO createRating(Integer studentId, StudentCourseRatingRequest request);
    StudentCourseRatingDTO getRatingById(Integer ratingId);
    List<StudentCourseRatingDTO> getAllRatings();
    StudentCourseRatingDTO updateRating(Integer ratingId, Integer studentId, StudentCourseRatingRequest request);
    void deleteRating(Integer ratingId);
    BigDecimal getAverageRatingForCourse(Integer courseId);
    PageResponseDTO<StudentCourseRatingDTO> getAllRatingsForCourse(Integer courseId, int pageNo, int pageSize, String sortBy, String sortDir);
    PageResponseDTO<String> getAllReviewsForCourse(Integer courseId, int pageNo, int pageSize, String sortBy, String sortDir);
    StudentCourseRatingDTO getRatingByStudentAndCourse(Integer studentId, Integer courseId);
}
