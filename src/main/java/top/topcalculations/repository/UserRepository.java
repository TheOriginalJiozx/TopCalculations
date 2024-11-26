package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import top.topcalculations.model.Calculations;
import top.topcalculations.model.User;

import javax.sql.DataSource;
import java.util.List;

@Repository
public class UserRepository {
    @Autowired
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, new UserRowMapper());
    }

    public Long getUserIdByUsername(String username) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM users WHERE username = ?",
                new Object[]{username},
                Long.class
        );
    }

    public String signUp(User user, Model model, PasswordEncoder passwordEncoder) {
        try {
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{user.getUsername()}, Integer.class);

            if (count != null && count > 0) {
                model.addAttribute("message", "Username already exists!");
                return "signup";
            }

            user.setEnabled(true);
            String hashedPassword = passwordEncoder.encode(user.getPassword());

            String sql = "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(sql, user.getUsername(), hashedPassword, user.isEnabled());

            System.out.println("Rows affected: " + rowsAffected);
            model.addAttribute("message", "User registered successfully!");
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return "signup";
        }
    }

    public List<Calculations> getSavedCalculations(String username) {
        return jdbcTemplate.query(
                "SELECT * FROM calculations WHERE user_id = (SELECT id FROM users WHERE username = ?)",
                new Object[]{username},
                (rs, rowNum) -> {
                    Calculations calculations = new Calculations();
                    calculations.setId(rs.getLong("id"));
                    calculations.setUserId(rs.getLong("user_id"));
                    calculations.setTitle(rs.getString("title"));
                    calculations.setCalculationData(rs.getString("calculation_data"));
                    return calculations;
                }
        );
    }

    public void saveCalculation(Calculations calculations) {
        String sql = "INSERT INTO calculations (user_id, title, calculation_data) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, calculations.getUserId(), calculations.getTitle(), calculations.getCalculationData());
    }

    public void updateCalculation(Calculations calculations) {
        String sql = "UPDATE calculations SET title = ?, calculation_data = ? WHERE id = ?";
        jdbcTemplate.update(sql, calculations.getTitle(), calculations.getCalculationData(), calculations.getId());
    }

    public void deleteCalculation(Long id) {
        String sql = "DELETE FROM calculations WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}