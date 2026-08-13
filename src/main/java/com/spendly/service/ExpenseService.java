package com.spendly.service;

import com.spendly.model.Expense;
import com.spendly.model.ExpenseSummary;
import com.spendly.repository.ExpenseRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    private static final int RECENT_EXPENSE_LIMIT = 5;

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    public ExpenseSummary getSummaryForUser(long userId) {
        List<Expense> expenses = expenseRepository.findByUserId(userId);

        double totalSpent = expenses.stream().mapToDouble(Expense::amount).sum();

        Map<String, Double> categoryTotals = expenses.stream()
                .collect(Collectors.groupingBy(Expense::category,
                        Collectors.summingDouble(Expense::amount)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

        List<Expense> recentExpenses = expenses.stream()
                .limit(RECENT_EXPENSE_LIMIT)
                .toList();

        return new ExpenseSummary(totalSpent, expenses.size(), categoryTotals, recentExpenses);
    }
}
