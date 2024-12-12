package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.topcalculations.model.Project;
import top.topcalculations.repository.ProjectRepository;
import top.topcalculations.repository.SubTaskRepository;
import top.topcalculations.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service // Markerer klassen som en Spring Service, som kan bruges til at håndtere forretningslogik
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository; // Injicerer ProjectRepository for at kunne interagere med databasen
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private SubTaskRepository subTaskRepository;

    // Gemmer et projekt i databasen
    public void saveProject(Project project) {
        projectRepository.saveProject(project); // Kald til repository-metode for at gemme projektet
    }

    // Opdaterer et projekt i databasen
    public void updateProject(int id, Project project) {
        projectRepository.updateProject(id, project); // Kald til repository-metode for at opdatere underopgaven
    }

    // Opdaterer et projekts status i databasen
    public void updateProjectStatus(Long id, String status) {
        projectRepository.updateProjectStatusByID(id, status);
    }

    // Sletter et projekt i databasen
    public void deleteProject(int id) {
        projectRepository.deleteProject(id);
    }

    public List<Object> getAll() {
        // Hent alle projekter fra repository
        List<Project> projects = projectRepository.getAllProjects();
        // Hent alle opgaver (tasks) fra repository
        List<Project> tasks = taskRepository.getAllTasks();
        // Hent alle delopgaver (subtasks) fra repository
        List<Project> subtasks = subTaskRepository.getAllSubTasks();
        // Opret en liste til at indeholde alle data (projekter, opgaver og delopgaver)
        List<Object> allData = new ArrayList<>();
        // Tilføj alle projekter til listen
        allData.addAll(projects);
        // Tilføj alle opgaver til listen
        allData.addAll(tasks);
        // Tilføj alle delopgaver til listen
        allData.addAll(subtasks);        // Returner den kombinerede liste med alle data
        return allData;
    }

    // Henter alle hovedprojekter (uden opgaver) fra databasen
    public List<Project> getAllProjects() {
        return projectRepository.findAllProjects(); // Kald til repository-metode for at hente projekter uden opgaver
    }

    // Henter opgaver baseret på ID fra databasen
    public List<Project> getProjectByID(Long id) {
        return projectRepository.findProjectByID(id); // Kald til repository-metode for at hente opgave baseret på ID
    }

    // Henter et projekt baseret på navnet
    public Project getProjectByName(String projectName) {
        return projectRepository.findProjectByName(projectName); // Kald til repository-metode for at hente projekt baseret på navn
    }

    public int getHighestWbsIndex(String mainProjectWBS) {
        // Hent det højeste WBS-indeks fra projekter-tabellen og opgaver-tabellen
        int highestIndexFromProjects = projectRepository.getHighestWbsIndexFromProjects(mainProjectWBS);
        int highestIndexFromTasks = taskRepository.getHighestWbsIndexFromTasks(mainProjectWBS);
        // Returner det højeste af de to indekser
        return Math.max(highestIndexFromProjects, highestIndexFromTasks);
    }
}