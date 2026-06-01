package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.PageResponseDTO;
import org.example.technihongo.entities.LessonResource;
import org.example.technihongo.entities.StudentFavorite;

public interface StudentFavoriteService {
    StudentFavorite saveLearningResource(Integer studentId, Integer lessonResourceId);
    PageResponseDTO<LessonResource> getListFavoriteLearningResourcesByStudentId(
            Integer studentId, int pageNo, int pageSize, String sortBy, String sortDir);
    void removeFavoriteLearningResource(Integer studentId, Integer lessonResourceId);
    boolean checkLearningResourceFavorited(Integer studentId, Integer lessonResourceId);
}
