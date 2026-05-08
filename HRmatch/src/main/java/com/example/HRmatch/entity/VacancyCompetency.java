package com.example.HRmatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "vacancy_competencies")
@Data @NoArgsConstructor @AllArgsConstructor
public class VacancyCompetency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Vacancy vacancy;

    @ManyToOne
    private Competency competency;

    @Min(1) @Max(5)
    private Integer requiredLevel; // требуемый уровень: 1-5
}