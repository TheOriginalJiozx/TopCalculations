package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import top.topcalculations.model.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Method to find user by username
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, this::mapRowToUser);
        } catch (EmptyResultDataAccessException e) {
            return null; // Return null if no user is found
        }
    }

    public String signUp(User user, Model model) {
        try {
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{user.getUsername()}, Integer.class);

            if (count != null && count > 0) {
                model.addAttribute("error", "Username already exists!");
                return "signup";
            }

            user.setEnabled(true);
            // Using a simple hashing mechanism (you can replace this with any preferred method)
            String hashedPassword = hashPassword(user.getPassword());

            String sql = "INSERT INTO users (username, password, enabled, role) VALUES (?, ?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(sql, user.getUsername(), hashedPassword, user.isEnabled(), user.getRole());

            System.out.println("Rows affected: " + rowsAffected);
            model.addAttribute("message", "User registered successfully!");
            return "redirect:/signup";
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
            e.printStackTrace();
            return "signup";
        }
    }

    // Method to map ResultSet to User object
    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEnabled(rs.getBoolean("enabled"));
        user.setRole(rs.getString("role"));
        return user;
    }

    // Simple password hashing method (MD5 for example, but you should replace with a better algorithm like bcrypt in production)
    public String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}