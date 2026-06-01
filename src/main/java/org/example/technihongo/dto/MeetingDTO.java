package org.example.technihongo.dto;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class MeetingDTO {
    private String title;
    private String description;
    private Boolean isActive;
    private String voiceName;
}
