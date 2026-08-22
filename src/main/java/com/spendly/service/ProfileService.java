package com.spendly.service;

import com.spendly.model.Expense;
import com.spendly.model.ExpenseSummary;
import com.spendly.model.ProfileStats;
import com.spendly.model.User;
import com.spendly.repository.ExpenseRepository;
import com.spendly.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final ExpenseService expenseService;
    private final ExpenseRepository expenseRepository;

    public ProfileService(UserRepository userRepository, ExpenseService expenseService, ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.expenseService = expenseService;
        this.expenseRepository = expenseRepository;
    }

    public User getUser(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    public String getInitials(User user) {
        String[] parts = user.name().trim().split("\\s+");
        String initials = parts[0].substring(0, 1);
        if (parts.length > 1) {
            initials += parts[parts.length - 1].substring(0, 1);
        }
        return initials.toUpperCase();
    }

    public List<Expense> getRecentExpenses(long userId, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        return expenseRepository.findByUserId(userId, pageSize, offset);
    }

    public ProfileStats getSummaryStats(long userId) {
        ExpenseSummary summary = expenseService.getSummaryForUser(userId);
        String topCategory = summary.categoryTotals().keySet().stream()
                .findFirst()
                .orElse("N/A");
        return new ProfileStats(summary.totalSpent(), summary.transactionCount(), topCategory);
    }

    public Map<String, Double> getCategoryBreakdown(long userId) {
        return expenseService.getSummaryForUser(userId).categoryTotals();
    }
}
