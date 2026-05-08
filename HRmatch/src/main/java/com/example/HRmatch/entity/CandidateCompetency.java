package com.example.HRmatch.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "candidate_competencies")
@Data @NoArgsConstructor @AllArgsConstructor
public class CandidateCompetency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User candidate;

    @ManyToOne
    private Competency competency;

    @Min(1) @Max(5)
    private Integer level; // уровень кандидата: 1-5
}