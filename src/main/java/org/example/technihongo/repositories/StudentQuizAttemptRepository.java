package org.example.technihongo.repositories;

import org.example.technihongo.entities.StudentQuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentQuizAttemptRepository extends JpaRepository<StudentQuizAttempt, Integer> {
    List<StudentQuizAttempt> findByStudentStudentIdAndQuizQuizId(Integer studentId, Integer quizId);

    boolean existsByStudentStudentIdAndQuizQuizIdAndIsPassedAndIsCompleted
            (Integer studentId, Integer quizId, boolean isPassed, boolean isCompleted);

    Optional<StudentQuizAttempt> findByStudentStudentIdAndQuizQuizIdAndIsCompletedFalse(Integer studentId, Integer quizId);
    Optional<StudentQuizAttempt> findByAttemptId(Integer attemptId);

    boolean existsByQuiz_QuizId(Integer quizId);

    List<StudentQuizAttempt> findByStudentStudentIdAndQuizQuizIdOrderByDateTakenDesc(Integer studentId, Integer quizId);

    List<StudentQuizAttempt> findByStudentStudentIdAndQuizQuizIdAndIsCompletedTrueAndAttemptNumberGreaterThan(
            Integer studentId, Integer quizId, Integer attemptNumber);

    List<StudentQuizAttempt> findByStudentStudentIdAndQuizQuizIdAndAttemptNumberGreaterThan(Integer studentId, Integer quizId, int i);

    boolean existsByStudentStudentIdAndQuizQuizIdAndIsPassedTrueAndIsCompletedTrue(Integer studentId, Integer quizId);

    /**
     * Calculate the average quiz score grouped by day for a student.
     * Filters out the initial assessment attempt (attempt_number != 0) and
     * only includes attempts from the specified start date onwards.
     *
     * @return List of Objects where [0] = attemptDay (Date), [1] = avgScore (Double)
     */
    @Query(value = "SELECT CAST(q.date_taken AS DATE) AS attemptDay, " +
            "       AVG(q.score) AS avgScore " +
            "FROM StudentQuizAttempt q " +
            "WHERE q.student_id = :studentId " +
            "AND q.attempt_number != 0 " +
            "AND q.date_taken >= :startDate " +
            "GROUP BY CAST(q.date_taken AS DATE)", nativeQuery = true)
    List<Object[]> findAverageScoreByDay(
            @Param("studentId") Integer studentId,
            @Param("startDate") LocalDateTime startDate
    );
}
