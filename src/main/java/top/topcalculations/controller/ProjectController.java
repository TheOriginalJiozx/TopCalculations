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
@RequestMapping("/")  // Root URL for controlleren
public class ProjectController {
    private final ProjectService projectService;  // Service til at håndtere projekter
    private final UserService userService;  // Service til at håndtere brugere

    // Konstruktor til at injicere ProjectService og UserService afhængigheder
    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    // Hjælpefunktion til at få den autentificerede brugers brugernavn
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Returner brugernavnet, hvis brugeren er autentificeret, ellers returner "Guest"
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    // Vis index-siden med den autentificerede brugers brugernavn
    @GetMapping("/")
    public String showIndexPage(Model model) {
        model.addAttribute("username", getAuthenticatedUsername());  // Tilføj brugernavn til model
        return "index";  // Returner index-siden
    }

    // Hjælpefunktion til at tilføje den autentificerede brugers brugernavn til model
    private void addAuthenticatedUsernameToModel(Model model) {
        String authenticatedUsername = getAuthenticatedUsername();
        model.addAttribute("username", authenticatedUsername);  // Tilføj brugernavn til model
    }

    // Vis formularen til at tilføje et nyt projekt
    @GetMapping("/add")
    public String showProjectForm(Model model) {
        addAuthenticatedUsernameToModel(model);  // Tilføj autentificeret brugernavn
        List<Project> projects = projectService.getAllProjectsWithoutTasks();  // Hent alle projekter uden opgaver
        model.addAttribute("projects", projects);  // Tilføj projekterne til model
        model.addAttribute("project", new Project());  // Tilføj et tomt Project-objekt til model
        return "add";  // Returner formularen til at tilføje et nyt projekt
    }

    // Håndter formularindsendelsen for at tilføje et nyt projekt eller opgave
    @PostMapping("/add")
    public String submitForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId();  // Hent den nuværende brugers ID

        if (userId == null) {  // Hvis brugeren ikke er autentificeret
            model.addAttribute("message", "Error: User not authenticated.");  // Vis fejlnmessage
            addAuthenticatedUsernameToModel(model);
            return "add";  // Returner til formularen
        }

        if (project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()) {  // Hvis et opgavenavn er angivet
            Project mainProject = projectService.getProjectByName(project.getTaskProjectName());  // Hent hovedprojektet

            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs();  // Hent WBS for hovedprojektet

                List<Project> tasks = projectService.getTasks(mainProject.getProjectTaskName());  // Hent opgaver for hovedprojektet

                int highestTaskIndex = 0;
                // Find det højeste indeks blandt eksisterende opgaver
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

                // Opret et nyt WBS for opgaven
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                // Tjek om WBS allerede findes og opdater om nødvendigt
                while (projectService.wbsExists(newWBS)) {
                    highestTaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);
                }

                project.setWbs(newWBS);  // Sæt WBS for den nye opgave
                project.setTaskProjectName(project.getTaskProjectName());  // Sæt opgavenavn
                project.setProjectTaskName(mainProject.getProjectTaskName());  // Sæt hovedprojektets opgavenavn
                project.setMainProjectName(mainProject.getProjectTaskName());  // Sæt hovedprojektets navn
                projectService.saveTask(project);  // Gem opgaven
                redirectAttributes.addFlashAttribute("message", "Task saved successfully.");  // Success message
            } else {
                model.addAttribute("message", "Error: Main project not found.");  // Fejlmeddelelse hvis hovedprojektet ikke findes
            }
        } else {
            // Hvis det er et nyt projekt (ikke en opgave)
            project.setProjectTaskName(project.getTaskProjectName());
            project.setTaskProjectName(null);
            project.setWbs(project.getWbs());
            projectService.saveProject(project);  // Gem projektet
            redirectAttributes.addFlashAttribute("message", "Project saved successfully.");  // Success message
        }

        return "redirect:/add";  // Omdiriger tilbage til formularen
    }

    // Vis alle projekter
    @GetMapping("/view-projects")
    public String viewProject(Model model) {
        List<Project> projects = projectService.getAllProjects();  // Hent alle projekter
        model.addAttribute("projects", projects);  // Tilføj projekter til model
        addAuthenticatedUsernameToModel(model);  // Tilføj autentificeret brugernavn
        return "view-projects";  // Returner visning for at vise projekterne
    }

    // Vis en specifik opgave baseret på ID
    @GetMapping("/view-task/{id}")
    public String viewTask(@PathVariable("id") Long id, Model model) {
        addAuthenticatedUsernameToModel(model);  // Tilføj autentificeret brugernavn
        List<Project> projects = projectService.getTaskByID(id);  // Hent opgave baseret på ID
        model.addAttribute("tasks", projects);  // Tilføj opgaver til model
        model.addAttribute("task", new Project());  // Tilføj et tomt Project-objekt til model
        return "view-task";  // Returner visning for at vise opgavedetaljer
    }

    // Vis en specifik opgave baseret på ID
    @GetMapping("/view-subtask/{id}")
    public String viewSubTask(@PathVariable("id") Long id, Model model) {
        addAuthenticatedUsernameToModel(model);  // Tilføj autentificeret brugernavn
        List<Project> projects = projectService.getSubTaskByID(id);  // Hent underopgave baseret på ID
        model.addAttribute("subtasks", projects);  // Tilføj underopgaver til model
        model.addAttribute("subtask", new Project());  // Tilføj et tomt Project-objekt til model
        return "view-subtask";  // Returner visning for at vise underopgavedetaljer
    }

    @GetMapping("/edit-task/{id}")
    public String editTask(@PathVariable("id") Long id, Model model) {
        addAuthenticatedUsernameToModel(model);  // Tilføjer det autentificerede brugernavn til modellen
        List<Project> task = projectService.getTaskByID(id); // Henter opgaven med det angivne ID
        model.addAttribute("task", task); // Tilføjer den hentede opgave til modellen, så den kan vises i viewet
        return "edit-task"; // Returnerer viewet til at redigere opgaven (f.eks. 'edit-task.html')
    }

    @PostMapping("/update-task/{id}")
    public String updateTask(@PathVariable("id") int id, @ModelAttribute Project task, Model model) {
        System.out.println("Opdaterer opgave med ID: " + id);  // Printer ID'et på opgaven, der opdateres (til debugging)
        System.out.println("Nyt opgavenavn: " + task.getTaskProjectName());  // Printer det nye opgavenavn (til debugging)
        System.out.println("Ny varighed: " + task.getDuration());  // Printer den nye varighed (til debugging)

        task.setId(id);  // Sætter ID'et for opgaven for at sikre, at den rigtige opgave opdateres
        projectService.updateTask(id, task);  // Kalder tjenesten for at opdatere opgaven med de nye data

        return "redirect:/view-task/" + id;  // Omdirigerer til den side, hvor den opdaterede opgave vises
    }

    @GetMapping("/edit-subtask/{id}")
    public String editSubTask(@PathVariable("id") Long id, Model model) {
        addAuthenticatedUsernameToModel(model);  // Tilføjer det autentificerede brugernavn til modellen
        List<Project> subTask = projectService.getSubTaskByID(id); // Henter opgaven med det angivne ID
        model.addAttribute("subTask", subTask); // Tilføjer den hentede opgave til modellen, så den kan vises i viewet
        return "edit-subtask"; // Returnerer viewet til at redigere opgaven (f.eks. 'edit-task.html')
    }

    @PostMapping("/update-subtask/{id}")
    public String updateSubTask(@PathVariable("id") int id, @ModelAttribute Project subtask, Model model) {
        System.out.println("Opdaterer underopgave med ID: " + id);  // Printer ID'et på underopgaven, der opdateres (til debugging)
        System.out.println("Nyt underopgavenavn: " + subtask.getSubTaskName());  // Printer det nye underopgavenavn (til debugging)
        System.out.println("Ny varighed: " + subtask.getDuration());  // Printer den nye varighed (til debugging)

        subtask.setId(id);  // Sætter ID'et for opgaven for at sikre, at den rigtige underopgave opdateres
        projectService.updateSubTask(id, subtask);  // Kalder tjenesten for at opdatere underopgaven med de nye data

        return "redirect:/view-subtask/" + id;  // Omdirigerer til den side, hvor den opdaterede underopgave vises
    }
}
