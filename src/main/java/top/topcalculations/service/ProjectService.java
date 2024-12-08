package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.topcalculations.model.Project;
import top.topcalculations.repository.ProjectRepository;

import java.util.ArrayList;
import java.util.List;

@Service // Markerer klassen som en Spring Service, som kan bruges til at håndtere forretningslogik
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository; // Injicerer ProjectRepository for at kunne interagere med databasen

    // Gemmer et projekt i databasen
    public void saveProject(Project project) {
        projectRepository.saveProject(project); // Kald til repository-metode for at gemme projektet
    }

    // Gemmer en opgave i databasen
    public void saveTask(Project project) {
        projectRepository.saveTask(project); // Kald til repository-metode for at gemme opgaven
    }

    // Gemmer en underopgave i databasen
    public void saveSubTask(Project project) {
        projectRepository.saveSubTask(project); // Kald til repository-metode for at gemme underopgaven
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project task) {
        projectRepository.updateTask(id, task); // Kald til repository-metode for at opdatere opgaven
    }

    // Opdaterer en underopgave i databasen
    public void updateSubTask(int id, Project subTask) {
        projectRepository.updateSubTask(id, subTask); // Kald til repository-metode for at opdatere underopgaven
    }

    public List<Object> getAll() {
        // Hent alle projekter fra repository
        List<Project> projects = projectRepository.getAllProjects();
        // Hent alle opgaver (tasks) fra repository
        List<Project> tasks = projectRepository.getAllTasks();
        // Hent alle delopgaver (subtasks) fra repository
        List<Project> subtasks = projectRepository.getAllSubTasks();
        // Opret en liste til at indeholde alle data (projekter, opgaver og delopgaver)
        List<Object> allData = new ArrayList<>();
        // Tilføj alle projekter til listen
        allData.addAll(projects);
        // Tilføj alle opgaver til listen
        allData.addAll(tasks);
        // Tilføj alle delopgaver til listen
        allData.addAll(subtasks);
        // Returner den kombinerede liste med alle data
        return allData;
    }

    // Henter alle hovedprojekter (uden opgaver) fra databasen
    public List<Project> getAllProjectsWithoutTasks() {
        return projectRepository.findAllProjects(); // Kald til repository-metode for at hente projekter uden opgaver
    }

    // Henter alle opgaver fra databasen
    public List<Project> getAllTasks() {
        return projectRepository.findAllTasks(); // Kald til repository-metode for at hente alle opgaver
    }

    // Henter opgaver baseret på ID fra databasen
    public List<Project> getTaskByID(Long id) {
        return projectRepository.findTaskByID(id); // Kald til repository-metode for at hente opgave baseret på ID
    }

    // Henter underopgaver baseret på ID fra databasen
    public List<Project> getSubTaskByID(Long id) {
        return projectRepository.findSubTaskByID(id); // Kald til repository-metode for at hente underopgave baseret på ID
    }

    // Henter et projekt baseret på navnet
    public Project getProjectByName(String projectName) {
        return projectRepository.findProjectByName(projectName); // Kald til repository-metode for at hente projekt baseret på navn
    }

    // Henter en opgave baseret på navnet
    public Project getTaskByName(String taskName) {
        return projectRepository.findTaskByName(taskName); // Kald til repository-metode for at hente opgave baseret på navn
    }

    public int getHighestWbsIndex(String mainProjectWBS) {
        // Hent det højeste WBS-indeks fra projekter-tabellen og opgaver-tabellen
        int highestIndexFromProjects = projectRepository.getHighestWbsIndexFromProjects(mainProjectWBS);
        int highestIndexFromTasks = projectRepository.getHighestWbsIndexFromTasks(mainProjectWBS);
        // Returner det højeste af de to indekser
        return Math.max(highestIndexFromProjects, highestIndexFromTasks);
    }

    public int getHighestWbsIndexForSubtasks(String mainTaskWBS) {
        // Get the highest WBS index from tasks and subtasks tables
        int highestIndexFromTasks = projectRepository.getHighestWbsIndexFromTasks(mainTaskWBS);
        int highestIndexFromSubTasks = projectRepository.getHighestWbsIndexFromSubTasks(mainTaskWBS);
        // Return the highest of the two indexes
        return Math.max(highestIndexFromTasks, highestIndexFromSubTasks);
    }
}