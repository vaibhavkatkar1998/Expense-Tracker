package com.spendly.model;

import java.time.LocalDateTime;

public record Expense(long id, long userId, double amount, String category, String date, String description, LocalDateTime createdAt) {}
