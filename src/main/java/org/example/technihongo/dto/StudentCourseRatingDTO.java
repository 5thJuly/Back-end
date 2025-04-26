package org.example.technihongo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseRatingDTO {
    private Integer ratingId;
    private Integer studentId;
    private String userName;
    private String profileImg;
    private Integer courseId;
    private Integer rating;
    private String review;
    private LocalDateTime createdAt;
}
