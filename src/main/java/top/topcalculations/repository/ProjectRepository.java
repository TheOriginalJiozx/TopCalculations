package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class ProjectRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveProject(Project project) {
        if (project.getTaskName() == null || project.getTaskName().isEmpty()) {
            // It's a new project (not a task), generate a new WBS
            String newWBS = generateNewWBS();
            project.setWbs(newWBS);
        }

        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getProjectName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    public void saveTask(Project project) {
        // Save task (task doesn't require WBS generation)
        String sql = "INSERT INTO projects (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getMainProjectName(), project.getTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    // Method to generate a new WBS for a project (for new projects)
    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        // Increment the WBS value
        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
        return String.valueOf(newWBSValue);  // WBS is stored as VARCHAR, so return as a String
    }

    public List<Project> findTasks(String mainProjectName) {
        String sql = "SELECT * FROM projects WHERE task_name = ?";
        return jdbcTemplate.query(sql, new Object[]{mainProjectName}, new ProjectRowMapper());
    }

    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);
    }

    public List<Project> findAll() {
        String sql = "SELECT * FROM projects";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    public List<Project> findAllWithoutTasks() {
        String sql = "SELECT * FROM projects WHERE task_name IS NULL";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    public boolean wbsExists(String wbs) {
        String sql = "SELECT COUNT(*) FROM projects WHERE WBS = ?";
        int count = jdbcTemplate.queryForObject(sql, new Object[]{wbs}, Integer.class);
        return count > 0;
    }

    private static class ProjectRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setWbs(rs.getString("WBS"));
            project.setProjectName(rs.getString("project_name"));
            project.setTaskName(rs.getString("task_name"));
            project.setDuration(rs.getString("duration"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setAssigned(rs.getString("assigned"));
            return project;
        }
    }
}