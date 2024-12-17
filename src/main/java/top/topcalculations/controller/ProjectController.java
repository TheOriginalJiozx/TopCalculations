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

@Controller
@RequestMapping("/")  // Root URL for controller
public class ProjectController {
    private final ProjectService projectService;  // Service til at håndtere projekter
    private final UserService userService;  // Service til at håndtere brugere
    private final TaskService taskService;
    private final SubTaskService subTaskService;

    // Konstruktor til at injicere ProjectService og UserService afhængigheder
    public ProjectController(ProjectService projectService, UserService userService, TaskService taskService, SubTaskService subTaskService) {
        this.projectService = projectService;
        this.userService = userService;
        this.taskService = taskService;
        this.subTaskService = subTaskService;
    }

    // Vis index-siden
    @GetMapping("/")
    public String homePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }
        return "index"; // Thymeleaf will render index.html
    }

    @GetMapping("/addProject")
    public String showAddProjectForm(Model model, HttpSession session) {
        // Check if the user is logged in, otherwise redirect to login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect to login page
        }

        User user = (User) session.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getUsername());  // Add username to the model

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);  // Add isAdmin to the model (true if the user is an Admin)
            } else {
                model.addAttribute("isAdmin", false);  // Add isAdmin as false for non-admin users
            }
        } else {
            model.addAttribute("username", "Guest");  // If user is not logged in, set username to "Guest"
            model.addAttribute("isAdmin", false);  // Set isAdmin to false for guest users
        }

        List<User> users = userService.getAllUsers();  // Assume userService is injected
        model.addAttribute("users", users);  // Add the list of users to the model

        model.addAttribute("project", new Project());  // Add a new empty Project object for the form binding
        return "addProject";  // Return the "addProject" view to show the form
    }

    @PostMapping("/addProject")
    public String submitAddProjectForm(@ModelAttribute Project project, @ModelAttribute Task task, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login siden
        }

        // Hent brugeren fra sessionen og tilføj brugernavnet til modellen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }

        // Hvis hovedprojektet ikke er valgt, behandl som et nyt projekt
        if (task.getMainProjectName() == null || task.getMainProjectName().isEmpty()) {
            // Indstil projectTaskName som taskProjectName og ryd taskProjectName
            project.setProjectName(project.getProjectName());  // Sæt projectTaskName som den indtastede taskProjectName
            task.setTaskName(null);  // Ryd taskProjectName
            projectService.saveProject(project, task);  // Gem projektet
            redirectAttributes.addFlashAttribute("message", "Project added successfully.");  // Success meddelelse
        }

        // Omdiriger tilbage til formularen for at tilføje et projekt/opgave
        return "redirect:/addProject";  // Omdiriger tilbage til formularen for at tilføje et projekt/opgave
    }

    // Vis et specifikt projekt ved ID
    @GetMapping("/view-project/{id}")
    public String viewProject(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }

        List<Project> projects = projectService.getProjectByID(id);  // Hent projekt efter ID
        model.addAttribute("projects", projects);  // Tilføj projekter til model
        return "view-project";  // Returner view til visning af underopgavedetaljer
    }

    @GetMapping("/edit-project/{id}")
    public String editProject(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }

        List<Project> project = projectService.getProjectByID(id); // Henter projektet med det angivne ID
        model.addAttribute("project", project); // Tilføjer det hentede projekt til modellen
        return "edit-project"; // Returnerer viewet til at redigere projektet
    }

    @PostMapping("/update-project/{id}")
    public String updateProject(@PathVariable("id") int id, @ModelAttribute Project project, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Opdaterer projekt med ID: " + id);
        System.out.println("Nyt projektnavn: " + project.getProjectName());
        System.out.println("Ny varighed: " + project.getDuration());

        project.setId(id);  // Sætter ID for projektet
        projectService.updateProject(id, project);  // Opdater projektet

        return "redirect:/view-project/" + id;  // Redirect til visning af projektet
    }

    @PostMapping("/delete-project/{id}")
    public String deleteProject(@PathVariable("id") int id, @ModelAttribute Project project, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Sletter projekt med ID: " + id);

        projectService.deleteProject(id);

        return "redirect:/view";
    }

    // Opdaterer en tasks status
    @PostMapping("/update-project-status/{id}/{status}")
    public String updateProjectStatus(@PathVariable("id") Long id,
                                      @PathVariable("status") String status, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        projectService.updateProjectStatus(id, status);
        return "redirect:/view-task/" + id;  // Redirect til task view efter opdatering af status
    }
}