package com.example.HRmatch.controller;

import com.example.HRmatch.entity.*;
import com.example.HRmatch.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/match")
public class MatchingController {

    private final CandidateCompetencyRepository candidateRepo;
    private final VacancyRepository vacancyRepo;
    private final UserRepository userRepo;
    private final CompetencyRepository compRepo;

    public MatchingController(CandidateCompetencyRepository candidateRepo, VacancyRepository vacancyRepo,
                              UserRepository userRepo, CompetencyRepository compRepo) {
        this.candidateRepo = candidateRepo;
        this.vacancyRepo = vacancyRepo;
        this.userRepo = userRepo;
        this.compRepo = compRepo;
    }

    @GetMapping("/{candidateId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> findMatches(@PathVariable Long candidateId) {
        List<CandidateCompetency> skills = candidateRepo.findByCandidateId(candidateId);
        if (skills.isEmpty()) return ResponseEntity.badRequest().body("No skills found");

        Map<Long, Integer> map = new HashMap<>();
        for (CandidateCompetency s : skills) map.put(s.getCompetency().getId(), s.getLevel());

        List<Vacancy> vacancies = vacancyRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vacancy v : vacancies) {
            if (v.getRequiredCompetencies() == null) continue;
            double total = 0, score = 0;
            int count = 0;
            for (VacancyCompetency req : v.getRequiredCompetencies()) {
                total += req.getRequiredLevel();
                if (map.containsKey(req.getCompetency().getId())) {
                    score += Math.min(map.get(req.getCompetency().getId()), req.getRequiredLevel());
                    count++;
                }
            }
            double percent = total > 0 ? (score / total) * 100 : 0;

            Map<String, Object> r = new HashMap<>();
            r.put("title", v.getTitle());
            r.put("matchPercentage", Math.round(percent));
            r.put("matchedSkills", count + "/" + v.getRequiredCompetencies().size());
            result.add(r);
        }
        result.sort((a, b) -> Double.compare((Double)b.get("matchPercentage"), (Double)a.get("matchPercentage")));
        return ResponseEntity.ok(result);
    }

    // Тестовые данные
    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<?> seed() {
        if (userRepo.count() > 0) return ResponseEntity.ok("Data already exists");

        User hr = userRepo.save(new User(null, "admin", "123", Role.HR));
        User cand = userRepo.save(new User(null, "user", "123", Role.CANDIDATE));

        Competency java = compRepo.save(new Competency(null, "Java"));
        Competency sql = compRepo.save(new Competency(null, "SQL"));

        Vacancy v = new Vacancy();
        v.setTitle("Java Developer");
        v.setCreatedBy(hr);
        v.setRequiredCompetencies(List.of(
                new VacancyCompetency(null, v, java, 4),
                new VacancyCompetency(null, v, sql, 3)
        ));
        vacancyRepo.save(v);

        candidateRepo.save(new CandidateCompetency(null, cand, java, 5));
        candidateRepo.save(new CandidateCompetency(null, cand, sql, 2));

        return ResponseEntity.ok("Data seeded");
    }
}