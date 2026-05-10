package com.example.HRmatch.repository;

import com.example.HRmatch.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CompetencyRepository extends JpaRepository<Competency, Long> {
    Optional<Competency> findByName(String name);
}