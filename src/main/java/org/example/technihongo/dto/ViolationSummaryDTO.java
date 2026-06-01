package org.example.technihongo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ViolationSummaryDTO {
    private Integer studentSetId;
    private Integer ratingId;
    private long totalViolations;
    private PageResponseDTO<ViolationDescriptionDTO> descriptions;

    @Data
    public static class ViolationDescriptionDTO {
        private Integer violationId;
        private String description;
        private String status;
        private LocalDateTime createdAt;
    }
}
