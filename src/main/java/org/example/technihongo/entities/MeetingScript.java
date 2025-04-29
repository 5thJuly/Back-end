package org.example.technihongo.entities;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "MeetingScript")
@Data
public class MeetingScript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "script_id")
    private Integer scriptId;

    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @Column(name = "question")
    private String question;

    @Column(name = "question_explain")
    private String questionExplain;

    @Column(name = "answer")
    private String answer;

    @Column(name = "answer_explain")
    private String answerExplain;

    @Column(name = "script_order")
    private Integer scriptOrder;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (scriptOrder < 1) {
            throw new IllegalArgumentException("Script order must be positive");
        }
    }
}
