package com.example.HRmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "vacancy_competencies")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VacancyCompetency {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Vacancy vacancy;

    @ManyToOne
    private Competency competency;

    private Integer requiredLevel;
}