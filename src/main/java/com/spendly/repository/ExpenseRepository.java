package com.spendly.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;

@Repository
public class ExpenseRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExpenseRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
