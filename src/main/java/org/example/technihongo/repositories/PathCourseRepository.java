package org.example.technihongo.repositories;

import org.example.technihongo.entities.PathCourse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PathCourseRepository extends JpaRepository<PathCourse, Integer> {
    List<PathCourse> findByLearningPath_PathId(Integer pathId);
    List<PathCourse> findByLearningPath_PathIdOrderByCourseOrderAsc(Integer pathId);
    int countByLearningPath_PathId(Integer pathId);
    Page<PathCourse> findByLearningPath_PathId(Integer pathId, Pageable pageable);
    PathCourse findByLearningPath_PathIdAndCourse_CourseId(Integer pathId, Integer courseId);
    Page<PathCourse> findByLearningPath_PathIdAndCourse_PublicStatus(Integer pathId, boolean isPublic, Pageable pageable);
    Boolean existsByCourse_CourseId(Integer courseId);
}
