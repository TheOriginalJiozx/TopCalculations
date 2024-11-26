package top.topcalculations.controller;

import kalkunlationsvaerktoej.kalkulationsvaerktoej.model.Task;
import kalkunlationsvaerktoej.kalkulationsvaerktoej.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ProjectController {

    @Autowired
    private TaskService taskService;

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    @GetMapping("/task")
    public String showForm(Model model) {
        model.addAttribute("task", new Task());
        return "task";
    }

    @PostMapping("/task")
    public String submitForm(@ModelAttribute Task task, Model model) {
        taskService.saveTask(task);
        model.addAttribute("message", "Task saved successfully!");
        return "task";
    }
}