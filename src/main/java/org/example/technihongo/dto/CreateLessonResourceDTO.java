package org.example.technihongo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateLessonResourceDTO {
    private Integer lessonId;
    private Integer resourceId;
    private Integer systemSetId;
    private Integer quizId;
    private Boolean isActive;
}
