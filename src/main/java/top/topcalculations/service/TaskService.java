package top.topcalculations.service;

import top.topcalculations.model.Task;
import top.topcalculations.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public void saveTask(Task task) {
        taskRepository.saveTask(task);
    }
}