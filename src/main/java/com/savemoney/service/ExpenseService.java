package com.savemoney.service;

import com.savemoney.entity.Expense;
import com.savemoney.entity.User;
import com.savemoney.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExpenseService {
    
    @Autowired
    private ExpenseRepository expenseRepository;
    
    public Expense addExpense(Expense expense) {
        return expenseRepository.save(expense);
    }
    
    public List<Expense> getUserExpenses(User user) {
        return expenseRepository.findByUserOrderByDateDescIdDesc(user);
    }
    
    public Expense updateExpense(Long id, Expense updatedExpense, User user) {
        Expense existing = expenseRepository.findById(id).orElse(null);
        if (existing != null && existing.getUser().getId().equals(user.getId())) {
            existing.setCategory(updatedExpense.getCategory());
            existing.setDescription(updatedExpense.getDescription());
            existing.setAmount(updatedExpense.getAmount());
            existing.setDate(updatedExpense.getDate());
            existing.setTime(updatedExpense.getTime());
            return expenseRepository.save(existing);
        }
        return null;
    }
    
    public boolean deleteExpense(Long id, User user) {
        Expense expense = expenseRepository.findById(id).orElse(null);
        if (expense != null && expense.getUser().getId().equals(user.getId())) {
            expenseRepository.delete(expense);
            return true;
        }
        return false;
    }
    
    public Map<String, Double> getCategorySummary(User user) {
        List<Object[]> results = expenseRepository.getCategorySummary(user);
        Map<String, Double> summary = new HashMap<>();
        for (Object[] result : results) {
            summary.put((String) result[0], (Double) result[1]);
        }
        return summary;
    }
    
    public Double getMonthlyTotal(User user, int year, int month) {
        Double total = expenseRepository.sumByUserAndMonth(user, year, month);
        return total != null ? total : 0.0;
    }
}