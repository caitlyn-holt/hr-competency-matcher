package com.example.HRmatch.repository;

import com.example.HRmatch.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByVacancyId(Long vacancyId);
}