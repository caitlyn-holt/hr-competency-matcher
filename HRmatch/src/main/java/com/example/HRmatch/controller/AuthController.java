package com.example.HRmatch.controller;

import com.example.HRmatch.LoginRequest;
import com.example.HRmatch.RegisterRequest;
import com.example.HRmatch.entity.Company;
import com.example.HRmatch.entity.Role;
import com.example.HRmatch.entity.User;
import com.example.HRmatch.repository.CompanyRepository;
import com.example.HRmatch.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, CompanyRepository companyRepo) {
        this.userRepository = userRepository;  // ← Правильное имя!
        this.companyRepo = companyRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            String username = request.getUsername();
            String password = request.getPassword();
            String roleStr = request.getRole();
            Role role = Role.valueOf(roleStr.toUpperCase());

            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(request.getEmail());
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(password));

            if (role == Role.HR) {
                Company company = new Company();
                company.setName(request.getCompanyName() != null ? request.getCompanyName() : "Не указана");
                company.setIndustry(request.getCompanyIndustry());
                company.setLocation(request.getCompanyLocation());
                company.setDescription(request.getCompanyDescription());

                companyRepo.save(company);
                user.setCompany(company);
            }

            userRepository.save(user);
            return ResponseEntity.ok(user);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid role: " + request.getRole());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);  // ← userRepository, не userRepo!
        if (user == null) return ResponseEntity.notFound().build();

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole());

        // Если это HR — добавляем данные компании
        if (user.getRole() == Role.HR && user.getCompany() != null) {
            Map<String, String> company = new HashMap<>();
            company.put("name", user.getCompany().getName());
            company.put("industry", user.getCompany().getIndustry());
            company.put("location", user.getCompany().getLocation());
            company.put("description", user.getCompany().getDescription());
            profile.put("company", company);
        }

        return ResponseEntity.ok(profile);
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable Long id,
            @RequestBody Map<String, String> updates) {

        User user = userRepository.findById(id).orElse(null);  // ← userRepository!
        if (user == null) return ResponseEntity.notFound().build();

        // Обновляем базовые поля
        if (updates.containsKey("username")) {
            user.setUsername(updates.get("username"));
        }
        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }

        // Если это HR и есть данные компании — обновляем компанию
        if (user.getRole() == Role.HR && user.getCompany() != null) {
            Company company = user.getCompany();
            if (updates.containsKey("companyName")) company.setName(updates.get("companyName"));
            if (updates.containsKey("companyIndustry")) company.setIndustry(updates.get("companyIndustry"));
            if (updates.containsKey("companyLocation")) company.setLocation(updates.get("companyLocation"));
            if (updates.containsKey("companyDescription")) company.setDescription(updates.get("companyDescription"));
            companyRepo.save(company);
        }

        userRepository.save(user);  // ← userRepository!
        return ResponseEntity.ok(user);
    }
}