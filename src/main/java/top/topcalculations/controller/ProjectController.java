package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.model.User;
import top.topcalculations.service.ProjectService;
import top.topcalculations.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/")  // Root URL for controller
public class ProjectController {
    private final ProjectService projectService;  // Service til at håndtere projekter
    private final UserService userService;  // Service til at håndtere brugere

    // Konstruktor til at injicere ProjectService og UserService afhængigheder
    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    // Vis index-siden
    @GetMapping("/")
    public String homePage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        return "index"; // Thymeleaf vil rendre index.html
    }

    // Vis formularen til at tilføje et projekt eller en opgave
    @GetMapping("/add")
    public String showAddForm(Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers send til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        List<Project> projects = projectService.getAllProjectsWithoutTasks();  // Hent alle hovedprojekter
        model.addAttribute("projects", projects);  // Tilføj projekter til model
        model.addAttribute("project", new Project());  // Tilføj et nyt tomt projekt til model
        return "add";  // Returner formularen til tilføjelse af projekt
    }

    @PostMapping("/add")
    public String submitAddForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Check if the user is logged in, otherwise redirect to login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect to login page
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());

        if (project.getMainProjectName() == null || project.getMainProjectName().isEmpty()) {
            // Handle case where no main project is selected, save as a new project
            project.setProjectTaskName(project.getTaskProjectName());  // Set projectTaskName as the entered taskProjectName
            project.setTaskProjectName(null);  // Clear taskProjectName
            projectService.saveProject(project);  // Save the project
            redirectAttributes.addFlashAttribute("message", "Project added successfully.");  // Success message
        } else {
            Project mainProject = projectService.getProjectByName(project.getMainProjectName());  // Get main project by name

            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs();  // Get WBS of the main project

                // Get the highest WBS index from projects and tasks tables
                int highestTaskIndex = projectService.getHighestWbsIndex(mainProjectWBS);

                // Generate a new WBS for the task
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                project.setWbs(newWBS);  // Set WBS for the new task
                project.setTaskProjectName(project.getTaskProjectName());  // Set taskProjectName
                project.setProjectTaskName(mainProject.getProjectTaskName());  // Set projectTaskName
                project.setResource_name(project.getResource_name());
                project.setId(project.getId());
                projectService.saveTask(project);  // Save the task
                redirectAttributes.addFlashAttribute("message", "Task added successfully.");  // Success message
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Error: Main project not found.");  // Error message
                return "redirect:/add";  // Redirect back to the form
            }
        }

        return "redirect:/add";  // Redirect back to the form for adding a project/task
    }

    // Vis formularen til at tilføje en underopgave
    @GetMapping("/addSub")
    public String showSubForm(Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers send til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        List<Project> projects = projectService.getAllTasks();  // Hent alle opgaver
        model.addAttribute("projects", projects);  // Tilføj opgaver til model
        model.addAttribute("project", new Project());  // Tilføj et nyt tomt projekt til model
        return "addSub";  // Returner formularen til tilføjelse af underopgave
    }

    @PostMapping("/addSub")
    public String submitSubForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers send til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());

        if (project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()) {  // Hvis taskProjectName er angivet
            Project mainTask = projectService.getTaskByName(project.getTaskProjectName());  // Hent hovedopgaven

            if (mainTask != null) {
                String mainTaskWBS = mainTask.getWbs();  // Hent WBS for hovedopgaven

                // Hent den højeste WBS-index for underopgaver
                int highestSubtaskIndex = projectService.getHighestWbsIndexForSubtasks(mainTaskWBS);

                // Generer en ny WBS for underopgaven
                String newWBS = mainTaskWBS + "." + (highestSubtaskIndex + 1);

                project.setWbs(newWBS);  // Sæt WBS for den nye underopgave
                project.setTaskProjectName(mainTask.getTaskProjectName());  // Sæt taskProjectName
                project.setProjectTaskName(mainTask.getProjectTaskName());  // Sæt projectTaskName
                project.setResource_name(project.getResource_name());
                project.setId(project.getId());

                projectService.saveSubTask(project);  // Gem underopgaven
                redirectAttributes.addFlashAttribute("messageSub", "Subtask added successfully.");  // Success-besked
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Error: Main task not found.");  // Fejlbesked
            }
        }

        return "redirect:/addSub";  // Redirect tilbage til formularen for underopgave
    }

    // Vis alle projekter
    @GetMapping("/view")
    public String viewProject(Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        List<Object> projects = projectService.getAll();  // Hent alle projekter
        model.addAttribute("projects", projects);  // Tilføj projekter til model
        return "view";  // Returner view til visning af projekter
    }

    // Vis en specifik opgave ved ID
    @GetMapping("/view-task/{id}")
    public String viewTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        List<Project> projects = projectService.getTaskByID(id);  // Hent opgave efter ID
        model.addAttribute("tasks", projects);  // Tilføj opgaver til model
        model.addAttribute("task", new Project());  // Tilføj et nyt tomt projekt til model
        return "view-task";  // Returner view til visning af opgavedetaljer
    }

    // Vis en specifik underopgave ved ID
    @GetMapping("/view-subtask/{id}")
    public String viewSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        List<Project> projects = projectService.getSubTaskByID(id);  // Hent underopgave efter ID
        model.addAttribute("subtasks", projects);  // Tilføj underopgaver til model
        model.addAttribute("subtask", new Project());  // Tilføj et nyt tomt projekt til model
        return "view-subtask";  // Returner view til visning af underopgavedetaljer
    }

    @GetMapping("/edit-task/{id}")
    public String editTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        List<Project> task = projectService.getTaskByID(id); // Henter opgaven med det angivne ID
        model.addAttribute("task", task); // Tilføjer den hentede opgave til modellen
        return "edit-task"; // Returnerer viewet til at redigere opgaven
    }

    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") int id, @ModelAttribute Project task, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Opdaterer opgave med ID: " + id);
        System.out.println("Nyt opgavenavn: " + task.getTaskProjectName());
        System.out.println("Ny varighed: " + task.getDuration());

        task.setId(id);  // Sætter ID for opgaven
        projectService.updateTask(id, task);  // Opdater opgaven

        return "redirect:/view-task/" + id;  // Redirect til visning af opgaven
    }

    @GetMapping("/edit-subtask/{id}")
    public String editSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
        } else {
            model.addAttribute("username", "Guest");
        }
        List<Project> subTask = projectService.getSubTaskByID(id); // Henter underopgave med ID
        model.addAttribute("subTask", subTask); // Tilføjer underopgave til model
        return "edit-subtask"; // Returnerer view til redigering af underopgave
    }

    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Project subtask, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Opdaterer underopgave med ID: " + id);
        System.out.println("Nyt underopgavenavn: " + subtask.getSubTaskName());
        System.out.println("Ny varighed: " + subtask.getDuration());

        subtask.setId(id);  // Sætter ID for underopgaven
        projectService.updateSubTask(id, subtask);  // Opdater underopgave

        return "redirect:/view-subtask/" + id;  // Redirect til visning af underopgaven
    }
}