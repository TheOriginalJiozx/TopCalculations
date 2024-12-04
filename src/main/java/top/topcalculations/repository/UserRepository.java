package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import org.springframework.ui.Model;
import top.topcalculations.model.User;

import javax.sql.DataSource;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbcTemplate;

    // Konstruktør der initialiserer JdbcTemplate med en DataSource
    public UserRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Finder en bruger i databasen baseret på brugernavn
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{username}, new UserRowMapper());  // Returnerer brugeren eller null hvis ikke fundet
    }

    // Tilmeld en ny bruger i systemet
    public String signUp(User user, Model model, PasswordEncoder passwordEncoder) {
        try {
            // Tjek om brugernavnet allerede findes i databasen
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, new Object[]{user.getUsername()}, Integer.class);

            // Hvis brugernavnet allerede findes, vis en fejlbesked
            if (count != null && count > 0) {
                model.addAttribute("message", "Username already exists!");  // Vis en fejlmeddelelse
                return "signup";  // Returner til signup-siden
            }

            // Sæt brugeren som aktiv og hash adgangskoden
            user.setEnabled(true);
            String hashedPassword = passwordEncoder.encode(user.getPassword());  // Krypter adgangskoden

            // SQL-indsættelse af brugerdata i databasen
            String sql = "INSERT INTO users (username, password, enabled) VALUES (?, ?, ?)";
            int rowsAffected = jdbcTemplate.update(sql, user.getUsername(), hashedPassword, user.isEnabled());  // Indsæt bruger i databasen

            System.out.println("Rows affected: " + rowsAffected);  // Print antallet af berørte rækker (brugere oprettet)
            model.addAttribute("message", "User registered successfully!");  // Vis en succesmeddelelse
            return "redirect:/login";  // Omdiriger til login-siden
        } catch (Exception e) {
            // Hvis der opstår en fejl, vis en fejlmeddelelse og print fejlen
            model.addAttribute("message", "Error: " + e.getMessage());
            e.printStackTrace();
            return "signup";  // Returner til signup-siden
        }
    }
}
