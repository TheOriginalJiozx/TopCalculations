package top.topcalculations.repository;
import top.topcalculations.model.Calculations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CalculationsRepository {
    private final JdbcTemplate jdbcTemplate;

    public CalculationsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Calculations> findByUserId(Long userId) {
        String sql = "SELECT * FROM calculations WHERE user_id = ?";
        return jdbcTemplate.query(
                sql,
                new Object[]{userId},
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

    private static class CalculationsRowMapper implements RowMapper<Calculations> {
        @Override
        public Calculations mapRow(ResultSet rs, int rowNum) throws SQLException {
            Calculations calculations = new Calculations();
            calculations.setId(rs.getLong("id"));
            calculations.setCalculationData(rs.getString("calculationData"));
            calculations.setUserId(rs.getLong("user_id"));
            return calculations;
        }
    }

    public List<Calculations> findAll() {
        String sql = "SELECT * FROM calculations";
        return jdbcTemplate.query(sql, new CalculationsRowMapper());
    }

    public void save(Calculations calculations) {
        String sql = "INSERT INTO calculations (user_id, title, calculation_data) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, calculations.getUserId(), calculations.getTitle(), calculations.getCalculationData());
    }

    public void update(Calculations calculations) {
        String sql = "UPDATE calculations SET title = ?, calculation_data = ? WHERE id = ?";
        jdbcTemplate.update(sql, calculations.getTitle(), calculations.getCalculationData(), calculations.getId());
    }

    public void delete(Long id) {
        String sql = "DELETE FROM calculations WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
}
