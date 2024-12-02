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

    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void saveProject(Project project) {
        if (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty()) {
            String newWBS = generateNewWBS();
            project.setWbs(newWBS);
        }

        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getProjectTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    public void saveTask(Project project) {
        String sql = "INSERT INTO projects (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getTaskProjectName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    public void saveSubTask(Project project) {
        String sql = "INSERT INTO projects (WBS, task_name, sub_task_name, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getTaskProjectName(), project.getSubTaskName(), project.getProjectTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned());
    }

    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
        return String.valueOf(newWBSValue);
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

    public Project findTaskByName(String taskName) {
        String sql = "SELECT * FROM projects WHERE task_name = ?";
        List<Project> tasks = jdbcTemplate.query(sql, new Object[]{taskName}, new ProjectRowMapper());
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    public List<Project> findAll() {
        String sql = "SELECT * FROM projects";
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
        }

        return projects;
    }

    public List<Project> findAllTasks() {
        String sql = "SELECT * FROM projects WHERE task_name IS NOT NULL AND task_name != ''";

        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    public List<Project> findTaskByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.task_name, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.sub_task_name " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.task_name IS NOT NULL AND p.task_name != ''";  // Remove username condition
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    public void updateTask(int id, Project project) {
        // Log to verify the ID and project values
        System.out.println("Updating task with ID: " + id);
        System.out.println("New Task Name: " + project.getTaskProjectName());
        System.out.println("New Duration: " + project.getDuration());

        String sql = "UPDATE projects SET task_name = ?, duration = ?, planned_start_date = ?, planned_finish_date = ?, assigned = ? WHERE id = ?";

        jdbcTemplate.update(sql,
                project.getTaskProjectName(),
                project.getDuration(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                project.getAssigned(),
                id);
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
            project.setProjectTaskName(rs.getString("project_name"));
            project.setTaskProjectName(rs.getString("task_name"));
            project.setSubTaskName(rs.getString("sub_task_name"));
            project.setDuration(rs.getString("duration"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setAssigned(rs.getString("assigned"));
            return project;
        }
    }

    private static class TaskRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project task = new Project();
            task.setId(rs.getInt("id"));
            task.setWbs(rs.getString("WBS"));
            task.setTaskProjectName(rs.getString("task_name"));
            task.setDuration(rs.getString("duration"));
            task.setPlannedStartDate(rs.getString("planned_start_date"));
            task.setPlannedFinishDate(rs.getString("planned_finish_date"));
            task.setAssigned(rs.getString("assigned"));
            return task;
        }
    }
}