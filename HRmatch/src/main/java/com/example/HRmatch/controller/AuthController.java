package com.example.HRmatch.controller;

import com.example.HRmatch.LoginRequest;
import com.example.HRmatch.entity.Company;
import com.example.HRmatch.entity.Role;
import com.example.HRmatch.entity.User;
import com.example.HRmatch.repository.CompanyRepository;
import com.example.HRmatch.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserRepository userRepository;
    private final CompanyRepository companyRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, CompanyRepository companyRepo) {
        this.userRepository = userRepository;
        this.companyRepo = companyRepo;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String roleStr = request.get("role");
            Role role = Role.valueOf(roleStr.toUpperCase());

            // 1. Проверяем, нет ли уже такого юзера
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            User user = new User();
            user.setUsername(username);
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(password));

            if (role == Role.HR) {
                Company company = new Company();
                company.setName(request.getOrDefault("companyName", "Не указана"));
                company.setIndustry(request.getOrDefault("companyIndustry", ""));
                company.setLocation(request.getOrDefault("companyLocation", ""));
                company.setDescription(request.getOrDefault("companyDescription", ""));
                companyRepo.save(company);
                user.setCompany(company);
            }

            // 3. Сохраняем пользователя
            userRepository.save(user);
            return ResponseEntity.ok(user);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepository.findByUsername(req.getUsername());

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                return ResponseEntity.ok(user);
            }
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }
}