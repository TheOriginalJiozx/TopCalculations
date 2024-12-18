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
                model.addAttribute("isAdmin", true); // Dette vil være sandt, hvis brugeren er Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
        }
        return "index"; // Thymeleaf vil rendre index.html
    }

    // Vis formularen for at tilføje et projekt
    @GetMapping("/addProject")
    public String showAddProjectForm(Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        User user = (User) session.getAttribute("user");

        if (user != null) {
            model.addAttribute("username", user.getUsername());  // Tilføj brugernavn til modellen

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);  // Tilføj isAdmin til modellen (true hvis brugeren er Admin)
            } else {
                model.addAttribute("isAdmin", false);  // Tilføj isAdmin som false for ikke-admin brugere
            }
        } else {
            model.addAttribute("username", "Guest");  // Hvis brugeren ikke er logget ind, sæt brugernavn til "Guest"
            model.addAttribute("isAdmin", false);  // Sæt isAdmin til false for gæstebrugere
        }

        List<User> users = userService.getAllUsers();  // Antager at userService er injiceret
        model.addAttribute("users", users);  // Tilføj listen af brugere til modellen

        model.addAttribute("project", new Project());  // Tilføj et nyt tomt Project-objekt til formularbinding
        return "addProject";  // Returner "addProject" view for at vise formularen
    }

    // Indsend formularen for at tilføje et projekt
    @PostMapping("/addProject")
    public String submitAddProjectForm(@ModelAttribute Project project, @ModelAttribute Task task, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        // Hent brugeren fra sessionen og tilføj brugernavnet til modellen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Dette vil være sandt, hvis brugeren er Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
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
                model.addAttribute("isAdmin", true); // Dette vil være sandt, hvis brugeren er Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
        }

        List<Project> projects = projectService.getProjectByID(id);  // Hent projekt efter ID
        model.addAttribute("projects", projects);  // Tilføj projekter til model
        return "view-project";  // Returner view til visning af projektets detaljer
    }

    // Rediger et specifikt projekt ved ID
    @GetMapping("/edit-project/{id}")
    public String editProject(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Dette vil være sandt, hvis brugeren er Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
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