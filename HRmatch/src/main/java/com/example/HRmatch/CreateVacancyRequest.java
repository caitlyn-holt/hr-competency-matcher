package com.example.HRmatch;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateVacancyRequest {
    private String title;
    private String description;
    private Long createdById;
    private List<CompetencyLevelDto> requiredCompetencies;

    private Integer salaryFrom;
    private Integer salaryTo;
    private String experienceLevel;
}