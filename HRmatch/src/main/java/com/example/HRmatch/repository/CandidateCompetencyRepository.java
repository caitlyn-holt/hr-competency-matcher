// CandidateCompetencyRepository.java
package com.example.HRmatch.repository;
import com.example.HRmatch.entity.CandidateCompetency;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CandidateCompetencyRepository extends JpaRepository<CandidateCompetency, Long> {
    List<CandidateCompetency> findByCandidateId(Long candidateId);
}