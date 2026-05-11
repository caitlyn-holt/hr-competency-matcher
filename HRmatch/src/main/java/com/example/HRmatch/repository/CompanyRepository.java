package com.example.HRmatch.repository;

import com.example.HRmatch.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

// Этот интерфейс дает методы save, findById и т.д. для таблицы companies
public interface CompanyRepository extends JpaRepository<Company, Long> {
}