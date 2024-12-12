package top.topcalculations.component;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;

@Component
public class CleanUpUsers {

    private final JdbcTemplate jdbcTemplate;

    public CleanUpUsers(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(fixedRate = 86400000)
    public void removeInactiveUsers() {
        LocalDate cutoffDate = LocalDate.now().minusYears(5);
        System.out.println("Running cleanup task. Cutoff date: " + cutoffDate);
        String sql = "DELETE FROM users WHERE last_login < ?";
        int deletedCount = jdbcTemplate.update(sql, cutoffDate);
        System.out.println("Deleted " + deletedCount + " users who were inactive for more than 5 years.");
    }
}