package org.example.technihongo.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "StudentLearningStatistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentLearningStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_stat_id")
    private Integer learningStatId;

    @OneToOne
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "student_id", unique = true)
    private Student student;

    @Column(name = "total_study_time")
    @Builder.Default
    private Integer totalStudyTime = 0;

    @Column(name = "total_completed_courses")
    @Builder.Default
    private Integer totalCompletedCourses = 0;

    @Column(name = "total_completed_lessons")
    @Builder.Default
    private Integer totalCompletedLessons = 0;

    @Column(name = "total_completed_quizzes")
    @Builder.Default
    private Integer totalCompletedQuizzes = 0;

    @Column(name = "active_days_count")
    @Builder.Default
    private Integer activeDaysCount = 0;

    @Column(name = "max_days_streak")
    @Builder.Default
    private Integer maxDaysStreak = 0;

    @Column(name = "total_achievements_unlocked")
    @Builder.Default
    private Integer totalAchievementsUnlocked = 0;

    @Column(name = "last_study_date")
    private LocalDateTime lastStudyDate;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
