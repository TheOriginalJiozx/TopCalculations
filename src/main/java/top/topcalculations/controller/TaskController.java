package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import top.topcalculations.model.Project;
import top.topcalculations.service.ProjectService;
import top.topcalculations.model.User;
import top.topcalculations.service.TaskService;

import java.util.List;

@Controller
public class TaskController {

    private final TaskService taskService;  // Service til at håndtere projekter

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
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

        List<Project> projects = taskService.getTaskByID(id);  // Hent opgave efter ID
        model.addAttribute("tasks", projects);  // Tilføj opgaver til model
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

        List<Project> task = taskService.getTaskByID(id); // Henter opgaven med det angivne ID
        model.addAttribute("task", task); // Tilføjer den hentede opgave til modellen
        return "edit-task"; // Returnerer viewet til at redigere opgaven
    }

    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") int id, @ModelAttribute Project task, HttpSession session, String oldTaskName) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        task.setId(id);  // Sætter ID for opgaven
        taskService.updateTask(id, task, oldTaskName);  // Opdater opgaven

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
