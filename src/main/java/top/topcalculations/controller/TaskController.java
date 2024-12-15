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

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;

    public TaskController(TaskService taskService, ProjectService projectService, UserService userService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.userService = userService;
    }

    @GetMapping("/addTask")
    public String showAddTaskForm(Model model, HttpSession session) {
        // Check if the user is logged in, otherwise redirect to login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect to login page
        }

        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());  // Add username to the model

            // Check if the user is an admin and add isAdmin to the model
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);  // Add isAdmin as true for admins
            } else {
                model.addAttribute("isAdmin", false);  // Add isAdmin as false for non-admins
            }
        } else {
            model.addAttribute("username", "Guest");  // Set username to "Guest" if not logged in
            model.addAttribute("isAdmin", false);  // Set isAdmin to false for guest users
        }

        List<Project> projects = projectService.getAllProjects();  // Get all projects
        model.addAttribute("projects", projects);  // Add the list of projects to the model

        // Fetch the list of users (usernames)
        List<User> users = userService.getAllUsers();  // Assume userService is injected
        model.addAttribute("users", users);  // Add the list of users to the model

        model.addAttribute("task", new Task());  // Add a new empty Task object for the form binding

        return "addTask";  // Return the "addTask" view to show the form
    }

    @PostMapping("/addTask")
    public String submitAddTaskForm(@ModelAttribute Task task, @ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login siden
        }

        // Hent brugeren fra sessionen og tilføj brugernavnet til modellen
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user != null ? user.getUsername() : "Guest");
        model.addAttribute("isAdmin", user != null && "Admin".equals(user.getRole()));

        // Hvis hovedprojektet ikke er valgt, behandl som et nyt projekt
        if (task.getMainProjectName() != null && !task.getMainProjectName().isEmpty()) {
            // Hent hovedprojektet ved navn
            Project mainProject = projectService.getProjectByName(task.getMainProjectName());  // Få hovedprojektet efter navn

            // Tjek om hovedprojektet findes
            if (mainProject != null) {
                // Hent WBS (Work Breakdown Structure) for hovedprojektet
                String mainProjectWBS = mainProject.getWbs();  // Få WBS for hovedprojektet

                // Hent den højeste WBS indeks fra projekter og opgave tabeller
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

    @PostMapping("/delete-task/{id}")
    public String deleteTask(@PathVariable("id") int id, HttpSession session, Project name) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Sletter task med ID: " + id);

        taskService.deleteTask(id);

        return "redirect:/view";
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

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }

        List<Task> tasks = taskService.getTaskByID(id);  // Hent opgave efter ID
        model.addAttribute("tasks", tasks);  // Tilføj opgaver til model
        return "view-task";  // Returner view til visning af opgavedetaljer
    }

    @GetMapping("/edit-task/{id}")
    public String editTask(@PathVariable("id") Long id, Model model, HttpSession session) {
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

        List<Task> task = taskService.getTaskByID(id); // Henter opgaven med det angivne ID
        model.addAttribute("task", task); // Tilføjer den hentede opgave til modellen
        return "edit-task"; // Returnerer viewet til at redigere opgaven
    }

    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") int id, @ModelAttribute Task task, @ModelAttribute Project project, HttpSession session, String oldTaskName) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        task.setId(id);  // Sætter ID for opgaven
        taskService.updateTask(id, task, oldTaskName, project);  // Opdater opgaven

        return "redirect:/view-task/" + id;  // Redirect til visning af opgaven
    }

    // Opdaterer en tasks status
    @PostMapping("/update-task-status/{id}/{status}")
    public String updateTaskStatus(@PathVariable("id") Long id,
                                   @PathVariable("status") String status, HttpSession session, String projectName) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        taskService.updateTaskStatus(id, status, projectName);
        return "redirect:/view-task/" + id;  // Redirect til task view efter opdatering af status
    }
}
