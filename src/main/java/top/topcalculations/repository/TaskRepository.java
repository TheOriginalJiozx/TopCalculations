package top.topcalculations.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;
import top.topcalculations.model.Task;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class TaskRepository {
    private JdbcTemplate jdbcTemplate;

    public TaskRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Gemmer en opgave i databasen
    public void saveTask(Task task, Project project) {
        // Udskriver hvilken opgave der gemmes
        System.out.println("Saving task: " + task);

        String fullTaskName = project.getProjectName() + "_" + task.getTaskName();

        // Første SQL-spørgsmål for at indsætte en opgave i tasks-tabellen med DATEDIFF(?, ?)
        String sql = "INSERT INTO tasks (WBS, project_name, task_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                "VALUES (?, ?, ?, ?, ?, DATEDIFF(?, ?), ?, ?)";

        try {
            // Udfører SQL-spørgsmålet og gemmer opgaven i databasen
            jdbcTemplate.update(sql, task.getWbs(), task.getProjectName(), fullTaskName,
                    task.getTimeToSpend(), task.getAssigned(), task.getPlannedFinishDate(), task.getPlannedStartDate(), task.getPlannedStartDate(), task.getPlannedFinishDate());
        } catch (Exception e) {
            // Hvis den første SQL fejler, udfør alternativ forespørgsel med DATEDIFF(DAY, ?, ?)
            System.out.println("First SQL query failed, trying to execute alternate query.");

            // Anden SQL-spørgsmål for at indsætte en opgave med DATEDIFF(DAY, ?, ?)
            String fallbackSql = "INSERT INTO tasks (WBS, project_name, task_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                    "VALUES (?, ?, ?, ?, ?, DATEDIFF(DAY, ?, ?), ?, ?)";
            // Udfør den alternative SQL forespørgsel
            jdbcTemplate.update(fallbackSql, task.getWbs(), project.getProjectName(), fullTaskName,
                    task.getTimeToSpend(), task.getAssigned(), task.getPlannedStartDate(), task.getPlannedFinishDate(), task.getPlannedStartDate(), task.getPlannedFinishDate());
        }

        // SQL-spørgsmål for at finde ID'et for den gemte opgave
        String findTaskIdQuery = "SELECT id FROM tasks WHERE WBS = ? AND task_name = ?";
        // Henter ID'et for opgaven baseret på WBS og task_name
        Long taskId = jdbcTemplate.queryForObject(findTaskIdQuery, Long.class, task.getWbs(), fullTaskName);

        // Hvis taskId ikke er gyldigt, kastes en undtagelse
        if (taskId == null || taskId <= 0) {
            throw new IllegalStateException("Failed to retrieve valid task ID after insert.");
        }

        // Udskriver det fundne task ID
        System.out.println("Task ID: " + taskId);

        // SQL-spørgsmål for at indsætte relationen mellem opgaven og ressourcen i resources_tasks-tabellen
        String sql2 = "INSERT INTO resources_tasks (resource_name, task_id) VALUES (?, ?)";
        // Gemmer ressourcen for opgaven i databasen
        jdbcTemplate.update(sql2, task.getResource_name(), taskId);

        // Opdaterer den forventede tid for projektet baseret på opgaven
        addExpectedTimeToProjectFromTask(task, project);
    }

    private void addExpectedTimeToProjectFromTask(Task task, Project project) {
        String sql = "UPDATE projects SET expected_time_in_total = expected_time_in_total + ? WHERE project_name = ?";
        jdbcTemplate.update(sql, task.getTimeToSpend(), project.getProjectName());
    }

    // Finder en opgave baseret på dens navn
    public Task findTaskByName(String taskName) {
        String sql = "SELECT * FROM tasks WHERE task_name = ?";
        List<Task> tasks = jdbcTemplate.query(sql, new Object[]{taskName}, new TaskRowMapper());
        return tasks.isEmpty() ? null : tasks.get(0);  // Returner opgaven eller null, hvis ikke fundet
    }

    // Henter alle opgaver fra databasen //skal ændres
    public List<Task> getAllTasks() {
        String sql = "SELECT *, time_to_spend, status FROM tasks";  // Adjust column name as needed
        List<Task> tasks = jdbcTemplate.query(sql, new TaskRepository.TaskRowMapper());

        for (Task task : tasks) {
            if (task.getProjectName() != null && !task.getProjectName().isEmpty() &&
                    (task.getTaskName() == null || task.getTaskName().isEmpty())) {
                task.setProjectName(task.getProjectName());
            } else {
                task.setProjectName(task.getTaskName() != null && !task.getTaskName().isEmpty()
                        ? task.getTaskName()
                        : task.getProjectName());
            }

            task.setTimeToSpend(task.getTimeToSpend());
            task.setStatus(task.getStatus());
        }

        return tasks;
    }

    // Finder alle opgaver fra databasen
    public List<Task> findAllTasks() {
        String sql = "SELECT * FROM tasks WHERE task_name IS NOT NULL AND task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper());
    }

    // Henter en opgave baseret på dens ID
    public List<Task> findTaskByID(Long id) {
        String sql = "SELECT t.id, t.wbs, t.project_name, t.time_to_spend, t.task_name, t.assigned, t.time_spent, t.duration, t.planned_start_date, t.planned_finish_date, t.status " +
                "FROM tasks t " +
                "WHERE t.id = ? AND t.task_name IS NOT NULL AND t.task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Henter en opgave baseret på dens ID (specifik opgave)
    public Task findTaskByIDForStatus(Long id) {
        String sql = "SELECT t.id, t.wbs, t.project_name, t.time_to_spend, t.task_name, t.assigned, t.time_spent, t.duration, t.planned_start_date, t.planned_finish_date, t.status " +
                "FROM tasks t " +
                "WHERE t.id = ? AND t.task_name IS NOT NULL AND t.task_name != ''";
        return jdbcTemplate.queryForObject(sql, new TaskRowMapper(), id);
    }

    // Opdaterer en opgaves status
    public void updateTaskStatusByID(Long id, String status, String projectName) {
        Task task = findTaskByIDForStatus(id);
        if (task != null) {
            task.setStatus(status);

            String sql = "UPDATE tasks SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);

            String projectQuery = "SELECT project_name FROM tasks WHERE id = ?";
            String fetchedProjectName = jdbcTemplate.queryForObject(projectQuery, String.class, id);

            String checkTasksQuery = "SELECT COUNT(*) FROM tasks WHERE project_name = ? AND status != 'done'";
            Integer countNotDone = jdbcTemplate.queryForObject(checkTasksQuery, Integer.class, fetchedProjectName);

            if (countNotDone == 0) {
                String updateProjectSql = "UPDATE projects SET status = 'done' WHERE project_name = ?";
                jdbcTemplate.update(updateProjectSql, fetchedProjectName);
            } else {
                if (!"done".equals(status)) {
                    String updateProjectSql = "UPDATE projects SET status = ? WHERE project_name = ?";
                    jdbcTemplate.update(updateProjectSql, status, fetchedProjectName);
                }
            }

            System.out.println("Updated project: " + fetchedProjectName);
        } else {
            throw new RuntimeException("Task with ID " + id + " not found");
        }
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Task task, String oldTaskName, Project project) {
        String updateSubtaskSql = "UPDATE subtasks SET task_name = ?, time_spent = time_spent + ? WHERE task_name = ?";
        jdbcTemplate.update(updateSubtaskSql, task.getTaskName(), task.getTimeSpent(), oldTaskName);

        String updateTaskSql = "UPDATE tasks SET task_name = ?, time_spent = time_spent + ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(updateTaskSql, task.getTaskName(), task.getTimeSpent(), task.getPlannedStartDate(), task.getPlannedFinishDate(), id);

        String sql = "UPDATE projects SET time_spent = time_spent + ? WHERE project_name = ?";
        jdbcTemplate.update(sql, task.getTimeSpent(), task.getProjectName());

        System.out.println("Project name: " + task.getProjectName());
        System.out.println("Task name: " + task.getTaskName());

        String insertIntoTimeSpentTasks = "INSERT INTO time_spent_tasks (days_date, time_spent, task_name) VALUES (CURRENT_DATE, ?, ?)";
        jdbcTemplate.update(insertIntoTimeSpentTasks, task.getTimeSpent(), task.getTaskName());
    }

    // Sletter en task i databasen
    public void deleteTask(int id) {
        // Hent detaljer for opgaven baseret på ID
        String selectSql = "SELECT time_spent, project_name, time_to_spend FROM tasks WHERE id = ?";
        Map<String, Object> taskDetails = jdbcTemplate.queryForMap(selectSql, id);

        String selectSqlSub = "SELECT time_spent, project_name, time_to_spend FROM tasks WHERE id = ?";
        Map<String, Object> subTaskDetails = jdbcTemplate.queryForMap(selectSqlSub, id);

        if (taskDetails != null) {
            // Ekstraher værdier fra den hentede opgave
            Double taskTimeSpent = (Double) taskDetails.get("time_spent");
            String projectName = (String) taskDetails.get("project_name");
            Double timeToSpend = (Double) taskDetails.get("time_to_spend");

            // Log oplysninger for debugging (kan fjernes i produktion)
            System.out.println("Task Time Spent: " + taskTimeSpent);
            System.out.println("Project Name: " + projectName);
            System.out.println("Task Time To Spend: " + timeToSpend);

            // Kontroller om projektets navn ikke er null
            if (projectName != null) {
                // Opdater projektets time_spent og expected_time_to_spend
                String updateProjectSql = "UPDATE projects SET time_spent = time_spent - ?, expected_time_in_total = expected_time_in_total - ? WHERE project_name = ?";
                int rowsUpdated = jdbcTemplate.update(updateProjectSql, taskTimeSpent, timeToSpend, projectName);

                System.out.println("Rows updated in projects table: " + rowsUpdated);

                // Hvis opdateringen lykkedes, slet opgaven
                if (rowsUpdated > 0) {
                    String deleteTaskSql = "DELETE FROM tasks WHERE id = ?";
                    jdbcTemplate.update(deleteTaskSql, id);
                } else {
                    throw new IllegalStateException("No matching project found to updat time_spent.");
                }
            } else {
                // Kast en undtagelse, hvis projektets navn er null
                throw new IllegalStateException("Project name is null for task ID: " + id);
            }
        }

        if (subTaskDetails != null) {
            Double subTaskTimeSpent = (Double) subTaskDetails.get("time_spent");
            String projectName = (String) subTaskDetails.get("project_name");
            Double timeToSpend = (Double) subTaskDetails.get("time_to_spend");

            String updateProjectSqlFromSubTask = "UPDATE projects SET time_spent = time_spent - ?, expected_time_in_total = expected_time_in_total - ? WHERE project_name = ?";
            jdbcTemplate.update(updateProjectSqlFromSubTask, subTaskTimeSpent, timeToSpend, projectName);
        } else {
            // Kast en undtagelse, hvis opgaven ikke findes
            throw new IllegalArgumentException("Task with ID " + id + " was not found.");
        }

        // Opdater opgavetabellen (hvis nødvendigt)
        updateTasksTable();
    }

    // Opdaterer IDs i tasks tabellen
    public void updateTasksTable() {
        // Hent alle opgaver fra tasks-tabellen, sorteret efter id
        String sql = "SELECT * FROM tasks ORDER BY id";
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> task : tasks) {
            int originalID = (int) task.get("id");

            // Opdater ID i tasks-tabellen
            String updateSql = "UPDATE tasks SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            // Opdater task_id i resources_tasks-tabellen
            String updateSql2 = "UPDATE resources_tasks SET task_id = ? WHERE task_id = ?";
            jdbcTemplate.update(updateSql2, newId, originalID);

            // Hent og opdater WBS (Work Breakdown Structure) i tasks-tabellen
            String originalWBS = (String) task.get("wbs");
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\."); // Opdel WBS i dele baseret på '.'
                int lastDigit = Integer.parseInt(wbsParts[wbsParts.length - 1]); // Hent sidste tal i WBS
                if (lastDigit != 1) {
                    wbsParts[wbsParts.length - 1] = String.valueOf(lastDigit - 1); // Reducer sidste tal med 1
                    String updatedWBS = String.join(".", wbsParts); // Saml WBS igen

                    // Opdater WBS i tasks-tabellen
                    String updateWbsSql = "UPDATE tasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateWbsSql, updatedWBS, newId);
                }
            }

            newId++; // Forøg newId for næste opgave
        }

        // Hent alle underopgaver fra subtasks-tabellen
        String subtasksSql = "SELECT id, wbs FROM subtasks";
        List<Map<String, Object>> subtasks = jdbcTemplate.queryForList(subtasksSql);

        for (Map<String, Object> subtask : subtasks) {
            int subtaskId = (int) subtask.get("id");
            String originalWBS = (String) subtask.get("wbs");

            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\."); // Opdel WBS i dele baseret på '.'
                int secondDigit = Integer.parseInt(wbsParts[1]); // Hent andet tal i WBS
                if (secondDigit != 1) {
                    wbsParts[1] = String.valueOf(secondDigit - 1); // Reducer andet tal med 1
                    String updatedWBS = String.join(".", wbsParts); // Saml WBS igen

                    // Opdater WBS i subtasks-tabellen
                    String updateSubtasksWbsSql = "UPDATE subtasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateSubtasksWbsSql, updatedWBS, subtaskId);
                }
            }
        }
    }

    public int getHighestWbsIndexFromTasks(String mainProjectWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra opgaver-tabellen
        String sqlMySQL = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM tasks WHERE WBS LIKE CONCAT(?, '.%')";
        String sqlH2 = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS INT)) FROM tasks WHERE WBS LIKE CONCAT(?, '.%')";

        try {
            // Forsøg at køre MySQL-specifik forespørgsel
            Integer highestTaskIndex = jdbcTemplate.queryForObject(sqlMySQL, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
            // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
            return highestTaskIndex != null ? highestTaskIndex : 0;
        } catch (Exception eMySQL) {
            // Hvis MySQL-forespørgslen fejler, forsøg H2-forespørgslen i stedet
            System.err.println("MySQL query failed, attempting H2 query: " + eMySQL.getMessage());
            eMySQL.printStackTrace();

            try {
                // Kør H2-specifik forespørgsel
                Integer highestTaskIndex = jdbcTemplate.queryForObject(sqlH2, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
                // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
                return highestTaskIndex != null ? highestTaskIndex : 0;
            } catch (Exception eH2) {
                // Hvis H2-forespørgslen også fejler, log fejlen og returner 0
                System.err.println("H2 query failed: " + eH2.getMessage());
                eH2.printStackTrace();

                // Returner 0, hvis begge forespørgsler fejler
                return 0; // Standard WBS-værdi
            }
        }
    }

    // Mapper resultatet af SQL-spørgsmål til et Task-objekt
    private static class TaskRowMapper implements RowMapper<Task> {
        @Override
        public Task mapRow(ResultSet rs, int rowNum) throws SQLException {
            Task task = new Task();
            task.setId(rs.getInt("id"));
            task.setWbs(rs.getString("WBS"));
            task.setMainProjectName(rs.getString("project_name"));
            task.setTaskName(rs.getString("task_name"));
            task.setTimeSpent(rs.getDouble("time_spent"));
            task.setTimeToSpend(rs.getDouble("time_to_spend"));
            task.setStatus(rs.getString("status"));
            task.setDuration(rs.getInt("duration"));
            task.setPlannedStartDate(rs.getString("planned_start_date"));
            task.setPlannedFinishDate(rs.getString("planned_finish_date"));
            task.setAssigned(rs.getString("assigned"));
            return task;  // Returner det mapperede Task-objekt
        }
    }
}
