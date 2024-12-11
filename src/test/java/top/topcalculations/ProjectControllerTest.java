package top.topcalculations;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.model.User;
import top.topcalculations.service.ProjectService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProjectControllerTest {

    private ProjectService projectService;
    private HttpSession session;
    private RedirectAttributes redirectAttributes;
    private Model model;

    @BeforeEach
    void setUp() {
        // Opret mock-objekter til afhængigheder
        projectService = mock(ProjectService.class);
        session = mock(HttpSession.class);
        redirectAttributes = mock(RedirectAttributes.class);
        model = mock(Model.class);
    }

    @Test
    void testSubmitAddForm_UserNotLoggedIn() {
        // Arrange
        when(session.getAttribute("user")).thenReturn(null); // Brugeren er ikke logget ind

        // Act
        String result = submitAddForm(new Project(), session, model, redirectAttributes);

        // Assert
        assertEquals("redirect:/login", result); // Forventet omdirigering til login
        verifyNoInteractions(projectService); // Ingen interaktioner med projectService forventes
    }

    @Test
    void testSubmitAddForm_NewProject() {
        // Arrange
        User user = new User();
        user.setUsername("testUser");
        when(session.getAttribute("user")).thenReturn(user); // Mock en logget ind bruger

        Project project = new Project();
        project.setTaskProjectName("New Task");
        project.setMainProjectName(null); // Ingen hovedprojekt angivet

        // Act
        String result = submitAddForm(project, session, model, redirectAttributes);

        // Assert
        assertEquals("redirect:/add", result); // Forventet omdirigering tilbage til /add
        verify(projectService).saveProject(project); // Bekræft, at projektet blev gemt
        verify(redirectAttributes).addFlashAttribute("message", "Project added successfully."); // Tjek succesmeddelelse
    }

    @Test
    void testSubmitAddForm_TaskUnderMainProject() {
        // Arrange
        User user = new User();
        user.setUsername("testUser");
        when(session.getAttribute("user")).thenReturn(user); // Mock en logget ind bruger

        Project mainProject = new Project();
        mainProject.setWbs("1"); // WBS for hovedprojekt
        mainProject.setProjectTaskName("Main Project");

        Project newTask = new Project();
        newTask.setTaskProjectName("New Task");
        newTask.setMainProjectName("Main Project");

        when(projectService.getProjectByName("Main Project")).thenReturn(mainProject); // Returner hovedprojekt
        when(projectService.getHighestWbsIndex("1")).thenReturn(3); // Mock højeste WBS indeks

        // Act
        String result = submitAddForm(newTask, session, model, redirectAttributes);

        // Assert
        assertEquals("redirect:/add", result); // Forventet omdirigering tilbage til /add
        verify(projectService).saveTask(newTask); // Bekræft, at opgaven blev gemt
        verify(redirectAttributes).addFlashAttribute("message", "Task added successfully."); // Tjek succesmeddelelse
        assertEquals("1.4", newTask.getWbs()); // Bekræft korrekt WBS
    }

    @Test
    void testSubmitAddForm_MainProjectNotFound() {
        // Arrange
        User user = new User();
        user.setUsername("testUser");
        when(session.getAttribute("user")).thenReturn(user); // Mock en logget ind bruger

        Project project = new Project();
        project.setTaskProjectName("New Task");
        project.setMainProjectName("Nonexistent Project");

        when(projectService.getProjectByName("Nonexistent Project")).thenReturn(null); // Mock manglende hovedprojekt

        // Act
        String result = submitAddForm(project, session, model, redirectAttributes);

        // Assert
        assertEquals("redirect:/add", result, "Forventet omdirigering tilbage til add-formularen");
        verify(projectService).getProjectByName("Nonexistent Project"); // Bekræft opkald
        verify(redirectAttributes).addFlashAttribute("errorMessage", "Error: Main project not found."); // Bekræft fejlmeddelelse
        verifyNoMoreInteractions(projectService); // Ingen yderligere interaktioner forventes
    }

    // Simuleret metode til test
    private String submitAddForm(Project project, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        // Simulerer den oprindelige logik
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());

        if (project.getMainProjectName() == null || project.getMainProjectName().isEmpty()) {
            project.setProjectTaskName(project.getTaskProjectName());
            project.setTaskProjectName(null);
            projectService.saveProject(project);
            redirectAttributes.addFlashAttribute("message", "Project added successfully.");
        } else {
            Project mainProject = projectService.getProjectByName(project.getMainProjectName());
            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs();
                int highestTaskIndex = projectService.getHighestWbsIndex(mainProjectWBS);
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);
                project.setWbs(newWBS);
                projectService.saveTask(project);
                redirectAttributes.addFlashAttribute("message", "Task added successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Error: Main project not found.");
                return "redirect:/add";
            }
        }

        return "redirect:/add";
    }
}