// Importér nødvendige klasser og pakker
package top.topcalculations.controller;

// Importer biblioteker til session, modelhåndtering, og Spring MVC
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Importer dine egne model- og serviceklasser
import top.topcalculations.model.Project;
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;
import top.topcalculations.service.UserService;

import java.util.List;

/**
 * Controller til håndtering af operationer relateret til subtasks
 */
@Controller
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final TaskService taskService;
    private final UserService userService;

    // Constructor for afhængighedsinjektion af services
    public SubTaskController(SubTaskService subTaskService, TaskService taskService, UserService userService) {
        this.subTaskService = subTaskService;
        this.taskService = taskService;
        this.userService = userService;
    }

    /**
     * Vis formularen til at tilføje en ny underopgave
     */
    @GetMapping("/addSub")
    public String showAddSubForm(Model model, HttpSession session) {
        // Kontrollér, om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login, hvis sessionen er tom
        }

        // Hent brugeroplysninger og fastsæt roller
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAdmin", "Admin".equals(user.getRole()));
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        // Hent nødvendige data til dropdowns i formularen
        model.addAttribute("tasks", taskService.getAllTasks());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("subtask", new Subtask());

        return "addSub";  // Returner view til at tilføje underopgaver
    }

    /**
     * Behandl indsendelsen af formularen for at tilføje en ny underopgave
     */
    @PostMapping("/addSub")
    public String submitAddSubForm(@ModelAttribute Subtask subTask, @ModelAttribute Task task,
                                   Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Kontrollér loginstatus
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }

        // Sæt brugeroplysninger i model
        User user = (User) session.getAttribute("user");
        model.addAttribute("username", user != null ? user.getUsername() : "Guest");
        model.addAttribute("isAdmin", user != null && "Admin".equals(user.getRole()));

        // Kontrollér om hovedopgave eksisterer
        if (subTask.getTaskName() != null && !subTask.getTaskName().isEmpty()) {
            Task mainTask = taskService.getTaskByName(task.getTaskName());
            if (mainTask != null) {
                // Generér WBS for underopgaven
                int highestSubtaskIndex = subTaskService.getHighestWbsIndexForSubtasks(mainTask.getWbs());
                String newWBS = mainTask.getWbs() + "." + (highestSubtaskIndex + 1);
                subTask.setWbs(newWBS);
                subTaskService.saveSubTask(subTask, task);
                redirectAttributes.addFlashAttribute("messageSub", "Subtask added successfully.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessageSub", "Error: Main task not found.");
                return "redirect:/addSub";
            }
        }

        return "redirect:/addSub";
    }

    /**
     * Slet en specifik underopgave
     */
    @PostMapping("/delete-subtask/{id}")
    public String deleteSubTask(@PathVariable("id") int id, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";
        }
        subTaskService.deleteSubTask(id);
        return "redirect:/view";
    }
    // Vis en specifik underopgave ved ID
    @GetMapping("/view-subtask/{id}")
    public String viewSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Bruger skal være logget ind for at se underopgaven
        }

        // Hent den nuværende bruger fra sessionen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername()); // Tilføj brugernavn til modellen

            // Tilføj admin-flag til modellen baseret på brugerens rolle
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Brugeren er admin
            } else {
                model.addAttribute("isAdmin", false); // Brugeren er ikke admin
            }
        } else {
            // Hvis ingen bruger er logget ind, behandl dem som gæst
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        // Hent underopgave ved hjælp af ID og tilføj til modellen
        List<Subtask> subtasks = subTaskService.getSubTaskByID(id);
        model.addAttribute("subtasks", subtasks); // Tilføj underopgaven til modellen

        return "view-subtask"; // Returner view for at vise underopgavens detaljer
    }

    // Rediger en specifik underopgave ved ID
    @GetMapping("/edit-subtask/{id}")
    public String editSubTask(@PathVariable("id") Long id, Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Kun loggede brugere kan redigere
        }

        // Hent den nuværende bruger fra sessionen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername()); // Tilføj brugernavn til modellen

            // Tilføj admin-flag til modellen baseret på brugerens rolle
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // Brugeren er admin
            } else {
                model.addAttribute("isAdmin", false); // Brugeren er ikke admin
            }
        } else {
            // Hvis ingen bruger er logget ind, behandl dem som gæst
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        // Hent underopgave ved hjælp af ID og tilføj til modellen for redigering
        List<Subtask> subtasks = subTaskService.getSubTaskByID(id);
        model.addAttribute("subtask", subtasks); // Tilføj underopgaven til modellen

        return "edit-subtask"; // Returner view for redigering af underopgaven
    }

    // Opdater en specifik underopgave
    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Subtask subtask,
                                @ModelAttribute Project project, @ModelAttribute Task task, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Kun loggede brugere kan opdatere underopgaver
        }

        // Debugging-information for at verificere opdateringen
        System.out.println("Opdaterer underopgave med ID: " + id);
        System.out.println("Nyt underopgavenavn: " + subtask.getSubTaskName());
        System.out.println("Ny varighed: " + subtask.getDuration());

        // Sæt ID på underopgaven og kald service for at opdatere den
        subtask.setId(id);
        subTaskService.updateSubTask(id, subtask, project, task);

        // Efter opdatering, redirect til siden med opgavens detaljer
        return "redirect:/view-subtask/" + id;
    }

    // Opdater en underopgaves status
    @PostMapping("/update-subtask-status/{id}/{status}")
    public String updateSubTaskStatus(@PathVariable("id") Long id, @PathVariable("status") String status, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers omdiriger til login-siden
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Kun loggede brugere kan ændre status
        }

        // Kald service for at opdatere underopgavens status
        subTaskService.updateSubTaskStatus(id, status);

        // Efter opdatering, redirect til siden med opgavens detaljer
        return "redirect:/view-subtask/" + id;
    }

}
