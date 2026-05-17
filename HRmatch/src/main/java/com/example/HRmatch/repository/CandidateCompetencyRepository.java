package com.example.HRmatch.repository;

import com.example.HRmatch.entity.CandidateCompetency;
import com.example.HRmatch.entity.Competency;
import com.example.HRmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidateCompetencyRepository extends JpaRepository<CandidateCompetency, Long> {
    List<CandidateCompetency> findByCandidateId(Long candidateId);
    Optional<CandidateCompetency> findByCandidateAndCompetency(User candidate, Competency competency);
    void deleteByCandidateId(Long candidateId);
}