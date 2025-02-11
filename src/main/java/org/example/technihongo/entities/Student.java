package org.example.technihongo.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;


import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "Student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Integer studentId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    @Column(name = "bio")
    private String bio;

    @Column(name = "daily_goal")
    private Integer dailyGoal = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "occupation_status")
    private OccupationStatus occupation;

    @Column(name = "reminder_enabled")
    @Builder.Default
    private boolean reminderEnabled = true;

    @Column(name = "reminder_time")
    private LocalTime reminderTime;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "level_id", nullable = false, referencedColumnName = "level_id")
    private DifficultyLevel difficultyLevel;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OccupationStatus{
        STUDENT,EMPLOYED, UNEMPLOYED, FREELANCER, OTHER
    }


}