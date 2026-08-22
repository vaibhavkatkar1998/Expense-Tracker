package com.spendly.repository;

import com.spendly.model.Expense;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

@Repository
public class ExpenseRepository {

    private static final RowMapper<Expense> EXPENSE_ROW_MAPPER = (rs, rowNum) -> new Expense(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getDouble("amount"),
            rs.getString("category"),
            rs.getString("date"),
            rs.getString("description"),
            rs.getTimestamp("created_at").toLocalDateTime()
    );

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Expense> findByUserId(long userId) {
        String sql = "SELECT id, user_id, amount, category, date, description, created_at "
                + "FROM expenses WHERE user_id = ? ORDER BY date DESC, id DESC";
        return jdbcTemplate.query(sql, EXPENSE_ROW_MAPPER, userId);
    }

    public List<Expense> findByUserId(long userId, int limit, int offset) {
        String sql = "SELECT id, user_id, amount, category, date, description, created_at "
                + "FROM expenses WHERE user_id = ? ORDER BY date DESC, id DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, EXPENSE_ROW_MAPPER, userId, limit, offset);
    }

    public long insert(long userId, double amount, String category, String date, String description) {
        String sql = "INSERT INTO expenses (user_id, amount, category, date, description) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[] {"id"});
            ps.setLong(1, userId);
            ps.setDouble(2, amount);
            ps.setString(3, category);
            ps.setString(4, date);
            if (description != null) {
                ps.setString(5, description);
            } else {
                ps.setNull(5, Types.VARCHAR);
            }
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }
}
