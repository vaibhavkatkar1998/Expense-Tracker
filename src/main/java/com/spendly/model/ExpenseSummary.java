package com.spendly.model;

import java.util.List;
import java.util.Map;

public record ExpenseSummary(double totalSpent, int transactionCount,
                              Map<String, Double> categoryTotals,
                              List<Expense> recentExpenses) {}
