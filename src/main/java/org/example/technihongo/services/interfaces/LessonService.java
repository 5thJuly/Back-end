package org.example.technihongo.services.interfaces;

import org.example.technihongo.dto.*;
import org.example.technihongo.entities.Lesson;

import java.util.List;
import java.util.Optional;

public interface LessonService {
    Optional<Lesson> getLessonById(Integer lessonId);
    List<Lesson> getLessonListByStudyPlanId(Integer studyPlanId);
    Lesson createLesson(CreateLessonDTO createLessonDTO);
    void updateLesson(Integer lessonId, UpdateLessonDTO updateLessonDTO);
    void updateLessonOrder(Integer studyPlanId, UpdateLessonOrderDTO updateLessonOrderDTO);
    void deleteLesson(Integer lessonId);

    PageResponseDTO<Lesson> getLessonListByStudyPlanIdPaginated(Integer studyPlanId, int pageNo, int pageSize, String sortBy, String sortDir, String keyword);
    void checkLessonProgressPrerequisite(Integer studentId, Integer lessonId);
    Integer getCourseIdByLessonId(Integer lessonId);
    void setLessonOrder(Integer studyPlanId, Integer lessonId, Integer newOrder);
    PageResponseDTO<LessonDTO> getLessonListByStudyPlanIdWithProgress(Integer studyPlanId, Integer studentId,
                                                                      int pageNo, int pageSize, String sortBy,
                                                                      String sortDir, String keyword);
}
