package org.example.technihongo.dto;

import lombok.Data;
import org.example.technihongo.entities.User;

import java.time.LocalDateTime;

@Data
public class MeetingDTO {
    private String title;
    private String description;
    private Boolean isActive;
    private String voiceName;
}
