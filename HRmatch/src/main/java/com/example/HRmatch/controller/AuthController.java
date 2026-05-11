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
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request, BindingResult result) {
        // Проверка валидации
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        try {
            String username = request.getUsername();
            String password = request.getPassword();
            String roleStr = request.getRole();
            Role role = Role.valueOf(roleStr.toUpperCase());

            // Проверка на существующего пользователя
            if (userRepository.findByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }

            User user = new User();
            user.setUsername(username);
            user.setEmail(request.getEmail());
            user.setRole(role);
            user.setPassword(passwordEncoder.encode(password));

            // Если это HR — создаём компанию
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
}