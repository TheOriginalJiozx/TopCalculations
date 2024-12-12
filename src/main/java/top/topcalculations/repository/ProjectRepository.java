package top.topcalculations.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@Repository

public class ProjectRepository {
    private JdbcTemplate jdbcTemplate;

    // Konstruktør, der initialiserer JdbcTemplate med en DataSource
    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void saveProject(Project project) {
        // Generer ny WBS, hvis nødvendigt
        if (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty()) {
            String newWBS = generateNewWBS();  // Generer ny WBS
            project.setWbs(newWBS);  // Sæt den nye WBS for projektet
        }

        System.out.println("Saving project: " + project);

        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) " +
                "VALUES (?, ?, DATEDIFF(?, ?), ?, ?, ?, 0)";
        jdbcTemplate.update(sql,
                project.getWbs(),
                project.getProjectTaskName(),
                project.getPlannedFinishDate(),
                project.getPlannedStartDate(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                project.getAssigned());
    }

    void updateTimeToSpendIfNecessary(Project name) {
        String calculateSubtaskTimeQuery =
                "SELECT SUM(st.time_to_spend) " +
                        "FROM subtasks st " +
                        "WHERE st.task_name = ?";

        String getTaskTimeQuery =
                "SELECT time_to_spend " +
                        "FROM tasks " +
                        "WHERE task_name = ?";

        String updateTaskTimeQuery =
                "UPDATE tasks " +
                        "SET time_to_spend = ? " +
                        "WHERE task_name = ?";

        Double totalSubtaskTime = jdbcTemplate.queryForObject(calculateSubtaskTimeQuery, Double.class, name.getTaskProjectName());
        if (totalSubtaskTime == null) {
            totalSubtaskTime = 0.0;
        }

        Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeQuery, Double.class, name.getTaskProjectName());
        if (taskTimeToSpend == null) {
            taskTimeToSpend = 0.0;
        }

        if (totalSubtaskTime > taskTimeToSpend) {
            jdbcTemplate.update(updateTaskTimeQuery, totalSubtaskTime, name.getTaskProjectName());
        }
    }

    // Genererer en ny WBS ved at finde den højeste WBS-værdi og øge den
    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;  // Start med 1, hvis ingen WBS findes
        return String.valueOf(newWBSValue);  // Returner den nye WBS
    }

    // Finder et projekt baseret på dets navn
    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);  // Returner projektet eller null, hvis ikke fundet
    }

    // Henter alle projekter fra databasen
    public List<Project> getAllProjects() {
        String sql = "SELECT *, expected_time_in_total, status FROM projects";
        List<Project> projects = jdbcTemplate.query(sql, new ProjectRowMapper());

        for (Project project : projects) {
            if (project.getProjectTaskName() != null && !project.getProjectTaskName().isEmpty() &&
                    (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty())) {
                project.setProjectTaskName(project.getProjectTaskName());
            } else {
                project.setProjectTaskName(project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()
                        ? project.getTaskProjectName()
                        : project.getProjectTaskName());
            }

            project.setTimeToSpend(project.getExpectedTimeInTotal());
            project.setStatus(project.getStatus());
        }

        return projects;
    }

    // Henter et projekt baseret på dens ID
    public List<Project> findProjectByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.query(sql, new ProjectRowMapper(), id);
    }

    // Henter et projekt baseret på dens ID (specifikt projekt)
    public Project findProjectByIDForStatus(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.queryForObject(sql, new ProjectRowMapper(), id);
    }

    // Opdaterer et projekts status
    public void updateProjectStatusByID(Long id, String status) {
        Project project = findProjectByIDForStatus(id);
        if (project != null) {
            project.setStatus(status);
            String sql = "UPDATE projects SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        } else {
            throw new RuntimeException("Project with ID " + id + " not found");
        }
    }

    // Opdaterer et projekt i databasen
    public void updateProject(int id, Project project) {
        String updateProjectSql = "UPDATE projects SET project_name = ?, duration = DATEDIFF(?, ?), planned_start_date = ?, planned_finish_date = ?, expected_time_in_total = ? WHERE id = ?";
        jdbcTemplate.update(updateProjectSql,
                project.getProjectTaskName(),
                project.getPlannedFinishDate(),
                project.getPlannedStartDate(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                project.getExpectedTimeInTotal(),
                id);
    }

    // Sletter et projekt i databasen
    public void deleteProject(int id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        jdbcTemplate.update(sql, id);

        updateProjectsTable();
    }

    // Opdaterer IDs i projects tabellen
    public void updateProjectsTable() {
        String sql = "SELECT * FROM projects ORDER BY id";
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> project : projects) {
            int originalID = (int) project.get("id");

            String updateSql = "UPDATE projects SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            newId++;
        }
    }

    // Finder projekter
    public List<Project> findAllProjects() {
        String sql = "SELECT * FROM projects WHERE status =! 'done'";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    public int getHighestWbsIndexFromProjects(String mainProjectWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra projekter-tabellen
        String sql = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";
        // Udfør forespørgslen og få resultatet som en Integer
        Integer highestTaskIndex = jdbcTemplate.queryForObject(sql, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
        // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
        return highestTaskIndex != null ? highestTaskIndex : 0;
    }

    // Mapper resultatet af SQL-spørgsmål til et Project-objekt
    private static class ProjectRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setWbs(rs.getString("WBS"));
            project.setProjectTaskName(rs.getString("project_name"));
            project.setTimeSpent(rs.getDouble("time_spent"));
            project.setExpectedTimeInTotal(rs.getDouble("expected_time_in_total"));
            project.setDuration(rs.getInt("duration"));
            project.setStatus(rs.getString("status"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setAssigned(rs.getString("assigned"));
            return project;  // Returner det mapperede Project-objekt
        }
    }
}