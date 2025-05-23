package org.example.technihongo.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter

public class UpdateSubscriptionDTO {
    private BigDecimal price;
    private String benefits;
    private Integer durationDays;
    private boolean isActive;
}
