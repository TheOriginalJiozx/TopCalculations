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
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;

import java.util.List;

@Controller
public class SubTaskController {

    private final SubTaskService subtaskService;  // Service til at håndtere projekter

    public SubTaskController(SubTaskService subtaskService) {
        this.subtaskService = subtaskService;
    }
    @PostMapping("/delete-subtask/{id}")
    public String deleteSubTask(@PathVariable("id") int id, HttpSession session, Project name) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Sletter subtask med ID: " + id);

        subtaskService.deleteSubTask(id);

        return "redirect:/view";
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

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // This will be true if the user is Admin
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // Set isAdmin to false for guest users
        }

        List<Project> subtasks = subtaskService.getSubTaskByID(id);  // Hent underopgave efter ID
        model.addAttribute("subtasks", subtasks);  // Tilføj underopgaver til model
        return "view-subtask";  // Returner view til visning af underopgavedetaljer
    }

    @GetMapping("/edit-subtask/{id}")
    public String editSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
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

        List<Project> subtask = subtaskService.getSubTaskByID(id); // Henter underopgave med ID
        model.addAttribute("subtask", subtask); // Tilføjer underopgave til model
        return "edit-subtask"; // Returnerer view til redigering af underopgave
    }

    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Project subtask, HttpSession session, String oldSubTaskName) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Opdaterer underopgave med ID: " + id);
        System.out.println("Nyt underopgavenavn: " + subtask.getSubTaskName());
        System.out.println("Ny varighed: " + subtask.getDuration());

        subtask.setId(id);  // Sætter ID for underopgaven
        subtaskService.updateSubTask(id, subtask, oldSubTaskName);  // Opdater underopgave

        return "redirect:/view-subtask/" + id;  // Redirect til visning af underopgaven
    }

    // Opdaterer en subtasks status
    @PostMapping("/update-subtask-status/{id}/{status}")
    public String updateSubTaskStatus(@PathVariable("id") Long id,
                                      @PathVariable("status") String status, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }
        subtaskService.updateSubTaskStatus(id, status);
        return "redirect:/view-subtask/" + id;  // Redirect til subtask view efter opdatering af status
    }
}
