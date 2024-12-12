package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.topcalculations.model.Project;
import top.topcalculations.repository.TaskRepository;

import java.util.List;

@Service
public class TaskService {
    @Autowired
    private TaskRepository taskRepository; // Injicerer TaskRepository for at kunne interagere med databasen

    // Gemmer en opgave i databasen
    public void saveTask(Project project) {
        taskRepository.saveTask(project); // Kald til repository-metode for at gemme opgaven
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project task, String oldTaskName) {
        taskRepository.updateTask(id, task, oldTaskName); // Kald til repository-metode for at opdatere opgaven
    }

    // Opdaterer en opgaves status i databasen
    public void updateTaskStatus(Long id, String status, String projectName) {
        taskRepository.updateTaskStatusByID(id, status, projectName);
    }

    // Sletter en opgave i databasen
    public void deleteTask(int id) {
        taskRepository.deleteTask(id);
    }

    // Henter alle opgaver fra databasen
    public List<Project> getAllTasks() {
        return taskRepository.findAllTasks(); // Kald til repository-metode for at hente alle opgaver
    }

    // Henter opgaver baseret på ID fra databasen
    public List<Project> getTaskByID(Long id) {
        return taskRepository.findTaskByID(id); // Kald til repository-metode for at hente opgave baseret på ID
    }

    // Henter en opgave baseret på navnet
    public Project getTaskByName(String taskName) {
        return taskRepository.findTaskByName(taskName); // Kald til repository-metode for at hente opgave baseret på navn
    }
}
