package com.example.HRmatch;

import lombok.Data;
import java.util.List;

@Data
public class CreateVacancyRequest {
    private String title;
    private String description;
    private Long createdById; // ID HR-менеджера
    private List<CompetencyLevelDto> requiredCompetencies; // Список требуемых навыков
}