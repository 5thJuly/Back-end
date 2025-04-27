package org.example.technihongo.dto;

import lombok.Data;
import org.example.technihongo.entities.Meeting;

import java.time.LocalDateTime;

@Data
public class MeetingScriptDTO {
    private Integer meetingId;
    private String question;
    private String answer;
}