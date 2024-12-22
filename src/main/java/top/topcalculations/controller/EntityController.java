package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import top.topcalculations.model.*;
import top.topcalculations.service.ProjectService;
import top.topcalculations.service.SubTaskService;
import top.topcalculations.service.TaskService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class EntityController {
    private final ProjectService projectService;  // Service til at håndtere projekter
    private final TaskService taskService;  // Service til at håndtere opgaver
    private final SubTaskService subTaskService;  // Service til at håndtere underopgaver

    // Konstruktor der initialiserer services
    public EntityController(ProjectService projectService, TaskService taskService, SubTaskService subTaskService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.subTaskService = subTaskService;
    }

    @GetMapping("/view")
    public String view(Model model, HttpSession session) {
        // Tjekker om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Hvis brugeren ikke er logget ind, omdiriger til login-siden
        }

        // Henter brugeroplysninger fra sessionen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAdmin", "Admin".equals(user.getRole())); // Tjekker om brugeren er admin
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        // Henter projekter, opgaver og underopgaver fra service-laget
        List<Project> projects = projectService.getProjects();
        List<Task> tasks = taskService.getTasks();
        List<Subtask> subtasks = subTaskService.getSubTasks();

        // Opretter en liste til at kombinere alle entiteter (projekter, opgaver og underopgaver)
        List<CombinedEntity> combinedEntities = new ArrayList<>();

        // Tilføjer projekterne til den kombinerede liste
        for (Project project : projects) {
            CombinedEntity entity = new CombinedEntity(
                    project.getWbs(),
                    project.getProjectName(),
                    project.getDuration(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getAssigned(),
                    project.getTimeToSpend(),
                    project.getStatus()
            );
            combinedEntities.add(entity);
        }

        // Tilføjer opgaverne til den kombinerede liste
        for (Task task : tasks) {
            CombinedEntity entity = new CombinedEntity(
                    task.getWbs(),
                    task.getTaskName(),
                    task.getDuration(),
                    task.getPlannedStartDate(),
                    task.getPlannedFinishDate(),
                    task.getAssigned(),
                    task.getTimeToSpend(),
                    task.getStatus()
            );
            combinedEntities.add(entity);
        }

        // Tilføjer underopgaverne til den kombinerede liste
        for (Subtask subtask : subtasks) {
            CombinedEntity entity = new CombinedEntity(
                    subtask.getWbs(),
                    subtask.getSubTaskName(),
                    subtask.getDuration(),
                    subtask.getPlannedStartDate(),
                    subtask.getPlannedFinishDate(),
                    subtask.getAssigned(),
                    subtask.getTimeToSpend(),
                    subtask.getStatus()
            );
            combinedEntities.add(entity);
        }

        // Sorterer den kombinerede liste af entiteter efter WBS-nummer
        combinedEntities.sort((entity1, entity2) -> {
            List<Integer> wbs1 = entity1.getWbsParts();
            List<Integer> wbs2 = entity2.getWbsParts();

            // Sammenligner WBS-numrene part for part
            for (int i = 0; i < Math.min(wbs1.size(), wbs2.size()); i++) {
                int compareResult = Integer.compare(wbs1.get(i), wbs2.get(i));
                if (compareResult != 0) {
                    return compareResult;
                }
            }

            // Hvis et WBS-nummer er et prefix af det andet, skal den kortere liste komme først
            return Integer.compare(wbs1.size(), wbs2.size());
        });

        // Tilføjer den sorterede liste af entiteter til modellen
        model.addAttribute("combinedEntities", combinedEntities);

        // Beregner den samlede tid, der skal bruges
        double totalTimeToSpend = projects.stream()
                .filter(project -> !"done".equals(project.getStatus())) // Filtrerer projekter med status "done"
                .mapToDouble(Project::getTimeToSpend)
                .sum()
                + tasks.stream()
                .mapToDouble(Task::getTimeToSpend)
                .sum()
                + subtasks.stream()
                .mapToDouble(Subtask::getTimeToSpend)
                .sum();

        // Tilføjer den samlede tid til modellen
        model.addAttribute("totalTimeToSpend", totalTimeToSpend);

        return "view"; // Returnerer navnet på view-siden (HTML-skabelon)
    }
}