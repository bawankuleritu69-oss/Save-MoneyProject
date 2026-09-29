package com.savemoney.controller;

import com.savemoney.dto.ExpenseDTO;
import com.savemoney.entity.Expense;
import com.savemoney.entity.User;
import com.savemoney.service.ExpenseService;
import com.savemoney.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserService userService;

    // ✅ FIXED
    private User getAuthenticatedUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;

        return userService.findById(userId).orElse(null);
    }

    @GetMapping
    public ResponseEntity<?> getAllExpenses(HttpSession session) {

        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<Expense> expenses = expenseService.getUserExpenses(user);

        List<ExpenseDTO> expenseDTOs = expenses.stream()
                .map(e -> new ExpenseDTO(
                        e.getId(),
                        e.getCategory(),
                        e.getDescription(),
                        e.getAmount(),
                        e.getDate(),
                        e.getTime()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(expenseDTOs);
    }

    @PostMapping
    public ResponseEntity<?> addExpense(@RequestBody ExpenseDTO expenseDTO, HttpSession session) {

        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Expense expense = new Expense(
                expenseDTO.getCategory(),
                expenseDTO.getDescription(),
                expenseDTO.getAmount(),
                expenseDTO.getDate(),
                expenseDTO.getTime(),
                user
        );

        Expense saved = expenseService.addExpense(expense);

        return ResponseEntity.ok(new ExpenseDTO(
                saved.getId(),
                saved.getCategory(),
                saved.getDescription(),
                saved.getAmount(),
                saved.getDate(),
                saved.getTime()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id,
                                           @RequestBody ExpenseDTO expenseDTO,
                                           HttpSession session) {

        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Expense expense = new Expense(
                expenseDTO.getCategory(),
                expenseDTO.getDescription(),
                expenseDTO.getAmount(),
                expenseDTO.getDate(),
                expenseDTO.getTime(),
                user
        );
        expense.setId(id);

        Expense updated = expenseService.updateExpense(id, expense, user);

        if (updated != null) {
            return ResponseEntity.ok(new ExpenseDTO(
                    updated.getId(),
                    updated.getCategory(),
                    updated.getDescription(),
                    updated.getAmount(),
                    updated.getDate(),
                    updated.getTime()
            ));
        }

        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, HttpSession session) {

        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        boolean deleted = expenseService.deleteExpense(id, user);

        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpSession session) {

        User user = getAuthenticatedUser(session);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<Expense> allExpenses = expenseService.getUserExpenses(user);

        Map<String, Object> stats = new HashMap<>();
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        double todayTotal = allExpenses.stream()
                .filter(e -> e.getDate() != null && e.getDate().equals(today))
                .mapToDouble(Expense::getAmount)
                .sum();

        double weekTotal = allExpenses.stream()
                .filter(e -> e.getDate() != null &&
                        !e.getDate().isBefore(startOfWeek) &&
                        !e.getDate().isAfter(today))
                .mapToDouble(Expense::getAmount)
                .sum();

        double monthTotal = allExpenses.stream()
                .filter(e -> e.getDate() != null &&
                        !e.getDate().isBefore(startOfMonth) &&
                        !e.getDate().isAfter(today))
                .mapToDouble(Expense::getAmount)
                .sum();

        Map<String, Double> categorySummary = expenseService.getCategorySummary(user);

        stats.put("todayTotal", todayTotal);
        stats.put("weekTotal", weekTotal);
        stats.put("monthTotal", monthTotal);
        stats.put("categorySummary", categorySummary);

        return ResponseEntity.ok(stats);
    }
}