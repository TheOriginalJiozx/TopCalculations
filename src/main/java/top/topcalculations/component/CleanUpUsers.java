package top.topcalculations.component;

// Importerer nødvendige Spring og Java-klasser.
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;

// Marker klassen som en Spring-komponent, så den registreres og administreres som en Spring Bean.
@Component
public class CleanUpUsers {

    // JdbcTemplate bruges til at udføre databaseoperationer.
    private final JdbcTemplate jdbcTemplate;

    // Constructor Injection: JdbcTemplate bliver injiceret via constructoren.
    // Dette er en god praksis, fordi det fremmer testbarhed og løs kobling.
    public CleanUpUsers(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Denne metode køres automatisk med et fast interval (hver 24. time).
    // @Scheduled annotationen angiver tidsplanen for denne opgave.
    @Scheduled(fixedRate = 86400000) // 86400000 millisekunder = 24 timer
    public void removeInactiveUsers() {
        // Beregner en dato, der repræsenterer skæringsdatoen (5 år tilbage fra i dag).
        LocalDate cutoffDate = LocalDate.now().minusYears(5);

        // Logger en besked for at vise, at opgaven kører, og hvad skæringsdatoen er.
        System.out.println("Running cleanup task. Cutoff date: " + cutoffDate);

        // SQL-forespørgsel: Sletter brugere, hvis sidste login var før skæringsdatoen.
        String sql = "DELETE FROM users WHERE last_login < ?";

        // Udfører SQL-forespørgslen og returnerer antallet af rækker, der blev slettet.
        int deletedCount = jdbcTemplate.update(sql, cutoffDate);

        // Logger antallet af slettede brugere.
        System.out.println("Deleted " + deletedCount + " users who were inactive for more than 5 years.");
    }
}
