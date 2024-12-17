package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;
import top.topcalculations.service.UserService;

import java.util.List;

@Controller
public class SubTaskController {

    // Service-lag til håndtering af underopgaver, opgaver og brugere
    private final SubTaskService subTaskService;
    private final TaskService taskService;
    private final UserService userService;

    /**
     * Konstruktor til at injicere de nødvendige services.
     */
    public SubTaskController(SubTaskService subTaskService, TaskService taskService, UserService userService) {
        this.subTaskService = subTaskService;
        this.taskService = taskService;
        this.userService = userService;
    }

    /**
     * Viser formularen til at tilføje en ny underopgave.
     */
    @GetMapping("/addSub")
    public String showAddSubForm(Model model, HttpSession session) {
        // Tjekker om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Hvis ikke, redirect til login-siden
        }

        // Tilføjer brugerinformation (navn og rolle) til modellen
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        // Henter liste over opgaver og brugere til formularens dropdown-menuer
        List<Task> tasks = taskService.getAllTasks();
        model.addAttribute("tasks", tasks);

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        // Tilføjer et tomt Subtask-objekt til formularen
        model.addAttribute("subtask", new Subtask());

        return "addSub"; // Returnerer viewet "addSub"
    }

    /**
     * Håndterer formularindsendelse for at tilføje en ny underopgave.
     */
    @PostMapping("/addSub")
    public String submitAddSubForm(@ModelAttribute Subtask subTask, @ModelAttribute Task task,
                                   Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjekker om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Validerer, at der er valgt en hovedopgave til underopgaven
        if (subTask.getTaskName() != null && !subTask.getTaskName().isEmpty()) {
            Task mainTask = taskService.getTaskByName(task.getTaskName());

            if (mainTask != null) {
                // Genererer en ny WBS (Work Breakdown Structure) for underopgaven
                String mainTaskWBS = mainTask.getWbs();
                int highestSubtaskIndex = subTaskService.getHighestWbsIndexForSubtasks(mainTaskWBS);
                String newWBS = mainTaskWBS + "." + (highestSubtaskIndex + 1);

                // Sætter værdier for den nye underopgave
                subTask.setWbs(newWBS);
                subTask.setTaskName(mainTask.getTaskName());
                subTask.setProjectName(mainTask.getProjectName());
                subTaskService.saveSubTask(subTask, task); // Gemmer underopgaven i databasen

                redirectAttributes.addFlashAttribute("messageSub", "Subtask added successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessageSub", "Error: Main task not found.");
                return "redirect:/addSub"; // Fejl, hvis hovedopgaven ikke findes
            }
        }
        return "redirect:/addSub"; // Omdirigerer til formularen efter succes
    }

    /**
     * Sletter en underopgave baseret på ID.
     */
    @PostMapping("/delete-subtask/{id}")
    public String deleteSubTask(@PathVariable("id") int id, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Tjek for login
        }

        System.out.println("Sletter subtask med ID: " + id);
        subTaskService.deleteSubTask(id); // Kalder service for at slette underopgaven

        return "redirect:/view"; // Omdirigerer til hovedvisningen
    }

    /**
     * Viser en specifik underopgave baseret på ID.
     */
    @GetMapping("/view-subtask/{id}")
    public String viewSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Redirect ved manglende login
        }

        // Tilføjer brugerinfo til modellen
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        // Henter underopgave med det angivne ID og tilføjer det til modellen
        List<Subtask> subtasks = subTaskService.getSubTaskByID(id);
        model.addAttribute("subtasks", subtasks);

        return "view-subtask"; // Returnerer view-subtask.html
    }

    /**
     * Viser formularen til redigering af en underopgave.
     */
    @GetMapping("/edit-subtask/{id}")
    public String editSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Redirect ved manglende login
        }

        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user.getUsername());
        model.addAttribute("isAdmin", "Admin".equals(user.getRole()));

        // Henter underopgave til redigering
        List<Subtask> subtasks = subTaskService.getSubTaskByID(id);
        model.addAttribute("subtask", subtasks);

        return "edit-subtask"; // Returnerer edit-subtask.html
    }

    /**
     * Opdaterer en underopgave baseret på indsendt formular.
     */
    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Subtask subtask,
                                @ModelAttribute Project project, @ModelAttribute Task task, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Redirect ved manglende login
        }

        System.out.println("Opdaterer underopgave med ID: " + id);
        subtask.setId(id);
        subTaskService.updateSubTask(id, subtask, project, task);

        return "redirect:/view-subtask/" + id; // Omdirigerer til visning af opdateret underopgave
    }

    /**
     * Opdaterer status for en underopgave.
     */
    @PostMapping("/update-subtask-status/{id}/{status}")
    public String updateSubTaskStatus(@PathVariable("id") Long id, @PathVariable("status") String status,
                                      HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Tjek for login
        }

        subTaskService.updateSubTaskStatus(id, status); // Opdaterer status via service
        return "redirect:/view-subtask/" + id; // Omdirigerer tilbage til underopgaven
    }
}
