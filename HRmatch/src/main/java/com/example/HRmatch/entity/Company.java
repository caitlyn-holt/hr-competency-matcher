package com.example.HRmatch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "companies")
@Data @NoArgsConstructor @AllArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // Название компании
    private String industry;    // Отрасль (IT, Торговля, Производство...)
    private String location;    // Город/Регион
    private String description; // Кратко о компании
}