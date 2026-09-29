package com.savemoney.service;

import com.savemoney.entity.User;
import com.savemoney.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // ✅ FIXED

    // ✅ REGISTER USER
    public User registerUser(String fullName, String email, String password, String about) {

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists!");
        }

        String encodedPassword = passwordEncoder.encode(password);

        User user = new User(fullName, email, encodedPassword, about);

        return userRepository.save(user);
    }

    // ✅ LOGIN / AUTHENTICATION
    public Optional<User> authenticate(String email, String password) {

        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent() &&
                passwordEncoder.matches(password, userOpt.get().getPassword())) {

            return userOpt;
        }

        return Optional.empty();
    }

    // ✅ FIND BY EMAIL
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // ✅ FIXED: RETURN OPTIONAL
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}