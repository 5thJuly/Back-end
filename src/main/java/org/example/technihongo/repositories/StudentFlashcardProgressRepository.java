package org.example.technihongo.repositories;

import org.example.technihongo.entities.StudentFlashcardProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentFlashcardProgressRepository extends JpaRepository<StudentFlashcardProgress, Integer> {
    @Query("SELECT p FROM StudentFlashcardProgress p " +
            "JOIN FETCH p.flashcard f " +
            "JOIN f.studentFlashCardSet s " +
            "WHERE p.student.studentId = :studentId " +
            "AND s.studentSetId = :setId " +
            "AND (:starred IS NULL OR p.starred = : starred)")
    List<StudentFlashcardProgress> findStarredByStudentAndStudentSet(
            @Param("studentId") Integer studentId,
            @Param("setId") Integer setId,
            @Param("starred") Boolean starred
    );
    @Query("SELECT p FROM StudentFlashcardProgress p " +
            "JOIN FETCH p.flashcard f " +
            "JOIN f.systemFlashCardSet s " +
            "WHERE p.student.studentId = :studentId " +
            "AND s.systemSetId = :setId " +
            "AND (:starred IS NULL OR p.starred = : starred)")
    List<StudentFlashcardProgress> findStarredByStudentAndSystemSet(
            @Param("studentId") Integer studentId,
            @Param("setId") Integer setId,
            @Param("starred") Boolean starred
    );    Optional<StudentFlashcardProgress> findByStudentStudentIdAndFlashcardFlashCardId(Integer studentId, Integer flashcardId);
    List<StudentFlashcardProgress> findByStudentStudentIdAndFlashcard_StudentFlashCardSet_StudentSetId(Integer studentId, Integer studentSetId);
    List<StudentFlashcardProgress> findByStudentStudentIdAndFlashcard_SystemFlashCardSet_SystemSetId(Integer studentId, Integer systemSetId);
}
