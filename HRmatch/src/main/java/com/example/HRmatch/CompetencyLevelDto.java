package com.example.HRmatch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetencyLevelDto {
    private Long id;              // ID компетенции
    private Integer level;        // Требуемый уровень (1-5)

    // Весовой коэффициент значимости (0.0 - 1.0), сумма всех = 1.0
    private Double weight;

    // Является ли компетенция критической (п. 4.5)
    private Boolean isCritical;

    // Минимальный порог для критической компетенции
    private Integer minRequiredLevel;
}