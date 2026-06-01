package org.example.technihongo.dto;
import lombok.*;
import org.example.technihongo.entities.StudyPlan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonDTO {
    private Integer lessonId;
    private StudyPlan studyPlan;
    private String title;
    private Integer lessonOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal studentProgress;

    public LessonDTO(Integer lessonId, StudyPlan studyPlan, String title, Integer lessonOrder,
                     BigDecimal studentProgress, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.lessonId = lessonId;
        this.studyPlan = studyPlan;
        this.title = title;
        this.lessonOrder = lessonOrder;
        this.studentProgress = studentProgress != null ? studentProgress : BigDecimal.ZERO;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
