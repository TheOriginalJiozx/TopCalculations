package top.topcalculations.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.service.ProjectService;
import top.topcalculations.service.UserService;

import java.util.List;

@Controller
@RequestMapping("/")
public class ProjectController {
    private final ProjectService projectService; // Service til at håndtere projekter
    private final UserService userService; // Service til at håndtere brugere

    // Konstruktor til at injectere dependencies
    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    // Hjælpefunktion til at hente det autentificerede brugernavn
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Hvis brugeren er autentificeret, returneres brugernavnet, ellers returneres "Guest"
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    // Vist på startsiden, med brugernavn
    @GetMapping("/")
    public String showIndexPage(Model model) {
        model.addAttribute("username", getAuthenticatedUsername()); // Tilføjer brugernavn til modellen
        return "index"; // Returnerer index-siden
    }

    // Hjælpefunktion til at tilføje autentificeret brugernavn til modellen
    private void addAuthenticatedUsernameToModel(Model model) {
        String authenticatedUsername = getAuthenticatedUsername();
        model.addAttribute("username", authenticatedUsername); // Tilføjer brugernavn til modellen
    }

    // Vist når man ønsker at tilføje et nyt projekt
    @GetMapping("/add")
    public String showProjectForm(Model model) {
        addAuthenticatedUsernameToModel(model); // Tilføjer autentificeret brugernavn
        List<Project> projects = projectService.getAllProjectsWithoutTasks(); // Henter projekter uden opgaver
        model.addAttribute("projects", projects); // Tilføjer projekterne til modellen
        model.addAttribute("project", new Project()); // Tilføjer en tom Project til modellen
        return "add"; // Returnerer formularen til at tilføje projekt
    }

    // Håndterer formindsendelse af nyt projekt eller opgave
    @PostMapping("/add")
    public String submitForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId(); // Henter id for den aktuelt autentificerede bruger

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated."); // Fejl, hvis bruger ikke er autentificeret
            addAuthenticatedUsernameToModel(model);
            return "add"; // Retur til formularen
        }

        if (project.getTaskName() != null && !project.getTaskName().isEmpty()) { // Hvis opgavenavn er angivet
            Project mainProject = projectService.getProjectByName(project.getTaskName()); // Henter hovedprojektet

            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs(); // Henter WBS (Work Breakdown Structure) for hovedprojekt

                List<Project> tasks = projectService.getTasks(mainProject.getProjectName()); // Henter opgaver relateret til hovedprojekt

                int highestTaskIndex = 0;
                // Finder den højeste indeks for opgaverne
                for (Project task : tasks) {
                    String[] wbsParts = task.getWbs().split("\\.");
                    if (wbsParts.length > 1) {
                        try {
                            int taskIndex = Integer.parseInt(wbsParts[wbsParts.length - 1]);
                            highestTaskIndex = Math.max(highestTaskIndex, taskIndex);
                        } catch (NumberFormatException e) {
                        }
                    }
                }

                // Skaber en ny WBS for opgaven
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                // Tjekker om WBS allerede eksisterer og opdaterer, hvis nødvendigt
                while (projectService.wbsExists(newWBS)) {
                    highestTaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);
                }

                project.setWbs(newWBS); // Sætter WBS for den nye opgave
                project.setTaskName(project.getTaskName()); // Sætter opgavenavnet
                project.setProjectName(mainProject.getProjectName()); // Sætter projektnavnet
                project.setMainProjectName(mainProject.getProjectName()); // Sætter hovedprojektnavnet
                projectService.saveTask(project); // Gemmer opgaven
                redirectAttributes.addFlashAttribute("message",
                        "Task saved successfully. To add a subtask, <a href='addSub'>click here</a>"); // Success besked
            } else {
                model.addAttribute("message", "Error: Main project not found."); // Fejl, hvis hovedprojekt ikke findes
            }
        } else {
            // Hvis det er et nyt projekt og ikke en opgave
            project.setProjectName(project.getTaskName());
            project.setTaskName(null);
            project.setWbs(project.getWbs());
            projectService.saveProject(project); // Gemmer projektet
            redirectAttributes.addFlashAttribute("message", "Project saved successfully."); // Success besked
        }

        return "redirect:/add"; // Omdirigerer til projektformularen
    }

    // Vist når man ønsker at tilføje en subtask (underopgave)
    @GetMapping("/addSub")
    public String showSubForm(Model model) {
        addAuthenticatedUsernameToModel(model); // Tilføjer autentificeret brugernavn
        List<Project> projects = projectService.getAllTasks(); // Henter alle opgaver
        model.addAttribute("projects", projects); // Tilføjer opgaverne til modellen
        model.addAttribute("project", new Project()); // Tilføjer en tom Project til modellen
        return "addSub"; // Returnerer formularen til at tilføje subtask
    }

    // Håndterer formindsendelse af subtask
    @PostMapping("/addSub")
    public String submitSubForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId(); // Henter id for den autentificerede bruger

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated."); // Fejl, hvis bruger ikke er autentificeret
            addAuthenticatedUsernameToModel(model);
            return "add"; // Retur til formularen
        }

        if (project.getTaskName() != null && !project.getTaskName().isEmpty()) { // Hvis opgavenavn er angivet
            Project mainTask = projectService.getTaskByName(project.getTaskName()); // Henter hovedopgaven

            if (mainTask != null) {
                String mainProjectWBS = mainTask.getWbs(); // Henter WBS for hovedopgaven

                List<Project> tasks = projectService.getTasks(mainTask.getProjectName()); // Henter opgaver tilhørende hovedopgaven

                int highestSubtaskIndex = 0;
                // Finder højeste indeks for underopgaver
                for (Project task : tasks) {
                    if (task.getWbs().startsWith(mainProjectWBS + ".")) {
                        String[] wbsParts = task.getWbs().split("\\.");
                        if (wbsParts.length > 2) {
                            try {
                                int subtaskIndex = Integer.parseInt(wbsParts[2]);
                                highestSubtaskIndex = Math.max(highestSubtaskIndex, subtaskIndex);
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }

                // Skaber en ny WBS for underopgaven
                String newWBS = mainProjectWBS + "." + (highestSubtaskIndex + 1);

                // Tjekker om WBS allerede eksisterer og opdaterer, hvis nødvendigt
                while (projectService.wbsExists(newWBS)) {
                    highestSubtaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestSubtaskIndex + 1);
                }

                project.setWbs(newWBS); // Sætter WBS for den nye underopgave
                project.setTaskName(mainTask.getTaskName()); // Sætter opgavenavnet
                project.setProjectName(mainTask.getProjectName()); // Sætter projektnavnet

                projectService.saveSubTask(project); // Gemmer underopgaven
                redirectAttributes.addFlashAttribute("message", "Subtask saved successfully."); // Success besked
            } else {
                model.addAttribute("message", "Error: Main task not found."); // Fejl, hvis hovedopgave ikke findes
            }
        }

        return "redirect:/addSub"; // Omdirigerer til formularen for underopgave
    }

    // Vist for at se alle projekter
    @GetMapping("/view-projects")
    public String viewProject(Model model) {
        List<Project> projects = projectService.getAllProjects(); // Henter alle projekter
        model.addAttribute("projects", projects); // Tilføjer projekterne til modellen
        addAuthenticatedUsernameToModel(model); // Tilføjer autentificeret brugernavn
        return "view-projects"; // Returnerer visning af projekter
    }
}