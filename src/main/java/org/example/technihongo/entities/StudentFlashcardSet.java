package org.example.technihongo.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "StudentFlashcardSet")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentFlashcardSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_set_id")
    private Integer studentSetId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "creator_id", referencedColumnName = "student_id", nullable = false)
    private Student creator;

    @ManyToOne
    @JoinColumn(name = "resource_id", referencedColumnName = "resource_id")
    private LearningResource learningResource;

    @Column(name = "total_cards")
    private Integer totalCards;

    @Column(name = "total_views")
    private Integer totalViews;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "is_violated")
    @Builder.Default
    private boolean isViolated = false;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "studentFlashCardSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Flashcard> flashcards = new HashSet<>();

    @JsonIgnore
    @Builder.Default
    @OneToMany(mappedBy = "studentFlashcardSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudentFlashcardSetProgress> progresses = new HashSet<>();
}