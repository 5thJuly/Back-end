package org.example.technihongo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "StudentFlashcardProgress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFlashcardProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "progress_id")
    private Integer progressId;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false, referencedColumnName = "student_id")
    private Student student;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "flashcard_id", nullable = false, referencedColumnName = "flashcard_id")
    private Flashcard flashcard;

    @Column(name = "is_learned")
    private boolean isLearned;

    @Column(name = "last_studied")
    private LocalDateTime lastStudied;

    @Column(name = "starred")
    private Boolean starred = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
