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
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;

import java.util.List;

@Controller
public class SubTaskController {

    private final SubTaskService subTaskService;
    private final TaskService taskService;

    public SubTaskController(SubTaskService subTaskService, TaskService taskService) {
        this.subTaskService = subTaskService;
        this.taskService = taskService;
    }

    // Vis formularen til at tilføje en underopgave
    @GetMapping("/addSub")
    public String showAddSubForm(Model model, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers send til login
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

        List<Task> tasks = taskService.getAllTasks();  // Hent alle opgaver
        model.addAttribute("tasks", tasks);  // Tilføj opgaver til model
        model.addAttribute("subtask", new Subtask());  // Tilføj et nyt tomt projekt til model
        return "addSub";  // Returner formularen til tilføjelse af underopgave
    }

    @PostMapping("/addSub")
    public String submitAddSubForm(@ModelAttribute Subtask subTask, @ModelAttribute Task task, Model model, RedirectAttributes redirectAttributes, HttpSession session) {
        // Tjek om brugeren er logget ind, ellers send til login
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

        if (subTask.getTaskName() != null && !subTask.getTaskName().isEmpty()) {  // Hvis taskProjectName er angivet
            Task mainTask = taskService.getTaskByName(task.getTaskName());  // Hent hovedopgaven

            if (mainTask != null) {
                String mainTaskWBS = mainTask.getWbs();  // Hent WBS for hovedopgaven

                // Hent den højeste WBS-index for underopgaver
                int highestSubtaskIndex = subTaskService.getHighestWbsIndexForSubtasks(mainTaskWBS);

                // Generer en ny WBS for underopgaven
                String newWBS = mainTaskWBS + "." + (highestSubtaskIndex + 1);

                subTask.setWbs(newWBS);  // Sæt WBS for den nye underopgave
                subTask.setTaskName(mainTask.getTaskName());  // Sæt taskProjectName
                subTask.setProjectName(mainTask.getProjectName());  // Sæt projectTaskName
                subTask.setResource_name(subTask.getResource_name());
                subTask.setId(subTask.getId());

                subTaskService.saveSubTask(subTask, task);  // Gem underopgaven
                redirectAttributes.addFlashAttribute("messageSub", "Subtask added successfully.");  // Success-besked
            } else {
                redirectAttributes.addFlashAttribute("errorMessageSub", "Error: Main task not found.");  // Fejlbesked
                return "redirect:/addSub";  // Omdiriger tilbage til formularen
            }
        }

        return "redirect:/addSub";  // Redirect tilbage til formularen for underopgave
    }

    @PostMapping("/delete-subtask/{id}")
    public String deleteSubTask(@PathVariable("id") int id, HttpSession session, Project name) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Sletter subtask med ID: " + id);

        subTaskService.deleteSubTask(id);

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

        List<Subtask> subtasks = subTaskService.getSubTaskByID(id);  // Hent underopgave efter ID
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

        List<Subtask> subtasks = subTaskService.getSubTaskByID(id); // Henter underopgave med ID
        model.addAttribute("subtask", subtasks); // Tilføjer underopgave til model
        return "edit-subtask"; // Returnerer view til redigering af underopgave
    }

    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Subtask subtask, HttpSession session, String oldSubTaskName) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        System.out.println("Opdaterer underopgave med ID: " + id);
        System.out.println("Nyt underopgavenavn: " + subtask.getSubTaskName());
        System.out.println("Ny varighed: " + subtask.getDuration());

        subtask.setId(id);  // Sætter ID for underopgaven
        subTaskService.updateSubTask(id, subtask, oldSubTaskName);  // Opdater underopgave

        return "redirect:/view-subtask/" + id;  // Redirect til visning af underopgaven
    }

    // Opdaterer en subtasks status
    @PostMapping("/update-subtask-status/{id}/{status}")
    public String updateSubTaskStatus(@PathVariable("id") Long id,
                                      @PathVariable("status") String status, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }
        subTaskService.updateSubTaskStatus(id, status);
        return "redirect:/view-subtask/" + id;  // Redirect til subtask view efter opdatering af status
    }
}
