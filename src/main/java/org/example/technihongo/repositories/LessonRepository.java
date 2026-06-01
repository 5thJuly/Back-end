package org.example.technihongo.repositories;

import jakarta.validation.constraints.NotNull;
import org.example.technihongo.entities.StudyPlan;
import org.example.technihongo.entities.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Integer> {
    Lesson findByLessonId(Integer lessonId);
    List<Lesson> findByStudyPlan_StudyPlanId(Integer studyPlanId);
    void deleteByStudyPlan_StudyPlanId(Integer studyPlanId);
    Integer countLessonByStudyPlan(@NotNull StudyPlan studyPlan);
    List<Lesson> findByStudyPlan_StudyPlanIdOrderByLessonOrderAsc(Integer studyPlanId);

    Page<Lesson> findByStudyPlan_StudyPlanId(Integer studyPlanId, Pageable pageable);
    Page<Lesson> findByStudyPlan_StudyPlanIdAndTitleContainingIgnoreCase(Integer studyPlanId, String keyword, Pageable pageable);
    Lesson findFirstByStudyPlanStudyPlanIdAndLessonOrderGreaterThanOrderByLessonOrderAsc(Integer studyPlanId, Integer lessonOrder);
}
