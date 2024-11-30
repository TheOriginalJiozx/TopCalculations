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
    private final ProjectService projectService;
    private final UserService userService;

    public ProjectController(ProjectService projectService, UserService userService) {
        this.projectService = projectService;
        this.userService = userService;
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    @GetMapping("/")
    public String showIndexPage(Model model) {
        model.addAttribute("username", getAuthenticatedUsername());
        return "index";
    }

    private void addAuthenticatedUsernameToModel(Model model) {
        String authenticatedUsername = getAuthenticatedUsername();
        model.addAttribute("username", authenticatedUsername);
    }

    @GetMapping("/add")
    public String showProjectForm(Model model) {
        addAuthenticatedUsernameToModel(model);
        List<Project> projects = projectService.getAllProjectsWithoutTasks();
        model.addAttribute("projects", projects);
        model.addAttribute("project", new Project());
        return "add";
    }

    @PostMapping("/add")
    public String submitForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId();

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated.");
            addAuthenticatedUsernameToModel(model);
            return "add";
        }

        if (project.getTaskName() != null && !project.getTaskName().isEmpty()) {
            Project mainProject = projectService.getProjectByName(project.getTaskName());

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
                redirectAttributes.addFlashAttribute("message",
                        "Task saved successfully. To add a subtask, <a href='addSub'>click here</a>");
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

        return "redirect:/add";
    }

    @GetMapping("/addSub")
    public String showSubForm(Model model) {
        addAuthenticatedUsernameToModel(model);
        List<Project> projects = projectService.getAllTasks();
        model.addAttribute("projects", projects);
        model.addAttribute("project", new Project());
        return "addSub";
    }

    @PostMapping("/addSub")
    public String submitSubForm(@ModelAttribute Project project, Model model, RedirectAttributes redirectAttributes) {
        Long userId = userService.getCurrentUserId();

        if (userId == null) {
            model.addAttribute("message", "Error: User not authenticated.");
            addAuthenticatedUsernameToModel(model);
            return "add";
        }

        if (project.getTaskName() != null && !project.getTaskName().isEmpty()) {
            Project mainTask = projectService.getTaskByName(project.getTaskName());

            if (mainTask != null) {
                String mainProjectWBS = mainTask.getWbs();

                List<Project> tasks = projectService.getTasks(mainTask.getProjectName());

                int highestSubtaskIndex = 0;
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

                String newWBS = mainProjectWBS + "." + (highestSubtaskIndex + 1);

                while (projectService.wbsExists(newWBS)) {
                    highestSubtaskIndex++;
                    newWBS = mainProjectWBS + "." + (highestSubtaskIndex + 1);
                }

                project.setWbs(newWBS);
                project.setTaskName(mainTask.getTaskName());
                project.setProjectName(mainTask.getProjectName());

                projectService.saveSubTask(project);
                redirectAttributes.addFlashAttribute("message", "Subtask saved successfully.");
            } else {
                model.addAttribute("message", "Error: Main task not found.");
            }
        }

        return "redirect:/addSub";
    }

    @GetMapping("/view-projects")
    public String viewProject(Model model) {
        List<Project> projects = projectService.getAllProjects();
        model.addAttribute("projects", projects);
        addAuthenticatedUsernameToModel(model);
        return "view-projects";
    }
}