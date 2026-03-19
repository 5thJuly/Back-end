package org.example.technihongo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReportViolationRequestDTO {
    private String classifyBy;
    private Integer contentId;
    private String description;
}
