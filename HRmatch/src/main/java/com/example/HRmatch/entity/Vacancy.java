package com.example.HRmatch.entity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity @Table(name = "vacancies")
@Data @NoArgsConstructor @AllArgsConstructor
public class Vacancy {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;

    @ManyToOne
    private User createdBy;

    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VacancyCompetency> requiredCompetencies;
}