package com.example.HRmatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Entity
@Table(name = "vacancies")
@Data // Lombok: автоматически создаёт геттеры/сеттеры
@NoArgsConstructor
@AllArgsConstructor
public class Vacancy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;

    // Поля зарплаты и опыта
    private Integer salaryFrom;
    private Integer salaryTo;
    private String experienceLevel;

    // Вакансия привязана к Компании
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id") // Колонка в таблице БД
    private Company company;

    // Вакансия создана Пользователем (HR)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    @JsonIgnore // Игнорируем при выводе JSON, чтобы не было зацикливания
    private User createdBy;

    // Вакансия имеет список требований (Компетенций)
    @OneToMany(mappedBy = "vacancy", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VacancyCompetency> requiredCompetencies;
}