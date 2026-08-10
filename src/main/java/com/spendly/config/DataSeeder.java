package com.spendly.config;

import com.spendly.repository.ExpenseRepository;
import com.spendly.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataSeeder(UserRepository userRepository, ExpenseRepository expenseRepository) {
        this.userRepository = userRepository;
        this.expenseRepository = expenseRepository;
    }

    private record SeedExpense(int dayOfMonth, double amount, String category, String description) {}

    private static final List<SeedExpense> SEED_EXPENSES = List.of(
            new SeedExpense(1, 45.50, "Food", "Groceries"),
            new SeedExpense(3, 12.00, "Transport", "Bus pass top-up"),
            new SeedExpense(5, 89.99, "Bills", "Electricity bill"),
            new SeedExpense(8, 25.00, "Health", "Pharmacy"),
            new SeedExpense(11, 15.75, "Entertainment", "Movie ticket"),
            new SeedExpense(14, 60.00, "Shopping", "New shoes"),
            new SeedExpense(18, 9.50, "Other", "Miscellaneous"),
            new SeedExpense(21, 32.20, "Food", "Restaurant dinner")
    );

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        long userId = userRepository.insert(
                "Demo User", "demo@spendly.com", passwordEncoder.encode("demo123"));

        LocalDate today = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        int lastDay = today.lengthOfMonth();

        for (SeedExpense se : SEED_EXPENSES) {
            int day = Math.min(se.dayOfMonth(), lastDay);
            String date = today.withDayOfMonth(day).format(fmt);
            expenseRepository.insert(userId, se.amount(), se.category(), date, se.description());
        }
    }
}
