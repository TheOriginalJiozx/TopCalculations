package top.topcalculations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import top.topcalculations.model.Project;
import top.topcalculations.repository.ProjectRepository;
import top.topcalculations.repository.TaskRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("h2")
public class ProjectRepositoryIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private TaskRepository taskRepository;

    @Test
    public void testSaveTask() {
        // Insert a project into the projects table first
        String projectSql = "INSERT INTO projects (project_name) VALUES (?)";
        jdbcTemplate.update(projectSql, "Test Project");

        // Ensure the project was inserted by checking its existence
        String projectCheckSql = "SELECT COUNT(*) FROM projects WHERE project_name = ?";
        Integer projectCount = jdbcTemplate.queryForObject(projectCheckSql, Integer.class, "Test Project");
        assertNotNull(projectCount);
        assertEquals(1, projectCount);

        // Create a new task associated with the project
        Project task = new Project();
        task.setWbs("WBS-123");
        task.setProjectTaskName("Test Project"); // Ensure this matches the project inserted
        task.setTaskProjectName("Test Task");
        task.setTimeToSpend(10.0);
        task.setAssigned("John Doe");
        task.setPlannedStartDate(String.valueOf(LocalDate.of(2024, 1, 1)));
        task.setPlannedFinishDate(String.valueOf(LocalDate.of(2024, 1, 10)));
        task.setResource_name("Resource 1");

        // Save the task
        taskRepository.saveTaskH2(task);

        // Verify the task was inserted correctly
        String sql = "SELECT COUNT(*) FROM tasks WHERE WBS = ? AND task_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, task.getWbs(), task.getTaskProjectName());
        assertNotNull(count);
        assertEquals(1, count);

        // Verify the resource is correctly linked
        String resourceSql = "SELECT COUNT(*) FROM resources_tasks WHERE resource_name = ?";
        Integer resourceCount = jdbcTemplate.queryForObject(resourceSql, Integer.class, task.getResource_name());
        assertNotNull(resourceCount);
        assertEquals(1, resourceCount);
    }
}