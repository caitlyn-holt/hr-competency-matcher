package com.example.HRmatch.controller;
import com.example.HRmatch.ApplicationRequest;
import com.example.HRmatch.CompetencyLevelDto;
import com.example.HRmatch.CreateVacancyRequest;
import com.example.HRmatch.entity.*;
import com.example.HRmatch.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AppController {

    private final UserRepository userRepo;
    private final CompetencyRepository compRepo;
    private final VacancyRepository vacRepo;
    private final CandidateCompetencyRepository candCompRepo;
    private final ApplicationRepository appRepo; // Новый репозиторий

    public AppController(UserRepository userRepo, CompetencyRepository compRepo,
                         VacancyRepository vacRepo, CandidateCompetencyRepository candCompRepo,
                         ApplicationRepository appRepo) {
        this.userRepo = userRepo;
        this.compRepo = compRepo;
        this.vacRepo = vacRepo;
        this.candCompRepo = candCompRepo;
        this.appRepo = appRepo;
    }

    // 1. Список компетенций
    @GetMapping("/competencies")
    public ResponseEntity<?> getCompetencies() {
        return ResponseEntity.ok(compRepo.findAll());
    }

    // 2. Создание вакансии (ИСПРАВЛЕНО: убрали try-catch, чтобы не было RollbackException)
    @PostMapping("/vacancies")
    public ResponseEntity<?> createVacancy(@RequestBody CreateVacancyRequest req) {
        System.out.println("=== CREATE VACANCY REQUEST ===");
        System.out.println("Title: " + req.getTitle());
        System.out.println("CreatedById: " + req.getCreatedById());
        System.out.println("Skills count: " + (req.getRequiredCompetencies() != null ? req.getRequiredCompetencies().size() : 0));

        if (req.getCreatedById() == null) {
            return ResponseEntity.badRequest().body("Missing createdById. Android app must send user ID.");
        }

        User hr = userRepo.findById(req.getCreatedById()).orElse(null);
        if (hr == null) {
            return ResponseEntity.badRequest().body("HR user with id " + req.getCreatedById() + " not found");
        }

        Vacancy v = new Vacancy();
        v.setTitle(req.getTitle());
        v.setDescription(req.getDescription());
        v.setCreatedBy(hr);

        List<VacancyCompetency> reqs = new ArrayList<>();
        if (req.getRequiredCompetencies() != null) {
            for (CompetencyLevelDto dto : req.getRequiredCompetencies()) {
                if (dto.getId() != null) { // Проверка на null ID компетенции
                    Competency c = compRepo.findById(dto.getId()).orElse(null);
                    if (c != null) {
                        reqs.add(new VacancyCompetency(null, v, c, dto.getLevel()));
                    }
                }
            }
        }
        v.setRequiredCompetencies(reqs);

        vacRepo.save(v);
        return ResponseEntity.ok(v);
    }

    // 3. Добавление навыка кандидату (ТОЖЕ ИСПРАВЛЕНО)
    @PostMapping("/candidate/{id}/skills")
    public ResponseEntity<?> addSkill(@PathVariable Long id, @RequestBody CompetencyLevelDto dto) {
        System.out.println("=== ADD SKILL REQUEST ===");
        System.out.println("Candidate ID: " + id);
        System.out.println("Competency ID: " + dto.getId());
        System.out.println("Level: " + dto.getLevel());

        User cand = userRepo.findById(id).orElse(null);
        if (cand == null) {
            System.out.println("ERROR: User not found");
            return ResponseEntity.badRequest().body("User not found");
        }

        Competency comp = compRepo.findById(dto.getId()).orElse(null);
        if (comp == null) {
            System.out.println("ERROR: Competency not found");
            return ResponseEntity.badRequest().body("Competency not found");
        }

        Optional<CandidateCompetency> existing = candCompRepo.findByCandidateAndCompetency(cand, comp);
        if (existing.isPresent()) {
            System.out.println("UPDATING existing skill: old level=" + existing.get().getLevel() + ", new level=" + dto.getLevel());
            existing.get().setLevel(dto.getLevel());
            CandidateCompetency saved = candCompRepo.save(existing.get()); // ✅ Явно сохраняем
            System.out.println("SAVED: " + saved.getId() + ", level=" + saved.getLevel());
        } else {
            System.out.println("CREATING new skill");
            CandidateCompetency saved = candCompRepo.save(new CandidateCompetency(null, cand, comp, dto.getLevel()));
            System.out.println("SAVED: " + saved.getId() + ", level=" + saved.getLevel());
        }

        return ResponseEntity.ok("Skill added");
    }
    // GET /api/candidate/{id}/skills - получить сохранённые навыки пользователя
    @GetMapping("/candidate/{id}/skills")
    public ResponseEntity<?> getUserSkills(@PathVariable Long id) {
        List<CandidateCompetency> skills = candCompRepo.findByCandidateId(id);
        List<CompetencyLevelDto> result = new ArrayList<>();
        for (CandidateCompetency cc : skills) {
            result.add(new CompetencyLevelDto(cc.getCompetency().getId(), cc.getLevel()));
        }
        return ResponseEntity.ok(result);
    }
    // 4. Алгоритм подбора
    @GetMapping("/match/{candidateId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> findMatches(@PathVariable Long candidateId) {
        List<CandidateCompetency> skills = candCompRepo.findByCandidateId(candidateId);
        if (skills.isEmpty()) return ResponseEntity.ok(new ArrayList<>());

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
            r.put("matchPercentage", percent);
            r.put("matchedSkills", count + "/" + v.getRequiredCompetencies().size());
            result.add(r);
        }

        result.sort((a, b) -> Double.compare(
                (Double) b.get("matchPercentage"),
                (Double) a.get("matchPercentage")
        ));

        return ResponseEntity.ok(result);
    }

    // 2. Получить вакансии конкретного HR (Мои вакансии)
    @GetMapping("/hr/vacancies/{hrId}")
    public ResponseEntity<?> getMyVacancies(@PathVariable Long hrId) {
        List<Vacancy> myVacs = vacRepo.findByCreatedById(hrId);
        List<Map<String, Object>> safeList = new ArrayList<>();
        for (Vacancy v : myVacs) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", v.getId());
            m.put("title", v.getTitle());
            m.put("description", v.getDescription());
            safeList.add(m);
        }
        return ResponseEntity.ok(safeList);
    }

    // 3. Удалить вакансию
    @DeleteMapping("/hr/vacancies/{id}")
    public ResponseEntity<?> deleteVacancy(@PathVariable Long id) {
        if (vacRepo.existsById(id)) {
            vacRepo.deleteById(id);
            return ResponseEntity.ok("Vacancy deleted");
        }
        return ResponseEntity.notFound().build();
    }

    // 4. Кандидат откликается на вакансию
    @PostMapping("/applications")
    public ResponseEntity<?> apply(@RequestBody ApplicationRequest req) {
        Vacancy v = vacRepo.findById(req.getVacancyId()).orElse(null);
        User c = userRepo.findById(req.getCandidateId()).orElse(null);

        if (v == null || c == null) return ResponseEntity.badRequest().body("User or Vacancy not found");

        Application app = new Application();
        app.setVacancy(v);
        app.setCandidate(c);
        app.setStatus("PENDING");
        app.setMessage(req.getMessage());
        appRepo.save(app);

        return ResponseEntity.ok("Application sent successfully!");
    }

    // 5. HR смотрит отклики на свою вакансию
    @GetMapping("/hr/vacancies/{vacancyId}/applications")
    public ResponseEntity<?> getApplications(@PathVariable Long vacancyId) {
        List<Application> apps = appRepo.findByVacancyId(vacancyId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application app : apps) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", app.getId());
            m.put("candidateName", app.getCandidate().getUsername());
            m.put("candidateId", app.getCandidate().getId());
            m.put("status", app.getStatus());
            m.put("message", app.getMessage());
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // Расширенная инициализация данных (Много компетенций)
    @PostConstruct
    public void init() {
        // Список всех компетенций для добавления
        String[] allComps = {
                "Java", "Python", "C++", "JavaScript", "React", "SQL", "Docker", "AWS",
                "Figma", "Adobe Photoshop", "UI/UX Design", "Marketing", "SEO",
                "Sales", "Accounting", "Management", "Leadership", "Communication"
        };

        for (String name : allComps) {
            // Проверяем, есть ли уже такая компетенция по имени
            if (compRepo.findByName(name).isEmpty()) {
                compRepo.save(new Competency(null, name));
                System.out.println("Added competency: " + name);
            }
        }
    }
}
