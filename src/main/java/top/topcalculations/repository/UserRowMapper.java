package top.topcalculations.repository;

import org.springframework.jdbc.core.RowMapper;
import top.topcalculations.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper<User> {
    // Mapper en række fra ResultSet til et User-objekt
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();  // Opretter en ny User instans

        // Sætter værdier fra ResultSet til User objektet
        user.setId(rs.getLong("id"));  // Sætter brugerens ID
        user.setUsername(rs.getString("username"));  // Sætter brugerens brugernavn
        user.setPassword(rs.getString("password"));  // Sætter brugerens adgangskode
        user.setEnabled(rs.getBoolean("enabled"));  // Sætter om brugeren er aktiveret eller ej (true/false)

        // Hvis der er et admin-felt, kan du tilføje den her:
        // user.setIsAdmin(rs.getBoolean("admin"));

        return user;  // Returnerer det mapperede User objekt
    }
}
