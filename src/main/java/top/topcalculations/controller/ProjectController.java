package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.*;
import top.topcalculations.service.ProjectService;
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;
import top.topcalculations.service.UserService;

import java.util.List;

/**
 * ProjectController styrer HTTP-anmodninger for projekthåndtering, herunder visning, redigering, sletning og tilføjelse.
 */
@Controller
@RequestMapping("/")  // Root URL for controller
public class ProjectController {
    private final ProjectService projectService;  // Service til at håndtere projekter
    private final UserService userService;        // Service til at håndtere brugere
    private final TaskService taskService;        // Service til at håndtere opgaver
    private final SubTaskService subTaskService;  // Service til at håndtere underopgaver

    /**
     * Konstruktor til at injicere nødvendige serviceklasser.
     */
    public ProjectController(ProjectService projectService, UserService userService, TaskService taskService, SubTaskService subTaskService) {
        this.projectService = projectService;
        this.userService = userService;
        this.taskService = taskService;
        this.subTaskService = subTaskService;
    }

    /**
     * Viser hjemmesiden (index).
     */
    @GetMapping("/")
    public String homePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");

        // Tilføj brugernavn og rolle (Admin eller Guest) til modellen
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAdmin", "Admin".equals(user.getRole()));
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        return "index"; // Returnerer index.html via Thymeleaf
    }

    /**
     * Viser formularen til at tilføje et projekt.
     */
    @GetMapping("/addProject")
    public String showAddProjectForm(Model model, HttpSession session) {
        // Hvis brugeren ikke er logget ind, omdiriger til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Henter brugerinformation og rolle
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        // Tilføjer en liste af brugere og et tomt projekt-objekt til formularbinding
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("project", new Project());
        return "addProject"; // Returnerer addProject.html
    }

    /**
     * Håndterer formularindsendelse for at tilføje et nyt projekt eller opgave.
     */
    @PostMapping("/addProject")
    public String submitAddProjectForm(@ModelAttribute Project project, @ModelAttribute Task task,
                                       Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Håndtering af hovedprojektnavn og ny opgave
        if (task.getMainProjectName() == null || task.getMainProjectName().isEmpty()) {
            projectService.saveProject(project, task); // Gem projektet i databasen
            redirectAttributes.addFlashAttribute("message", "Project added successfully."); // Success-meddelelse
        }

        return "redirect:/addProject"; // Omdiriger tilbage til tilføjelsesformularen
    }

    /**
     * Viser et specifikt projekt ved hjælp af ID.
     */
    @GetMapping("/view-project/{id}")
    public String viewProject(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Tjek om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        // Hent projektet fra databasen
        List<Project> projects = projectService.getProjectByID(id);
        model.addAttribute("projects", projects);
        return "view-project"; // Returnerer view-project.html
    }

    /**
     * Viser formularen til at redigere et projekt.
     */
    @GetMapping("/edit-project/{id}")
    public String editProject(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        List<Project> project = projectService.getProjectByID(id);
        model.addAttribute("project", project);
        return "edit-project";
    }

    /**
     * Opdaterer et projekt baseret på formularindsendelse.
     */
    @PostMapping("/update-project/{id}")
    public String updateProject(@PathVariable("id") int id, @ModelAttribute Project project, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Opdaterer projekt i databasen
        project.setId(id);
        projectService.updateProject(id, project);

        return "redirect:/view-project/" + id; // Omdiriger til visningen af projektet
    }

    /**
     * Sletter et projekt baseret på ID.
     */
    @PostMapping("/delete-project/{id}")
    public String deleteProject(@PathVariable("id") int id, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        projectService.deleteProject(id); // Slet projektet i databasen
        return "redirect:/view"; // Omdiriger til visningen af alle projekter
    }

    /**
     * Opdaterer status for et projekt.
     */
    @PostMapping("/update-project-status/{id}/{status}")
    public String updateProjectStatus(@PathVariable("id") Long id,
                                      @PathVariable("status") String status, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Opdaterer status for projektet
        projectService.updateProjectStatus(id, status);
        return "redirect:/view-task/" + id; // Omdiriger til visning af opgave efter opdatering
    }
}
