package com.example.HRmatch.controller;

import com.example.HRmatch.CompetencyLevelDto;
import com.example.HRmatch.CreateVacancyRequest;
import com.example.HRmatch.entity.*;
import com.example.HRmatch.repository.*;
import org.springframework.http.ResponseEntity;
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

    // 1. Список компетенций
    @GetMapping("/competencies")
    public ResponseEntity<?> getCompetencies() {
        return ResponseEntity.ok(compRepo.findAll());
    }

    // 2. Создание вакансии (ИСПРАВЛЕНО: убрали try-catch, чтобы не было RollbackException)
    @PostMapping("/vacancies")
    public ResponseEntity<?> createVacancy(@RequestBody CreateVacancyRequest req) {
        // Проверяем, существует ли HR
        User hr = userRepo.findById(req.getCreatedById()).orElse(null);
        if (hr == null) {
            return ResponseEntity.badRequest().body("HR user not found");
        }

        Vacancy v = new Vacancy();
        v.setTitle(req.getTitle());
        v.setDescription(req.getDescription());
        v.setCreatedBy(hr);

        List<VacancyCompetency> reqs = new ArrayList<>();
        for (CompetencyLevelDto dto : req.getRequiredCompetencies()) {
            Competency c = compRepo.findById(dto.getId()).orElse(null);
            if (c != null) {
                // Добавляем связь только если компетенция найдена
                reqs.add(new VacancyCompetency(null, v, c, dto.getLevel()));
            }
        }
        v.setRequiredCompetencies(reqs);

        vacRepo.save(v); // Cascade = ALL сохранит и вакансии, и связи
        return ResponseEntity.ok(v);
    }

    // 3. Добавление навыка кандидату (ТОЖЕ ИСПРАВЛЕНО)
    @PostMapping("/candidate/{id}/skills")
    public ResponseEntity<?> addSkill(@PathVariable Long id, @RequestBody CompetencyLevelDto dto) {
        User cand = userRepo.findById(id).orElse(null);
        if (cand == null) return ResponseEntity.badRequest().body("User not found");

        Competency comp = compRepo.findById(dto.getId()).orElse(null);
        if (comp == null) return ResponseEntity.badRequest().body("Competency not found");

        Optional<CandidateCompetency> existing = candCompRepo.findByCandidateAndCompetency(cand, comp);
        if (existing.isPresent()) {
            existing.get().setLevel(dto.getLevel()); // Обновляем
        } else {
            candCompRepo.save(new CandidateCompetency(null, cand, comp, dto.getLevel())); // Создаём
        }
        return ResponseEntity.ok("Skill added");
    }

    // 4. Алгоритм подбора
    @GetMapping("/match/{candidateId}")
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

    // 5. Авто-заполнение базы
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