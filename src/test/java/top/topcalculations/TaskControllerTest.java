package top.topcalculations;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import top.topcalculations.controller.TaskController;
import top.topcalculations.service.TaskService;
import top.topcalculations.model.Task;
import top.topcalculations.model.Project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TaskControllerTest {

    @Mock
    private TaskService taskService; // Mock for TaskService

    @Mock
    private HttpSession session; // Mock for HttpSession

    @InjectMocks
    private TaskController taskController; // TaskController der skal testes

    @BeforeEach
    void setUp() {
        // Initialiserer mock-objekter før hver test
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testUpdateTask_UserNotLoggedIn() {
        // Simuler at brugeren ikke er logget ind
        when(session.getAttribute("user")).thenReturn(null);

        // Forbered testdata
        Task task = new Task();
        task.setId(1);
        Project project = new Project();
        project.setProjectName("Project A");

        // Act: Simuler controller-metodens eksekvering
        String result = taskController.updateTask(1, task, project, session, "Old Task");

        // Assert: Bekræft at resultatet er en redirect til login-siden
        assertEquals("redirect:/login", result);

        // Verificer at taskService.updateTask ikke blev kaldt
        verify(taskService, times(0)).updateTask(anyInt(), any(Task.class), anyString(), any(Project.class));
    }

    @Test
    void testUpdateTask_UserLoggedIn() {
        // Simuler at brugeren er logget ind
        when(session.getAttribute("user")).thenReturn(new Object());

        // Forbered testdata
        Task task = new Task();
        task.setId(1);
        task.setTaskName("Updated Task");
        Project project = new Project();
        project.setProjectName("Project A");

        // Act: Simuler controller-metodens eksekvering
        String result = taskController.updateTask(1, task, project, session, "Old Task");

        // Assert: Bekræft at taskService.updateTask() blev kaldt
        verify(taskService, times(1)).updateTask(eq(1), eq(task), eq("Old Task"), eq(project));

        // Assert: Bekræft at resultatet er en redirect til visning af opgaven
        assertEquals("redirect:/view-task/1", result);
    }
}