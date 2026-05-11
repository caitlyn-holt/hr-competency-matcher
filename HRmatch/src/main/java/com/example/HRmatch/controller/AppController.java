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
        // Проверка 1: createdById не должен быть null
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
        v.setSalaryFrom(req.getSalaryFrom());
        v.setSalaryTo(req.getSalaryTo());
        v.setExperienceLevel(req.getExperienceLevel());

        List<VacancyCompetency> reqs = new ArrayList<>();
        double weightSum = 0.0;

        if (req.getRequiredCompetencies() != null) {
            for (CompetencyLevelDto dto : req.getRequiredCompetencies()) {
                if (dto.getId() != null) {
                    Competency c = compRepo.findById(dto.getId()).orElse(null);
                    if (c != null) {
                        // Создаём связь с учётом весов и критичности (согласно отчёту)
                        VacancyCompetency vc = new VacancyCompetency();
                        vc.setVacancy(v);
                        vc.setCompetency(c);
                        vc.setRequiredLevel(dto.getLevel());

                        // Вес по умолчанию 1.0 / количество, если не задан
                        Double weight = dto.getWeight();
                        vc.setWeight(weight != null ? weight : 1.0);
                        weightSum += vc.getWeight();

                        // Критическая компетенция (п. 4.5 отчёта)
                        vc.setIsCritical(dto.getIsCritical() != null ? dto.getIsCritical() : false);
                        vc.setMinRequiredLevel(dto.getMinRequiredLevel());

                        reqs.add(vc);
                    }
                }
            }
        }

        // Валидация: сумма весов должна быть ~1.0 (п. 4.2 отчёта)
        // Допускаем небольшую погрешность из-за float-арифметики
        if (Math.abs(weightSum - 1.0) > 0.01 && !reqs.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Sum of competency weights must be 1.0 (current: " + weightSum + "). Please normalize weights.");
        }

        v.setRequiredCompetencies(reqs);
        vacRepo.save(v); // Cascade = ALL сохранит и связи

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
            CandidateCompetency saved = candCompRepo.save(existing.get());
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
            CompetencyLevelDto dto = new CompetencyLevelDto();
            dto.setId(cc.getCompetency().getId());
            dto.setLevel(cc.getLevel());
            // Остальные поля можно не задавать, они будут null
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
    // 4. Алгоритм подбора
    @GetMapping("/match/{candidateId}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> findMatches(@PathVariable Long candidateId) {
        // 1. Получаем профиль компетенций кандидата
        List<CandidateCompetency> skills = candCompRepo.findByCandidateId(candidateId);
        if (skills.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        // Карта: ID компетенции -> Уровень кандидата
        Map<Long, Integer> candidateMap = new HashMap<>();
        for (CandidateCompetency s : skills) {
            candidateMap.put(s.getCompetency().getId(), s.getLevel());
        }

        List<Vacancy> vacancies = vacRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Vacancy v : vacancies) {
            if (v.getRequiredCompetencies() == null || v.getRequiredCompetencies().isEmpty()) {
                continue;
            }

            double weightedScore = 0.0;
            boolean criticalFail = false;
            List<Map<String, Object>> breakdown = new ArrayList<>();

            // 2. Расчёт по каждой компетенции вакансии
            for (VacancyCompetency req : v.getRequiredCompetencies()) {
                Long compId = req.getCompetency().getId();
                int candidateLevel = candidateMap.getOrDefault(compId, 0);
                int requiredLevel = req.getRequiredLevel() > 0 ? req.getRequiredLevel() : 1; // Защита от деления на 0
                double weight = req.getWeight() != null ? req.getWeight() : 1.0;

                // Параметры критической компетенции
                boolean isCritical = Boolean.TRUE.equals(req.getIsCritical());
                int minLevel = req.getMinRequiredLevel() != null ? req.getMinRequiredLevel() : 0;

                // Проверка критического порога (п. 4.5 отчёта)
                if (isCritical && candidateLevel < minLevel) {
                    criticalFail = true;
                }

                // Формула 4.3: ci = min(si / ri, 1)
                double ci = Math.min((double) candidateLevel / requiredLevel, 1.0);

                // Формула 4.4: R = Σ(wi * ci)
                weightedScore += weight * ci;

                // Формируем детализацию для Android
                Map<String, Object> detail = new HashMap<>();
                detail.put("competencyId", compId);
                detail.put("competencyName", req.getCompetency().getName());
                detail.put("candidateLevel", candidateLevel);
                detail.put("requiredLevel", requiredLevel);
                detail.put("matchPercent", Math.round(ci * 100));
                if (isCritical) {
                    detail.put("isCritical", true);
                    detail.put("metCritical", candidateLevel >= minLevel);
                }
                breakdown.add(detail);
            }

            // Если провалена критическая компетенция -> общий процент 0
            double finalScore = criticalFail ? 0.0 : weightedScore;
            int percent = (int) Math.round(finalScore * 100);

            // Классификация (п. 4.5 отчёта)
            String matchLevel;
            if (criticalFail) {
                matchLevel = "REJECTED";
            } else if (percent >= 80) {
                matchLevel = "HIGH";
            } else if (percent >= 50) {
                matchLevel = "MEDIUM";
            } else {
                matchLevel = "LOW";
            }

            // Собираем ответ
            Map<String, Object> r = new HashMap<>();
            r.put("id", v.getId());
            r.put("title", v.getTitle());
            r.put("description", v.getDescription());
            r.put("matchPercentage", percent); // int безопаснее для JSON и сортировки
            r.put("matchLevel", matchLevel);
            r.put("breakdown", breakdown);
            result.add(r);
        }

        // 3. Сортировка по убыванию процента (используем Integer.compare во избежание ClassCastException)
        result.sort((a, b) -> Integer.compare(
                (Integer) b.get("matchPercentage"),
                (Integer) a.get("matchPercentage")
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

    @PostConstruct
    public void init() {
        if (compRepo.count() == 0) {
            // Формат: { Имя, Категория }
            String[][] data = {
                    // IT
                    {"Java", "IT"}, {"Python", "IT"}, {"SQL", "IT"}, {"Docker", "IT"}, {"Git", "IT"},
                    // Менеджмент
                    {"Project Management", "Management"}, {"Agile", "Management"}, {"Leadership", "Management"},
                    // HR
                    {"Recruitment", "HR"}, {"Interviewing", "HR"}, {"HR Branding", "HR"},
                    // Продажи
                    {"B2B Sales", "Sales"}, {"Negotiation", "Sales"}, {"CRM", "Sales"},
                    // Дизайн
                    {"Figma", "Design"}, {"UI/UX", "Design"}, {"Photoshop", "Design"}
            };

            for (String[] item : data) {
                if (compRepo.findByName(item[0]).isEmpty()) {
                    Competency comp = new Competency();
                    comp.setName(item[0]);
                    comp.setCategory(item[1]);
                    compRepo.save(comp);
                    System.out.println("Added: " + item[0]);
                }
            }
        }
    }

    @GetMapping("/hr/{hrId}/applications")
    public ResponseEntity<?> getHRApplications(@PathVariable Long hrId) {
        // Находим все вакансии этого HR
        List<Vacancy> myVacs = vacRepo.findByCreatedById(hrId);
        List<Long> vacancyIds = myVacs.stream().map(Vacancy::getId).toList();

        // Находим все отклики на эти вакансии
        List<Application> allApps = appRepo.findByVacancyIdIn(vacancyIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Application app : allApps) {
            User candidate = app.getCandidate();
            Vacancy vacancy = app.getVacancy();

            // Получаем навыки кандидата
            List<CandidateCompetency> candidateSkills = candCompRepo.findByCandidateId(candidate.getId());
            List<Map<String, Object>> skills = new ArrayList<>();
            for (CandidateCompetency cc : candidateSkills) {
                Map<String, Object> s = new HashMap<>();
                s.put("name", cc.getCompetency().getName());
                s.put("level", cc.getLevel());
                skills.add(s);
            }

            // Считаем процент соответствия (упрощённо)
            int matchPercent = calculateMatchPercent(candidateSkills, vacancy.getRequiredCompetencies());

            Map<String, Object> m = new HashMap<>();
            m.put("applicationId", app.getId());
            m.put("vacancyId", vacancy.getId());
            m.put("vacancyTitle", vacancy.getTitle());

            m.put("candidateId", candidate.getId());
            m.put("candidateName", candidate.getUsername());
            m.put("candidateEmail", candidate.getEmail());
            m.put("candidateSkills", skills);
            m.put("matchPercent", matchPercent);

            m.put("message", app.getMessage());
            m.put("status", app.getStatus());
            m.put("appliedAt", app.getAppliedAt());

            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    // Вспомогательный метод для расчёта % совпадения
    private int calculateMatchPercent(List<CandidateCompetency> candidateSkills, List<VacancyCompetency> required) {
        if (required == null || required.isEmpty()) return 0;

        Map<Long, Integer> candidateMap = new HashMap<>();
        for (CandidateCompetency cs : candidateSkills) {
            candidateMap.put(cs.getCompetency().getId(), cs.getLevel());
        }

        double total = 0, score = 0;
        for (VacancyCompetency req : required) {
            total += req.getRequiredLevel();
            Integer actual = candidateMap.get(req.getCompetency().getId());
            if (actual != null) {
                score += Math.min(actual, req.getRequiredLevel());
            }
        }

        return total > 0 ? (int) Math.round((score / total) * 100) : 0;
    }
    // Обновить статус отклика
    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Application app = appRepo.findById(id).orElse(null);
        if (app == null) return ResponseEntity.notFound().build();

        String newStatus = request.get("status");
        if (newStatus != null && List.of("NEW", "VIEWED", "ACCEPTED", "REJECTED").contains(newStatus)) {
            app.setStatus(newStatus);
            appRepo.save(app);
            return ResponseEntity.ok("Status updated");
        }
        return ResponseEntity.badRequest().body("Invalid status");
    }
}
