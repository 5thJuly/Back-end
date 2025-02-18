package org.example.technihongo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FlashcardSetRequestDTO {
    private String title;
    private String description;
    private boolean isPublic;
}
