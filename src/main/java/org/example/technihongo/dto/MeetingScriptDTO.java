package org.example.technihongo.dto;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class MeetingScriptDTO {
    private Integer meetingId;
    private String question;
    private String questionExplain;
    private String answer;
    private String answerExplain;
}