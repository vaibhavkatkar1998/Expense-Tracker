package com.spendly.service;

import com.spendly.model.Expense;
import com.spendly.model.ExpenseSummary;
import com.spendly.model.User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    public User getUser() {
        return new User(1, "Vaibhav Katkar", "vaibhav.katkar2302@gmail.com",
                "placeholder-hash", LocalDateTime.of(2026, 1, 10, 0, 0));
    }

    public ExpenseSummary getSummary() {
        List<Expense> expenses = List.of(
                new Expense(1, 1, 45.00, "Food", "2026-08-10", "Groceries", null),
                new Expense(2, 1, 32.50, "Transport", "2026-08-09", "Fuel", null),
                new Expense(3, 1, 95.00, "Entertainment", "2026-08-07", "Movie night", null),
                new Expense(4, 1, 120.75, "Utilities", "2026-08-05", "Electricity bill", null),
                new Expense(5, 1, 60.00, "Food", "2026-08-03", "Dinner out", null)
        );

        double totalSpent = expenses.stream().mapToDouble(Expense::amount).sum();

        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::category,
                        Collectors.summingDouble(Expense::amount)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        return new ExpenseSummary(totalSpent, expenses.size(), categoryTotals, expenses);
    }

    public String getInitials() {
        String[] parts = getUser().name().trim().split("\\s+");
        String initials = parts[0].substring(0, 1);
        if (parts.length > 1) {
            initials += parts[parts.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    public String getTopCategory() {
        return getSummary().categoryTotals().keySet().stream()
                .findFirst()
                .orElse("N/A");
    }
}
