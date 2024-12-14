package top.topcalculations;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;
import top.topcalculations.service.ProjectService;
import top.topcalculations.service.TaskService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TaskControllerTest {

    private ProjectService projectService;
    private TaskService taskService;
    private HttpSession session;
    private RedirectAttributes redirectAttributes;
    private Model model;

    @BeforeEach
    void setUp() {
        // Initialize mock objects
        projectService = mock(ProjectService.class);
        taskService = mock(TaskService.class);
        session = mock(HttpSession.class);
        redirectAttributes = mock(RedirectAttributes.class);
        model = mock(Model.class);
    }
}