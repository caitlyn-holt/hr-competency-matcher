// CompetencyRepository.java
package com.example.HRmatch.repository;
import com.example.HRmatch.entity.Competency;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CompetencyRepository extends JpaRepository<Competency, Long> {}