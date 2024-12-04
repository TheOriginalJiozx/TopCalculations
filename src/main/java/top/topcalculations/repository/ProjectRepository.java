package top.topcalculations.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

@Repository
public class ProjectRepository {
    private JdbcTemplate jdbcTemplate;

    // Konstruktør, der initialiserer JdbcTemplate med en DataSource
    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Gemmer et projekt i databasen. Hvis projektet ikke har en WBS, genereres en ny.
    public void saveProject(Project project) {
        if (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty()) {
            String newWBS = generateNewWBS();  // Generer ny WBS
            project.setWbs(newWBS);  // Sæt den nye WBS for projektet
        }

        // SQL-indsættelse af projektdata i databasen
        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getProjectTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    // Gemmer en opgave (task) i databasen
    public void saveTask(Project project) {
        String sql = "INSERT INTO projects (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getTaskProjectName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    // Gemmer en delopgave (subtask) i databasen
    public void saveSubTask(Project project) {
        String sql = "INSERT INTO projects (WBS, task_name, sub_task_name, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getTaskProjectName(), project.getSubTaskName(), project.getProjectTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    // Genererer en ny WBS ved at finde den højeste WBS-værdi og øge den
    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;  // Start med 1, hvis ingen WBS findes
        return String.valueOf(newWBSValue);  // Returner den nye WBS
    }

    // Finder alle opgaver tilknyttet et hovedprojekt
    public List<Project> findTasks(String mainProjectName) {
        String sql = "SELECT * FROM projects WHERE task_name = ?";
        return jdbcTemplate.query(sql, new Object[]{mainProjectName}, new ProjectRowMapper());
    }

    // Finder et projekt baseret på dets navn
    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);  // Returner projektet eller null, hvis ikke fundet
    }

    // Finder en opgave baseret på dens navn
    public Project findTaskByName(String taskName) {
        String sql = "SELECT * FROM projects WHERE task_name = ?";
        List<Project> tasks = jdbcTemplate.query(sql, new Object[]{taskName}, new ProjectRowMapper());
        return tasks.isEmpty() ? null : tasks.get(0);  // Returner opgaven eller null, hvis ikke fundet
    }

    // Henter alle projekter fra databasen
    public List<Project> findAll() {
        String sql = "SELECT * FROM projects";
        List<Project> projects = jdbcTemplate.query(sql, new ProjectRowMapper());

        // Opdater projectTaskName, hvis det er nødvendigt
        for (Project project : projects) {
            if (project.getProjectTaskName() != null && !project.getProjectTaskName().isEmpty() &&
                    (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty())) {
                project.setProjectTaskName(project.getProjectTaskName());
            } else {
                project.setProjectTaskName(project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()
                        ? project.getTaskProjectName()
                        : project.getProjectTaskName());
            }
        }

        return projects;  // Returner alle projekter
    }

    // Finder alle opgaver fra databasen
    public List<Project> findAllTasks() {
        String sql = "SELECT * FROM projects WHERE task_name IS NOT NULL AND task_name != ''";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    // Henter en opgave baseret på dens ID
    public List<Project> findTaskByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.task_name, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.sub_task_name " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.task_name IS NOT NULL AND p.task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Henter en opgave baseret på dens ID
    public List<Project> findSubTaskByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.sub_task_name, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.task_name " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.sub_task_name IS NOT NULL AND p.sub_task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project project) {
        String sql = "UPDATE projects SET task_name = ?, duration = ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                project.getTaskProjectName(),
                project.getDuration(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                id);  // Opdater opgave med nye data
    }

    // Opdaterer en underopgave i databasen
    public void updateSubTask(int id, Project project) {
        String sql = "UPDATE projects SET sub_task_name = ?, duration = ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                project.getSubTaskName(),
                project.getDuration(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                id);  // Opdater underopgave med nye data
    }

    // Finder projekter, der ikke har nogen opgaver
    public List<Project> findAllWithoutTasks() {
        String sql = "SELECT * FROM projects WHERE task_name IS NULL";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    // Tjekker om en WBS allerede eksisterer i databasen
    public boolean wbsExists(String wbs) {
        String sql = "SELECT COUNT(*) FROM projects WHERE WBS = ?";
        int count = jdbcTemplate.queryForObject(sql, new Object[]{wbs}, Integer.class);
        return count > 0;  // Returner true, hvis WBS findes, ellers false
    }

    // Mapper resultatet af SQL-spørgsmål til et Project-objekt
    private static class ProjectRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setWbs(rs.getString("WBS"));
            project.setProjectTaskName(rs.getString("project_name"));
            project.setTaskProjectName(rs.getString("task_name"));
            project.setSubTaskName(rs.getString("sub_task_name"));
            project.setDuration(rs.getString("duration"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setAssigned(rs.getString("assigned"));
            return project;  // Returner det mapperede Project-objekt
        }
    }

    // Mapper resultatet af SQL-spørgsmål til et Task-objekt
    private static class TaskRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project task = new Project();
            task.setId(rs.getInt("id"));
            task.setWbs(rs.getString("WBS"));
            task.setTaskProjectName(rs.getString("task_name"));
            task.setSubTaskName(rs.getString("sub_task_name"));
            task.setDuration(rs.getString("duration"));
            task.setPlannedStartDate(rs.getString("planned_start_date"));
            task.setPlannedFinishDate(rs.getString("planned_finish_date"));
            task.setAssigned(rs.getString("assigned"));
            return task;  // Returner det mapperede Task-objekt
        }
    }
}
