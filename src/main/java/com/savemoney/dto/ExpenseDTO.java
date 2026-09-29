package com.savemoney.dto;

import java.time.LocalDate;

public class ExpenseDTO {
    private Long id;
    private String category;
    private String description;
    private Double amount;
    private LocalDate date;
    private String time;
    
    // Constructors
    public ExpenseDTO() {}
    
    public ExpenseDTO(Long id, String category, String description, Double amount, LocalDate date, String time) {
        this.id = id;
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
        this.time = time;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}