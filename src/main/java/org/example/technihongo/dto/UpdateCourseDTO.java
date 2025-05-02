package org.example.technihongo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCourseDTO {
    private String title;
    private String description;
    private Integer domainId;
    private Integer difficultyLevelId;
    private String attachmentUrl;
    private String thumbnailUrl;
    private String estimatedDuration;
//    private Boolean isPublic;
//    private Boolean isPremium;
}
