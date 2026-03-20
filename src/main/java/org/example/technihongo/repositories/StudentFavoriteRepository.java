package org.example.technihongo.repositories;

import org.example.technihongo.entities.StudentFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentFavoriteRepository extends JpaRepository<StudentFavorite, Integer>{
    boolean existsByStudent_StudentIdAndLessonResource_LessonResourceId(Integer studentId, Integer lessonResourceId);
    @Query("SELECT sf FROM StudentFavorite sf WHERE sf.student.studentId = :studentId " +
            "AND sf.lessonResource.learningResource.isPublic = :isPublic")
    Page<StudentFavorite> findByStudent_StudentIdAndLearningResource_IsPublic(
            @Param("studentId") Integer studentId,
            @Param("isPublic") boolean isPublic,
            Pageable pageable);

    @Query("SELECT sf FROM StudentFavorite sf WHERE sf.student.studentId = :studentId " +
            "AND sf.lessonResource.learningResource.isPublic = :isPublic AND sf.lessonResource.learningResource.isPremium = :isPremium")
    Page<StudentFavorite> findByStudent_StudentIdAndLearningResource_IsPublicAndLearningResource_IsPremium(
            @Param("studentId") Integer studentId,
            @Param("isPublic") boolean isPublic,
            @Param("isPremium") boolean isPremium,
            Pageable pageable);

    Optional<StudentFavorite> findByStudent_StudentIdAndLessonResource_LessonResourceId(Integer studentId, Integer lessonResourceId);

    long countByStudent_StudentId(Integer studentId);
}
