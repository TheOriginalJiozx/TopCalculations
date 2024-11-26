package top.topcalculations.repository;

import top.topcalculations.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void saveTask(Task task) {
        String sql = "INSERT INTO tasks (WBS, task_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, task.getWbs(), task.getTaskName(), task.getDuration(), task.getPlannedStartDate(), task.getPlannedFinishDate(), task.getAssigned());
    }
}