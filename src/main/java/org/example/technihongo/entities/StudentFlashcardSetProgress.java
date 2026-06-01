package org.example.technihongo.entities;

import jakarta.persistence.*;
import lombok.*;
import org.example.technihongo.enums.CompletionStatus;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "StudentFlashcardSetProgress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFlashcardSetProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "student_set_id")
    private StudentFlashcardSet studentFlashcardSet;

    @ManyToOne
    @JoinColumn(name = "system_set_id")
    private SystemFlashcardSet systemFlashcardSet;

    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", length = 20)
    private CompletionStatus completionStatus;

    @Column(name = "card_studied")
    private Integer cardStudied;

    @Column(name = "last_studied")
    private LocalDateTime lastStudied;

    @Column(name = "study_count")
    private Integer studyCount;

    @ManyToOne
    @JoinColumn(name = "current_flashcard_id")
    private Flashcard currentFlashCardId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
