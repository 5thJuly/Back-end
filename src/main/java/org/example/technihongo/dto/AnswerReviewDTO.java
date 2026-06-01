package org.example.technihongo.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnswerReviewDTO {
    private Integer questionId;
    private String questionType;
    private String questionText;
    private List<QuestionAnswerOptionDTO2> selectedOptions;
    private Boolean isCorrect;
    private String explanation;
}
