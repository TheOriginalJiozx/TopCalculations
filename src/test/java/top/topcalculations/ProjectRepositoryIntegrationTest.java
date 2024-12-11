package top.topcalculations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import top.topcalculations.model.Project;
import top.topcalculations.repository.ProjectRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Transactional
public class ProjectRepositoryIntegrationTest {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void testSaveTask() {
        // Arrange: Opret en task med nødvendige data
        Project task = new Project();
        task.setWbs("WBS-123");
        task.setProjectTaskName("Test Project");
        task.setTaskProjectName("Test Task");
        task.setTimeToSpend(10.0);
        task.setAssigned("John Doe");
        task.setPlannedStartDate(String.valueOf(LocalDate.of(2024, 1, 1)));
        task.setPlannedFinishDate(String.valueOf(LocalDate.of(2024, 1, 10)));
        task.setResource_name("Resource 1");

        // Act: Gem tasken i databasen
        projectRepository.saveTask(task);

        // Assert: Kontroller, at dataene blev gemt korrekt
        String sql = "SELECT COUNT(*) FROM tasks WHERE WBS = ? AND task_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, task.getWbs(), task.getTaskProjectName());
        assertNotNull(count);
        assertEquals(1, count);

        String resourceSql = "SELECT COUNT(*) FROM resources_tasks WHERE resource_name = ?";
        Integer resourceCount = jdbcTemplate.queryForObject(resourceSql, Integer.class, task.getResource_name());
        assertNotNull(resourceCount);
        assertEquals(1, resourceCount);
    }
}