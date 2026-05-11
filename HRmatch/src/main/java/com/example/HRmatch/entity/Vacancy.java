package com.example.HRmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Table(name = "vacancies")
@Data @NoArgsConstructor @AllArgsConstructor
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    private Integer salaryFrom;   // Зарплата от
    private Integer salaryTo;     // Зарплата до

    private String experienceLevel;

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    @JsonIgnore
    private User createdBy;

    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VacancyCompetency> requiredCompetencies;
}