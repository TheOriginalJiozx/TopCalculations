package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.topcalculations.model.Project;
import top.topcalculations.repository.ProjectRepository;

import java.util.List;

@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    public void saveProject(Project project) {
        projectRepository.saveProject(project);
    }

    public void saveTask(Project project) {
        projectRepository.saveTask(project);
    }

    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    public List<Project> getAllProjectsWithoutTasks() {
        return projectRepository.findAllWithoutTasks();
    }

    public List<Project> getTasks(String mainProjectName) {
        return projectRepository.findTasks(mainProjectName);
    }

    public Project getProjectByName(String projectName) {
        return projectRepository.findProjectByName(projectName);
    }

    public boolean wbsExists(String wbs) {
        return projectRepository.wbsExists(wbs);
    }
}