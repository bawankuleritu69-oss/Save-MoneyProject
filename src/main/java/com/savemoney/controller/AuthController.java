package com.savemoney.controller;

import com.savemoney.dto.LoginRequest;
import com.savemoney.dto.SignupRequest;
import com.savemoney.entity.User;
import com.savemoney.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SignupRequest request) {
        try {
            User user = userService.registerUser(
                    request.getFullName(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getAbout()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful!");
            response.put("userId", user.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", e.getMessage())
            );
        }
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {

        Optional<User> userOpt =
                userService.authenticate(request.getEmail(), request.getPassword());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("userName", user.getFullName());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Login successful!",
                    "userId", user.getId(),
                    "userName", user.getFullName()
            ));
        }

        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid email or password!"
        ));
    }

    // ✅ LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    // ✅ CURRENT USER
    @GetMapping("/current-user")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null && session.getAttribute("userId") != null) {
            return ResponseEntity.ok(Map.of(
                    "loggedIn", true,
                    "userId", session.getAttribute("userId"),
                    "userName", session.getAttribute("userName"),
                    "userEmail", session.getAttribute("userEmail")
            ));
        }

        return ResponseEntity.ok(Map.of("loggedIn", false));
    }
}