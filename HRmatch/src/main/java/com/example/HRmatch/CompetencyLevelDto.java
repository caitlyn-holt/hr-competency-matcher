package com.example.HRmatch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyLevelDto {
    private Long id;
    private Integer level;

    private Double weight;
    private Boolean isCritical;
    private Integer minRequiredLevel;

    public CompetencyLevelDto(Long id, Integer level) {
        this.id = id;
        this.level = level;
    }
}