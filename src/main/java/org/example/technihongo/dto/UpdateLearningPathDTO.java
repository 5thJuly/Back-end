package org.example.technihongo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLearningPathDTO {
    private String title;
    private String description;
    private Integer domainId;
    private Boolean isPublic;
}
