package org.example.technihongo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "LessonResource")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_resource_id")
    private Integer lessonResourceId;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "lesson_id", referencedColumnName = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(name = "type")
    private String type;

    @Column(name = "type_order")
    private Integer typeOrder;

    @ManyToOne
    @JoinColumn(name = "resource_id", referencedColumnName = "resource_id", nullable = true)
    private LearningResource learningResource;

    @ManyToOne
    @JoinColumn(name = "system_set_id", referencedColumnName = "system_set_id", nullable = true)
    private SystemFlashcardSet systemFlashCardSet;

    @ManyToOne
    @JoinColumn(name = "quiz_id", referencedColumnName = "quiz_id", nullable = true)
    private Quiz quiz;

//    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
