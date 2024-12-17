package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.model.Task;
import top.topcalculations.service.ProjectService;
import top.topcalculations.model.User;
import top.topcalculations.service.TaskService;
import top.topcalculations.service.UserService;

import java.util.List;

@Controller
public class TaskController {

    // Services der bruges i controlleren
    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    // Konstruktør for at injicere de nødvendige services
    public TaskController(TaskService taskService, ProjectService projectService, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }

    // Metode til at vise formularen for at tilføje en ny opgave
    @GetMapping("/addTask")
    public String showAddTaskForm(Model model, HttpSession session) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());  // Tilføj brugernavn til modellen

            // Tjek om brugeren er admin og tilføj isAdmin-attribut
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);  // Tilføj isAdmin som true for admins
            } else {
                model.addAttribute("isAdmin", false);  // Tilføj isAdmin som false for ikke-admins
            }
        } else {
            model.addAttribute("username", "Guest");  // Sæt brugernavn til "Guest" hvis ikke logget ind
            model.addAttribute("isAdmin", false);  // Sæt isAdmin til false for gæstebrugere
        }

        // Hent alle projekter og tilføj dem til modellen
        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);

        // Hent og tilføj alle brugere til modellen
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        model.addAttribute("task", new Task());  // Tilføj en ny tom Task-objekt til formularbindning

        return "addTask";  // Returner "addTask"-viewet for at vise formularen
    }

    // Metode til at håndtere formularindsendelse for at tilføje en opgave
    @PostMapping("/addTask")
    public String submitAddTaskForm(@ModelAttribute Task task, @ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        // Hent brugeren fra sessionen og tilføj brugernavn og rolle til modellen
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user != null ? user.getUsername() : "Guest");
        model.addAttribute("isAdmin", user != null && "Admin".equals(user.getRole()));

        // Hvis hovedprojektet er specificeret, behandl som et nyt projekt
        if (task.getMainProjectName() != null && !task.getMainProjectName().isEmpty()) {
            // Hent hovedprojektet ved navn
            Project mainProject = projectService.getProjectByName(task.getMainProjectName());  // Hent hovedprojektet efter navn

            // Tjek om hovedprojektet findes
            if (mainProject != null) {
                // Hent WBS (Work Breakdown Structure) for hovedprojektet
                String mainProjectWBS = mainProject.getWbs();  // Hent WBS for hovedprojektet

                // Hent den højeste WBS indeks fra projekter og opgavetable
                int highestTaskIndex = projectService.getHighestWbsIndex(mainProjectWBS);

                // Generer en ny WBS for opgaven
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                // Sæt WBS for den nye opgave
                task.setWbs(newWBS);  // Sæt WBS for den nye opgave
                task.setTaskName(task.getTaskName());  // Sæt taskProjectName
                project.setProjectName(mainProject.getProjectName());  // Sæt projectTaskName fra hovedprojektet
                task.setResource_name(task.getResource_name());  // Sæt resource_name
                task.setId(task.getId());  // Sæt ID
                taskService.saveTask(task, project);  // Gem opgaven
                redirectAttributes.addFlashAttribute("message", "Task added successfully.");  // Success meddelelse
            } else {
                // Hvis hovedprojektet ikke findes, vis en fejlmeddelelse
                redirectAttributes.addFlashAttribute("errorMessage", "Error: Main project not found.");  // Fejlmeddelelse
                return "redirect:/addTask";  // Omdiriger tilbage til formularen
            }
        }

        // Omdiriger tilbage til formularen for at tilføje et projekt/opgave
        return "redirect:/addTask";  // Omdiriger tilbage til formularen for at tilføje et projekt/opgave
    }

    // Metode til at slette en opgave
    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable("id") int id, HttpSession session, Project name) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        System.out.println("Sletter task med ID: " + id);  // Log besked til konsollen

        taskService.deleteTask(id);  // Slet opgaven

        return "redirect:/view";  // Omdiriger til visning
    }

    // Metode til at vise en specifik opgave baseret på ID
    @GetMapping("/view-task/{id}")
    public String viewTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());  // Tilføj brugernavn til modellen

            // Hvis brugeren er admin, sæt isAdmin som true
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Tilføj isAdmin som true for admins
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
        }

        List<Task> tasks = taskService.getTaskByID(id);  // Hent opgaven baseret på ID
        model.addAttribute("tasks", tasks);  // Tilføj opgaver til modellen
        return "view-task";  // Returner view til visning af opgavedetaljer
    }

    // Metode til at redigere en opgave
    @GetMapping("/edit-task/{id}")
    public String editTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            // Hvis brugeren er admin, sæt isAdmin som true
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Tilføj isAdmin som true for admins
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Sæt isAdmin til false for gæstebrugere
        }

        List<Task> task = taskService.getTaskByID(id); // Hent opgaven baseret på ID
        model.addAttribute("task", task); // Tilføj den hentede opgave til modellen
        return "edit-task"; // Returner view til redigering af opgaven
    }

    // Metode til at opdatere en opgave
    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") int id, @ModelAttribute Task task, @ModelAttribute Project project, HttpSession session, String oldTaskName) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        task.setId(id);  // Sæt ID for opgaven
        taskService.updateTask(id, task, oldTaskName, project);  // Opdater opgaven

        return "redirect:/view-task/" + id;  // Omdiriger til visning af opgaven
    }

    // Metode til at opdatere status for en opgave
    @PostMapping("/update-task-status/{id}/{status}")
    public String updateTaskStatus(@PathVariable("id") Long id,
                                   @PathVariable("status") String status, HttpSession session, String projectName) {
        // Tjekker om brugeren er logget ind, hvis ikke omdirigeres de til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden
        }

        taskService.updateTaskStatus(id, status, projectName);  // Opdater opgavestatus
        return "redirect:/view-task/" + id;  // Omdiriger til opgavevisning efter opdatering
    }
}
