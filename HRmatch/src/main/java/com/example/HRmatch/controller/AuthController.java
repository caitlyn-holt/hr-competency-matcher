package com.example.HRmatch.controller;

import com.example.HRmatch.entity.Role;
import com.example.HRmatch.entity.User;
import com.example.HRmatch.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            User user = new User();
            user.setUsername(request.get("username"));
            user.setPassword(request.get("password"));
            user.setRole(Role.valueOf(request.get("role").toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "OK", "id", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}