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
@Table(name = "SystemFlashcardSet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SystemFlashcardSet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "system_set_id")
    private Integer systemSetId;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "creator_id", referencedColumnName = "user_id", nullable = false)
    private User creator;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "difficulty_level_id", referencedColumnName = "level_id", nullable = false)
    private DifficultyLevel difficultyLevel;

    @Column(name = "total_cards")
    private Integer totalCards;

    @Column(name = "is_public")
    private boolean isPublic;

    @Column(name = "is_premium")
    private boolean isPremium;

    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "systemFlashCardSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Flashcard> flashcards = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "systemFlashcardSet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<StudentFlashcardSetProgress> progresses = new HashSet<>();
}
