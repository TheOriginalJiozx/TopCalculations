package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import top.topcalculations.model.Project;
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Metode til at finde en bruger baseret på brugernavn
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        // Denne metode udfører en SQL-forespørgsel for at finde en bruger i databasen ved hjælp af det angivne brugernavn.
        try {
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, this::mapRowToUser);
        } catch (EmptyResultDataAccessException e) {
            return null; // Returnerer null, hvis brugeren ikke findes
        }
    }

    public void updateLastLogin(String username) {
        String sql = "UPDATE users SET last_login = CURRENT_DATE WHERE username = ?";
        jdbcTemplate.update(sql, username);  // Use update instead of queryForObject
    }

    // Metode til at oprette en ny bruger
    public String signUp(User user, Model model) {
        try {
            // Tjekker om brugernavnet allerede findes i databasen
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{user.getUsername()}, Integer.class);

            if (count != null && count > 0) {
                // Tilføjer en fejlmeddelelse til modellen, hvis brugernavnet allerede findes
                model.addAttribute("error", "Username already exists!");
                return "signup"; // Returnerer til signup-siden
            }

            user.setEnabled(true);
            user.setRole("User");
            // Hasher adgangskoden (brug en stærkere algoritme som bcrypt i produktion)
            String hashedPassword = hashPassword(user.getPassword());

            // Indsætter den nye bruger i databasen
            String sql = "INSERT INTO users (username, password, enabled, role) VALUES (?, ?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(sql, user.getUsername(), hashedPassword, user.isEnabled(), user.getRole());

            // Logger hvor mange rækker der blev opdateret
            System.out.println("Rows affected: " + rowsAffected);
            model.addAttribute("message", "User registered successfully!");
            return "signup";
        } catch (Exception e) {
            // Tilføjer en fejlmeddelelse til modellen i tilfælde af en fejl
            model.addAttribute("error", "Error: " + e.getMessage());
            e.printStackTrace();
            return "signup";
        }
    }

    public List<User> getAllUsers() {
        String usersSql = "SELECT * FROM users";
        return jdbcTemplate.query(usersSql, new BeanPropertyRowMapper<>(User.class));
    }

    // Metode til at mappe en ResultSet til et User-objekt
    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id")); // Sætter id fra databasen
        user.setUsername(rs.getString("username")); // Sætter brugernavn
        user.setPassword(rs.getString("password")); // Sætter adgangskode
        user.setEnabled(rs.getBoolean("enabled")); // Sætter enabled-status
        user.setRole(rs.getString("role")); // Sætter rolle
        return user;
    }

    // Simpel metode til at hashe adgangskoder (MD5 bruges her, men bcrypt anbefales i produktion)
    public String hashPassword(String password) {
        try {
            // Opretter en MD5-hash af adgangskoden
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b)); // Konverterer hver byte til en hexadecimal streng
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // Smider en undtagelse, hvis algoritmen ikke findes
        }
    }

    public List<Project> getProjectsForUser(String username) {
        String projectSql = "SELECT id, project_name, assigned FROM projects WHERE assigned = ?";
        return jdbcTemplate.query(projectSql, (rs, rowNum) -> {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setProjectName(rs.getString("project_name"));
            project.setAssigned(rs.getString("assigned"));
            return project;
        }, username);
    }

    public List<Task> getTasksForUser(String username) {
        String taskSql = "SELECT id, task_name, assigned FROM tasks WHERE assigned = ?";
        return jdbcTemplate.query(taskSql, (rs, rowNum) -> {
            Task task = new Task();
            task.setId(rs.getInt("id"));
            task.setTaskName(rs.getString("task_name"));
            task.setAssigned(rs.getString("assigned"));
            return task;
        }, username);
    }

    public List<Subtask> getSubTasksForUser(String username) {
        String subtaskSql = "SELECT id, sub_task_name, assigned FROM subtasks WHERE assigned = ?";
        return jdbcTemplate.query(subtaskSql, (rs, rowNum) -> {
            Subtask subtask = new Subtask();
            subtask.setId(rs.getInt("id"));
            subtask.setSubTaskName(rs.getString("sub_task_name"));
            subtask.setAssigned(rs.getString("assigned"));
            return subtask;
        }, username);
    }
}
