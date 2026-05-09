package com.example.HRmatch.controller;

import com.example.HRmatch.CompetencyLevelDto;
import com.example.HRmatch.CreateVacancyRequest;
import com.example.HRmatch.entity.*;
import com.example.HRmatch.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AppController {

    private final UserRepository userRepo;
    private final CompetencyRepository compRepo;
    private final VacancyRepository vacRepo;
    private final CandidateCompetencyRepository candCompRepo;

    public AppController(UserRepository userRepo, CompetencyRepository compRepo,
                         VacancyRepository vacRepo, CandidateCompetencyRepository candCompRepo) {
        this.userRepo = userRepo;
        this.compRepo = compRepo;
        this.vacRepo = vacRepo;
        this.candCompRepo = candCompRepo;
    }

    @GetMapping("/competencies")
    public ResponseEntity<?> getCompetencies() {
        return ResponseEntity.ok(compRepo.findAll());
    }

    @PostMapping("/vacancies")
    @Transactional
    public ResponseEntity<?> createVacancy(@RequestBody CreateVacancyRequest req) {
        try {
            User hr = userRepo.findById(req.getCreatedById()).orElseThrow();

            Vacancy v = new Vacancy();
            v.setTitle(req.getTitle());
            v.setDescription(req.getDescription());
            v.setCreatedBy(hr);

            List<VacancyCompetency> reqs = new ArrayList<>();
            for (CompetencyLevelDto dto : req.getRequiredCompetencies()) {
                Competency c = compRepo.findById(dto.getId()).orElseThrow();
                reqs.add(new VacancyCompetency(null, v, c, dto.getLevel()));
            }
            v.setRequiredCompetencies(reqs);

            vacRepo.save(v);
            return ResponseEntity.ok(v);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/candidate/{id}/skills")
    @Transactional
    public ResponseEntity<?> addSkill(@PathVariable Long id, @RequestBody CompetencyLevelDto dto) {
        try {
            User cand = userRepo.findById(id).orElseThrow();
            Competency comp = compRepo.findById(dto.getId()).orElseThrow();

            Optional<CandidateCompetency> existing = candCompRepo.findByCandidateAndCompetency(cand, comp);
            if (existing.isPresent()) {
                existing.get().setLevel(dto.getLevel());
            } else {
                candCompRepo.save(new CandidateCompetency(null, cand, comp, dto.getLevel()));
            }
            return ResponseEntity.ok("Skill added");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/match/{candidateId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> findMatches(@PathVariable Long candidateId) {
        List<CandidateCompetency> skills = candCompRepo.findByCandidateId(candidateId);
        if (skills.isEmpty()) return ResponseEntity.badRequest().body("No skills found");

        Map<Long, Integer> map = new HashMap<>();
        for (CandidateCompetency s : skills) map.put(s.getCompetency().getId(), s.getLevel());

        List<Vacancy> vacancies = vacRepo.findAll();
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
            r.put("id", v.getId());
            r.put("title", v.getTitle());
            r.put("description", v.getDescription());
            r.put("matchPercentage", Math.round(percent));
            r.put("matchedSkills", count + "/" + v.getRequiredCompetencies().size());
            result.add(r);
        }
        result.sort((a, b) -> Double.compare((Double)b.get("matchPercentage"), (Double)a.get("matchPercentage")));
        return ResponseEntity.ok(result);
    }

    @PostConstruct
    public void init() {
        if (compRepo.count() == 0) {
            compRepo.save(new Competency(null, "Java"));
            compRepo.save(new Competency(null, "SQL"));
            compRepo.save(new Competency(null, "Python"));
            compRepo.save(new Competency(null, "English"));
            compRepo.save(new Competency(null, "Management"));
        }
    }
}