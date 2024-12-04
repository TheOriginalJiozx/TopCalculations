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

    // Display form for adding project or task
    @GetMapping("/add")
    public String showAddForm(Model model) {
        addAuthenticatedUsernameToModel(model); // Adds authenticated username
        List<Project> projects = projectService.getAllProjectsWithoutTasks(); // Gets all main projects
        model.addAttribute("projects", projects); // Adds projects to the model
        model.addAttribute("project", new Project()); // Adds a new Project to the model
        return "add"; // Returns the form for adding project/task
    }

    @PostMapping("/add")
    public String submitAddForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId(); // Gets the ID of the authenticated user

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated."); // Error if user not authenticated
            addAuthenticatedUsernameToModel(model);
            return "add"; // Return to the form
        }

        if (project.getMainProjectName() == null || project.getMainProjectName().isEmpty()) {
            // No main project selected, save as a new project
            project.setProjectTaskName(project.getTaskProjectName()); // Set projectTaskName as the entered taskProjectName
            project.setTaskProjectName(null); // Clear taskProjectName
            projectService.saveProject(project); // Save the project
            redirectAttributes.addFlashAttribute("message", "Project saved successfully."); // Success message
        } else {
            // Main project selected, save as a new task under the main project
            Project mainProject = projectService.getProjectByName(project.getMainProjectName()); // Gets the main project

            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs(); // Gets the WBS of the main project

                List<Project> tasks = projectService.getTasks(mainProject.getProjectTaskName()); // Gets tasks belonging to the main project

                int highestTaskIndex = 0;
                // Finds the highest index for tasks
                for (Project task : tasks) {
                    if (task.getWbs().startsWith(mainProjectWBS + ".")) {
                        String[] wbsParts = task.getWbs().split("\\.");
                        if (wbsParts.length > 1) {
                            try {
                                int taskIndex = Integer.parseInt(wbsParts[1]);
                                highestTaskIndex = Math.max(highestTaskIndex, taskIndex);
                            } catch (NumberFormatException e) {
                                // Handle exception if WBS part is not a number
                            }
                        }
                    }
                }

                // Creates a new WBS for the task
                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                // Checks if WBS already exists and updates if necessary
                while (projectService.wbsExists(newWBS)) {
                    highestTaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);
                }

                project.setWbs(newWBS); // Sets WBS for the new task
                project.setTaskProjectName(project.getTaskProjectName()); // Sets the task name
                project.setProjectTaskName(mainProject.getProjectTaskName()); // Sets the project name

                projectService.saveTask(project); // Saves the task
                redirectAttributes.addFlashAttribute("message", "Task saved successfully."); // Success message
            } else {
                model.addAttribute("message", "Error: Main project not found."); // Error if main project not found
                addAuthenticatedUsernameToModel(model);
                return "add"; // Return to the form
            }
        }

        return "redirect:/add"; // Redirects to the form for adding project/task
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

        if (project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()) { // Hvis opgavenavn er angivet
            Project mainTask = projectService.getTaskByName(project.getTaskProjectName()); // Henter hovedopgaven

            if (mainTask != null) {
                String mainProjectWBS = mainTask.getWbs(); // Henter WBS for hovedopgaven

                List<Project> tasks = projectService.getTasks(mainTask.getProjectTaskName()); // Henter opgaver tilhørende hovedopgaven

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
                project.setTaskProjectName(mainTask.getTaskProjectName()); // Sætter opgavenavnet
                project.setProjectTaskName(mainTask.getProjectTaskName()); // Sætter projektnavnet

                projectService.saveSubTask(project); // Gemmer underopgaven
                redirectAttributes.addFlashAttribute("message", "Subtask saved successfully."); // Success besked
            } else {
                model.addAttribute("message", "Error: Main task not found."); // Fejl, hvis hovedopgave ikke findes
            }
        }

        return "redirect:/addSub"; // Omdirigerer til formularen for underopgave
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
