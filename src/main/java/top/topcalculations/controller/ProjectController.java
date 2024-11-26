package top.topcalculations.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.Project;
import top.topcalculations.service.CalculationsService;
import top.topcalculations.service.ProjectService;

import java.util.List;

@Controller
public class ProjectController {

    @Autowired
    private ProjectService projectService;

    @Autowired
    private CalculationsService calculationsService;

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    private void addAuthenticatedUsernameToModel(Model model) {
        String authenticatedUsername = getAuthenticatedUsername();
        model.addAttribute("username", authenticatedUsername);
    }

    @GetMapping("/addProject")
    public String showProjectForm(Model model) {
        addAuthenticatedUsernameToModel(model);
        List<Project> projects = projectService.getAllProjectsWithoutTasks();
        model.addAttribute("projects", projects);
        model.addAttribute("project", new Project());
        return "addProject";
    }

    @PostMapping("/addProject")
    public String submitForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = calculationsService.getCurrentUserId();

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated.");
            addAuthenticatedUsernameToModel(model);
            return "addProject";
        }

        if (project.getMainProjectName() != null && !project.getMainProjectName().isEmpty()) {
            Project mainProject = projectService.getProjectByName(project.getMainProjectName());

            if (mainProject != null) {
                String mainProjectWBS = mainProject.getWbs();

                List<Project> tasks = projectService.getTasks(mainProject.getProjectName());

                int highestTaskIndex = 0;
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

                String newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);

                while (projectService.wbsExists(newWBS)) {
                    highestTaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestTaskIndex + 1);
                }

                project.setWbs(newWBS);
                project.setTaskName(project.getTaskName());
                project.setProjectName(mainProject.getProjectName());
                project.setMainProjectName(mainProject.getProjectName());
                projectService.saveTask(project);
                redirectAttributes.addFlashAttribute("message", "Task saved successfully.");
            } else {
                model.addAttribute("message", "Error: Main project not found.");
            }
        } else {
            project.setProjectName(project.getTaskName());
            project.setTaskName(null);
            project.setWbs(project.getWbs());
            projectService.saveProject(project);
            redirectAttributes.addFlashAttribute("message", "Project saved successfully.");
        }

        return "redirect:/addProject";
    }

    @GetMapping("/view-projects")
    public String viewProject(Model model) {
        List<Project> projects = projectService.getAllProjectsWithoutTasks();
        model.addAttribute("projects", projects);
        addAuthenticatedUsernameToModel(model);
        return "view-projects";
    }
}