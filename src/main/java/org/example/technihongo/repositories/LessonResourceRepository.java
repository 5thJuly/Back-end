package org.example.technihongo.repositories;

import org.example.technihongo.entities.LessonResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonResourceRepository extends JpaRepository<LessonResource, Integer> {
    void deleteByLesson_LessonIdIn(List<Integer> lessonIds);
    Integer countByLesson_LessonId(Integer lessonId);
    LessonResource findByLessonResourceId(Integer lessonResourceId);
    List<LessonResource> findByLesson_LessonIdOrderByTypeOrderAsc(Integer lessonId);
    Boolean existsByLesson_LessonIdAndLearningResource_ResourceId(Integer lessonId, Integer resourceId);
    Boolean existsByLesson_LessonIdAndSystemFlashCardSet_SystemSetId(Integer lessonId, Integer setId);
    Boolean existsByLesson_LessonIdAndQuiz_QuizId(Integer lessonId, Integer quizId);
    Boolean existsByLearningResource_ResourceId(Integer resourceId);

    Page<LessonResource> findByLesson_StudyPlan_StudyPlanId(Integer studyPlanId, Pageable pageable);
    Page<LessonResource> findByLesson_StudyPlan_StudyPlanIdAndType(Integer studyPlanId, String type, Pageable pageable);
    Page<LessonResource> findByLesson_StudyPlan_StudyPlanIdAndLearningResource_TitleContainsIgnoreCase(Integer studyPlanId, String keyword, Pageable pageable);
    Page<LessonResource> findByLesson_StudyPlan_StudyPlanIdAndSystemFlashCardSet_TitleContainsIgnoreCase(Integer studyPlanId, String keyword, Pageable pageable);
    Page<LessonResource> findByLesson_StudyPlan_StudyPlanIdAndQuiz_TitleContainsIgnoreCase(Integer studyPlanId, String keyword, Pageable pageable);
    Page<LessonResource> findByLesson_StudyPlan_StudyPlanIdAndLearningResource_TitleContainsIgnoreCaseOrSystemFlashCardSet_TitleContainsIgnoreCaseOrQuiz_TitleContainsIgnoreCase(Integer studyPlanId, String keyword1, String keyword2, String keyword3, Pageable pageable);

    List<LessonResource> findByQuiz_QuizId(Integer quizId);
    List<LessonResource> findBySystemFlashCardSet_SystemSetId(Integer flashcardSetId);
    List<LessonResource> findByLearningResource_ResourceId(Integer learningResourceId);

    @Query("SELECT lr FROM LessonResource lr " +
            "WHERE lr.lesson.lessonId = :lessonId AND lr.isActive = :isActive " +
            "ORDER BY lr.typeOrder ASC")
    List<LessonResource> findByLesson_LessonIdAndActiveOrderByTypeOrderAsc(
            @Param("lessonId") Integer lessonId,
            @Param("isActive") boolean isActive);

    void deleteByLesson_LessonId(Integer lessonId);
}
