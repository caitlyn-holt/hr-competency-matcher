// VacancyRepository.java
package com.example.HRmatch.repository;
import com.example.HRmatch.entity.Vacancy;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VacancyRepository extends JpaRepository<Vacancy, Long> {}