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

    // Services til håndtering af projekter, opgaver og underopgaver
    private final ProjectService projectService;
    private final TaskService taskService;
    private final SubTaskService subTaskService;

    // Konstruktor, som initialiserer services via dependency injection
    public EntityController(ProjectService projectService, TaskService taskService, SubTaskService subTaskService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.subTaskService = subTaskService;
    }

    // Håndterer GET-anmodningen til /view
    @GetMapping("/view")
    public String view(Model model, HttpSession session) {
        // 1. Tjekker om brugeren er logget ind via sessionen
        if (session.getAttribute("user") == null) {
            return "redirect:/login"; // Hvis ikke logget ind, send til login-siden
        }

        // 2. Henter brugerinformation fra sessionen
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername()); // Tilføjer brugernavn til modellen
            model.addAttribute("isAdmin", "Admin".equals(user.getRole())); // Sætter admin-flag
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
        }

        // 3. Henter data fra services: Projekter, Opgaver og Underopgaver
        List<Project> projects = projectService.getProjects(); // Henter liste af projekter
        List<Task> tasks = taskService.getTasks(); // Henter liste af opgaver
        List<Subtask> subtasks = subTaskService.getSubTasks(); // Henter liste af underopgaver

        // 4. Kombinerer alle entiteter i én liste (CombinedEntity)
        List<CombinedEntity> combinedEntities = new ArrayList<>();

        // Tilføjer projekter til den kombinerede liste
        for (Project project : projects) {
            CombinedEntity entity = new CombinedEntity(
                    project.getWbs(),                // Work Breakdown Structure (WBS)
                    project.getProjectName(),        // Navn på projektet
                    project.getDuration(),           // Varighed af projektet
                    project.getPlannedStartDate(),   // Planlagt startdato
                    project.getPlannedFinishDate(),  // Planlagt slutdato
                    project.getAssigned(),           // Tildelt bruger
                    project.getTimeToSpend(),        // Tid der skal bruges
                    project.getStatus()              // Status på projektet
            );
            combinedEntities.add(entity);
        }

        // Tilføjer opgaver til den kombinerede liste
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

        // Tilføjer underopgaver til den kombinerede liste
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

        // 5. Sorterer den kombinerede liste efter WBS (Work Breakdown Structure)
        combinedEntities.sort((entity1, entity2) -> {
            List<Integer> wbs1 = entity1.getWbsParts(); // Henter WBS-dele for første entitet
            List<Integer> wbs2 = entity2.getWbsParts(); // Henter WBS-dele for anden entitet

            // Sammenligner WBS-dele en for en
            for (int i = 0; i < Math.min(wbs1.size(), wbs2.size()); i++) {
                int compareResult = Integer.compare(wbs1.get(i), wbs2.get(i));
                if (compareResult != 0) {
                    return compareResult; // Returnerer forskel hvis WBS-delene er forskellige
                }
            }

            // Hvis en WBS er et prefix af den anden, sættes den kortere først
            return Integer.compare(wbs1.size(), wbs2.size());
        });

        // 6. Tilføjer den sorterede liste til modellen
        model.addAttribute("combinedEntities", combinedEntities);

        // 7. Beregner den samlede tid, der skal bruges for projekter, opgaver og underopgaver
        double totalTimeToSpend =

                // Beregner tiden for projekter
                projects.stream() // Omdanner listen af projekter til en stream for at behandle elementerne
                        .filter(project -> !"done".equals(project.getStatus())) // Filtrerer projekter med status "done" (færdige projekter medregnes ikke)
                        .mapToDouble(Project::getTimeToSpend) // Mapper hvert projekt til dets "timeToSpend"-værdi (tid der skal bruges)
                        .sum() // Summerer værdierne fra alle projekter i streamen

                        +

                        // Beregner tiden for opgaver
                        tasks.stream() // Omdanner listen af opgaver til en stream
                                .mapToDouble(Task::getTimeToSpend) // Mapper hver opgave til dens "timeToSpend"-værdi
                                .sum() // Summerer værdierne fra alle opgaver i streamen

                        +

                        // Beregner tiden for underopgaver
                        subtasks.stream() // Omdanner listen af underopgaver til en stream
                                .mapToDouble(Subtask::getTimeToSpend) // Mapper hver underopgave til dens "timeToSpend"-værdi
                                .sum(); // Summerer værdierne fra alle underopgaver i streamen

        // Tilføjer den samlede tid til modellen
        model.addAttribute("totalTimeToSpend", totalTimeToSpend);

        return "view"; // Returnerer navnet på view-siden (HTML-skabelon)
    }
}
