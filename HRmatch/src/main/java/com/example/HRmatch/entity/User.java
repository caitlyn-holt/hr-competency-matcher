package com.example.HRmatch.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    private String password; // Теперь тут будет хеш

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")

    private Company company;
    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private Role role;
}