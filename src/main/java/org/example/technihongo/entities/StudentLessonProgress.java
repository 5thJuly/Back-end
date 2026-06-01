package org.example.technihongo.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.technihongo.enums.CompletionStatus;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "[StudentLessonProgress]")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "lesson_id", nullable = false, referencedColumnName = "lesson_id")
    private Lesson lesson;

    @Column(name = "completion_percentage", precision = 5, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal completionPercentage = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", length = 30, nullable = false)
    @Builder.Default
    private CompletionStatus completionStatus = CompletionStatus.NOT_STARTED;

    @Column(name = "completed_items", nullable = false)
    @Builder.Default
    private Integer completedItems = 0;

    @Column(name = "last_studied")
    private LocalDateTime lastStudied;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
