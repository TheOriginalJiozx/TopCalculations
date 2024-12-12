package top.topcalculations.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@Repository

public class ProjectRepository {
    private JdbcTemplate jdbcTemplate;

    // Konstruktør, der initialiserer JdbcTemplate med en DataSource
    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void saveProject(Project project) {
        // Generer ny WBS, hvis nødvendigt
        if (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty()) {
            String newWBS = generateNewWBS();  // Generer ny WBS
            project.setWbs(newWBS);  // Sæt den nye WBS for projektet
        }

        System.out.println("Saving project: " + project);

        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) " +
                "VALUES (?, ?, DATEDIFF(?, ?), ?, ?, ?, 0)";
        jdbcTemplate.update(sql,
                project.getWbs(),
                project.getProjectTaskName(),
                project.getPlannedFinishDate(),
                project.getPlannedStartDate(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                project.getAssigned());
    }

    public void saveTask(Project task) {
        // Udskriver hvilken opgave der gemmes
        System.out.println("Saving task: " + task);

        // SQL-spørgsmål for at indsætte en opgave i tasks-tabellen
        String sql = "INSERT INTO tasks (WBS, project_name, task_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                "VALUES (?, ?, ?, ?, ?, DATEDIFF(?,?), ?, ?)";
        // Udfører SQL-spørgsmålet og gemmer opgaven i databasen
        jdbcTemplate.update(sql, task.getWbs(), task.getProjectTaskName(), task.getTaskProjectName(),
                task.getTimeToSpend(), task.getAssigned(), task.getPlannedFinishDate(), task.getPlannedStartDate(), task.getPlannedStartDate(), task.getPlannedFinishDate());

        // SQL-spørgsmål for at finde ID'et for den gemte opgave
        String findTaskIdQuery = "SELECT id FROM tasks WHERE WBS = ? AND task_name = ?";
        // Henter ID'et for opgaven baseret på WBS og task_name
        Long taskId = jdbcTemplate.queryForObject(findTaskIdQuery, Long.class, task.getWbs(), task.getTaskProjectName());

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

        addExpectedTimeToProjectFromTask(task);
    }

    // Gemmer en delopgave (subtask) i databasen
    public void saveSubTask(Project subTask) {

        // Udskriver hvilken delopgave der gemmes
        System.out.println("Saving subtask: " + subTask);

        // Justeret SQL-spørgsmål: Sørg for, at DATEDIFF beregner varigheden korrekt
        String sql = "INSERT INTO subtasks (WBS, task_name, sub_task_name, project_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                "VALUES (?, ?, ?, (SELECT project_name FROM tasks WHERE task_name = ?), ?, ?, DATEDIFF(?, ?), ?, ?)";

        // Udfører SQL-spørgsmålet og gemmer delopgaven i databasen
        jdbcTemplate.update(sql, subTask.getWbs(), subTask.getTaskProjectName(), subTask.getSubTaskName(), subTask.getTaskProjectName(),
                subTask.getTimeToSpend(), subTask.getAssigned(), subTask.getPlannedFinishDate(), subTask.getPlannedStartDate(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate());

        // SQL-spørgsmål for at finde ID'et for den gemte delopgave
        String findSubTaskIdQuery = "SELECT id FROM subtasks WHERE WBS = ? AND sub_task_name = ?";
        // Henter ID'et for delopgaven baseret på WBS og sub_task_name
        Long subTaskId = jdbcTemplate.queryForObject(findSubTaskIdQuery, Long.class, subTask.getWbs(), subTask.getSubTaskName());

        // Hvis subTaskId ikke er gyldigt, kastes en undtagelse
        if (subTaskId == null || subTaskId <= 0) {
            throw new IllegalStateException("Failed to retrieve valid subtask ID after insert.");
        }

        // Udskriver det fundne subtask ID
        System.out.println("Subtask ID: " + subTaskId);

        // SQL-spørgsmål for at indsætte relationen mellem delopgaven og ressourcen i resources_subtasks-tabellen
        String sql2 = "INSERT INTO resources_subtasks (resource_name, sub_task_id) VALUES (?, ?)\n";
        // Gemmer ressourcen for delopgaven i databasen
        jdbcTemplate.update(sql2, subTask.getResource_name(), subTaskId);

        addExpectedTimeToProjectFromSubTask(subTask);
    }

    private void addExpectedTimeToProjectFromTask(Project task) {
        String sql = "UPDATE projects SET expected_time_in_total = expected_time_in_total + ? WHERE project_name = ?";
        jdbcTemplate.update(sql, task.getTimeToSpend(), task.getProjectTaskName());
    }

    private void addExpectedTimeToProjectFromSubTask(Project subTask) {
        String sql = "UPDATE projects SET expected_time_in_total = expected_time_in_total + ? WHERE project_name = (SELECT t.project_name FROM tasks t WHERE t.task_name = ?)";
        jdbcTemplate.update(sql, subTask.getTimeToSpend(), subTask.getTaskProjectName());

        updateTimeToSpendIfNecessary(subTask);
    }

    private void updateTimeToSpendIfNecessary(Project name) {
        String calculateSubtaskTimeQuery =
                "SELECT SUM(st.time_to_spend) " +
                        "FROM subtasks st " +
                        "WHERE st.task_name = ?";

        String getTaskTimeQuery =
                "SELECT time_to_spend " +
                        "FROM tasks " +
                        "WHERE task_name = ?";

        String updateTaskTimeQuery =
                "UPDATE tasks " +
                        "SET time_to_spend = ? " +
                        "WHERE task_name = ?";

        Double totalSubtaskTime = jdbcTemplate.queryForObject(calculateSubtaskTimeQuery, Double.class, name.getTaskProjectName());
        if (totalSubtaskTime == null) {
            totalSubtaskTime = 0.0;
        }

        Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeQuery, Double.class, name.getTaskProjectName());
        if (taskTimeToSpend == null) {
            taskTimeToSpend = 0.0;
        }

        if (totalSubtaskTime > taskTimeToSpend) {
            jdbcTemplate.update(updateTaskTimeQuery, totalSubtaskTime, name.getTaskProjectName());
        }
    }

    // Genererer en ny WBS ved at finde den højeste WBS-værdi og øge den
    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;  // Start med 1, hvis ingen WBS findes
        return String.valueOf(newWBSValue);  // Returner den nye WBS
    }

    // Finder et projekt baseret på dets navn
    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);  // Returner projektet eller null, hvis ikke fundet
    }

    // Finder en opgave baseret på dens navn
    public Project findTaskByName(String taskName) {
        String sql = "SELECT * FROM tasks WHERE task_name = ?";
        List<Project> tasks = jdbcTemplate.query(sql, new Object[]{taskName}, new TaskRowMapper());
        return tasks.isEmpty() ? null : tasks.get(0);  // Returner opgaven eller null, hvis ikke fundet
    }

    // Henter alle projekter fra databasen
    public List<Project> getAllProjects() {
        String sql = "SELECT *, expected_time_in_total, status FROM projects";
        List<Project> projects = jdbcTemplate.query(sql, new ProjectRowMapper());

        for (Project project : projects) {
            if (project.getProjectTaskName() != null && !project.getProjectTaskName().isEmpty() &&
                    (project.getTaskProjectName() == null || project.getTaskProjectName().isEmpty())) {
                project.setProjectTaskName(project.getProjectTaskName());
            } else {
                project.setProjectTaskName(project.getTaskProjectName() != null && !project.getTaskProjectName().isEmpty()
                        ? project.getTaskProjectName()
                        : project.getProjectTaskName());
            }

            project.setTimeToSpend(project.getExpectedTimeInTotal());
            project.setStatus(project.getStatus());
        }

        return projects;
    }

    // Henter alle opgaver fra databasen
    public List<Project> getAllTasks() {
        String sql = "SELECT *, time_to_spend, status FROM tasks";  // Adjust column name as needed
        List<Project> tasks = jdbcTemplate.query(sql, new TaskRowMapper());

        for (Project task : tasks) {
            if (task.getProjectTaskName() != null && !task.getProjectTaskName().isEmpty() &&
                    (task.getTaskProjectName() == null || task.getTaskProjectName().isEmpty())) {
                task.setProjectTaskName(task.getProjectTaskName());
            } else {
                task.setProjectTaskName(task.getTaskProjectName() != null && !task.getTaskProjectName().isEmpty()
                        ? task.getTaskProjectName()
                        : task.getProjectTaskName());
            }

            task.setTimeToSpend(task.getTimeToSpend());
            task.setStatus(task.getStatus());
        }

        return tasks;
    }

    // Henter alle underopgaver fra databasen
    public List<Project> getAllSubTasks() {
        String sql = "SELECT *, status FROM subtasks";
        List<Project> subTasks = jdbcTemplate.query(sql, new SubTaskRowMapper());

        for (Project subTask : subTasks) {
            if (subTask.getTaskProjectName() != null && !subTask.getTaskProjectName().isEmpty() &&
                    (subTask.getSubTaskName() == null || subTask.getSubTaskName().isEmpty())) {
                subTask.setTaskProjectName(subTask.getTaskProjectName());
            } else {
                subTask.setProjectTaskName(subTask.getSubTaskName() != null && !subTask.getSubTaskName().isEmpty()
                        ? subTask.getSubTaskName()
                        : subTask.getTaskProjectName());
            }

            subTask.setStatus(subTask.getStatus());

        }

        return subTasks; // Returner alle projekter
    }

    // Finder alle opgaver fra databasen
    public List<Project> findAllTasks() {
        String sql = "SELECT * FROM tasks WHERE task_name IS NOT NULL AND task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper());
    }

    // Henter en opgave baseret på dens ID
    public List<Project> findTaskByID(Long id) {
        String sql = "SELECT t.id, t.wbs, t.project_name, t.time_to_spend, t.task_name, t.assigned, t.time_spent, t.duration, t.planned_start_date, t.planned_finish_date, t.status " +
                "FROM tasks t " +
                "WHERE t.id = ? AND t.task_name IS NOT NULL AND t.task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Henter en opgave baseret på dens ID (specifik opgave)
    public Project findTaskByIDForStatus(Long id) {
        String sql = "SELECT t.id, t.wbs, t.project_name, t.time_to_spend, t.task_name, t.assigned, t.time_spent, t.duration, t.planned_start_date, t.planned_finish_date, t.status " +
                "FROM tasks t " +
                "WHERE t.id = ? AND t.task_name IS NOT NULL AND t.task_name != ''";
        return jdbcTemplate.queryForObject(sql, new TaskRowMapper(), id);
    }

    // Henter et projekt baseret på dens ID
    public List<Project> findProjectByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.query(sql, new ProjectRowMapper(), id);
    }

    // Henter et projekt baseret på dens ID (specifikt projekt)
    public Project findProjectByIDForStatus(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.queryForObject(sql, new ProjectRowMapper(), id);
    }

    // Henter en underopgave baseret på dens ID
    public List<Project> findSubTaskByID(Long id) {
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.assigned, st.task_name, st.project_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
                "FROM subtasks st " +
                "WHERE st.id = ? AND st.sub_task_name IS NOT NULL AND st.sub_task_name != ''";
        return jdbcTemplate.query(sql, new SubTaskRowMapper(), id);
    }

    // Henter en underopgave baseret på dens ID (specifik underopgave)
    public Project findSubTaskByIDForStatus(Long id) {
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.project_name, st.assigned, st.task_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
                "FROM subtasks st " +
                "WHERE st.id = ? AND st.sub_task_name IS NOT NULL AND st.sub_task_name != ''";
        return jdbcTemplate.queryForObject(sql, new SubTaskRowMapper(), id);
    }

    // Opdaterer et projekts status
    public void updateProjectStatusByID(Long id, String status) {
        Project project = findProjectByIDForStatus(id);
        if (project != null) {
            project.setStatus(status);
            String sql = "UPDATE projects SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        } else {
            throw new RuntimeException("Project with ID " + id + " not found");
        }
    }

    // Opdaterer en opgaves status
    public void updateTaskStatusByID(Long id, String status, String projectName) {
        Project task = findTaskByIDForStatus(id);
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

    // Opdaterer en underopgaves status
    public void updateSubTaskStatusByID(Long id, String status) {
        Project subTask = findSubTaskByIDForStatus(id);
        if (subTask != null) {
            subTask.setStatus(status);

            // Update subtask status
            String sql = "UPDATE subtasks SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);

            // Fetch the task name associated with the subtask
            String taskQuery = "SELECT task_name FROM subtasks WHERE id = ?";
            String fetchedTaskName = jdbcTemplate.queryForObject(taskQuery, String.class, id);

            // Check if all subtasks for the task are "done"
            String checkSubtasksQuery = "SELECT COUNT(*) FROM subtasks WHERE task_name = ? AND status != 'done'";
            Integer countNotDoneSubtasks = jdbcTemplate.queryForObject(checkSubtasksQuery, Integer.class, fetchedTaskName);

            // Fetch project name
            String projectQuery = "SELECT project_name FROM subtasks WHERE id = ?";
            String fetchedProjectName = jdbcTemplate.queryForObject(projectQuery, String.class, id);

            // If all subtasks are done, update the task status to "done"
            if (countNotDoneSubtasks == 0) {
                String updateTaskSql = "UPDATE tasks SET status = 'done' WHERE task_name = ?";
                jdbcTemplate.update(updateTaskSql, fetchedTaskName);

                // After updating task status, check if all tasks for the project are done
                String checkTasksQuery = "SELECT COUNT(*) FROM tasks WHERE project_name = ? AND status != 'done'";
                Integer countNotDoneTasks = jdbcTemplate.queryForObject(checkTasksQuery, Integer.class, fetchedProjectName);

                // If all tasks are done, update the project status to "done"
                if (countNotDoneTasks == 0) {
                    String updateProjectSql = "UPDATE projects SET status = 'done' WHERE project_name = ?";
                    jdbcTemplate.update(updateProjectSql, fetchedProjectName);
                }
            } else {
                // Otherwise, update the task and project statuses to the current subtask status, unless "done"
                if (!"done".equals(status)) {
                    String updateTaskSql = "UPDATE tasks SET status = ? WHERE task_name = ?";
                    jdbcTemplate.update(updateTaskSql, status, fetchedTaskName);

                    // Update project status as well (if not "done")
                    String updateProjectSql = "UPDATE projects SET status = ? WHERE project_name = ?";
                    jdbcTemplate.update(updateProjectSql, status, fetchedProjectName);
                }
            }

            // Now that fetchedProjectName is in scope, we can print it
            System.out.println("Updated project: " + fetchedProjectName);

        } else {
            throw new RuntimeException("Subtask with ID " + id + " not found");
        }
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project task, String oldTaskName) {
        String updateSubtaskSql = "UPDATE subtasks SET task_name = ?, time_spent = time_spent + ? WHERE task_name = ?";
        jdbcTemplate.update(updateSubtaskSql, task.getTaskProjectName(), task.getTimeSpent(), oldTaskName);

        String updateTaskSql = "UPDATE tasks SET task_name = ?, time_spent = time_spent + ? WHERE id = ?";
        jdbcTemplate.update(updateTaskSql, task.getTaskProjectName(), task.getTimeSpent(), id);

        String sql = "UPDATE projects SET time_spent = time_spent + ? WHERE project_name = ?";
        jdbcTemplate.update(sql, task.getTimeSpent(), task.getProjectTaskName());

        System.out.println("Project name: " + task.getProjectTaskName());

        String insertIntoTimeSpentTasks = "INSERT INTO time_spent_tasks (days_date, time_spent, task_name) VALUES (CURRENT_DATE, ?, ?)";
        jdbcTemplate.update(insertIntoTimeSpentTasks, task.getTimeSpent(), task.getTaskProjectName());
    }

    // Opdaterer en underopgave i databasen
    public void updateSubTask(int id, Project subtask, String oldSubTaskName) {
        String sql = "UPDATE subtasks SET sub_task_name = ?, time_spent = time_spent + ? WHERE id = ?";
        jdbcTemplate.update(sql, subtask.getSubTaskName(), subtask.getTimeSpent(), id);

        String updateTaskSql = "UPDATE tasks SET time_spent = time_spent + ? WHERE task_name = ?";
        jdbcTemplate.update(updateTaskSql, subtask.getTimeSpent(), subtask.getTaskProjectName());

        String updateProjectSql = "UPDATE projects SET time_spent = time_spent + ? WHERE project_name = ?";
        jdbcTemplate.update(updateProjectSql, subtask.getTimeSpent(), subtask.getProjectTaskName());

        System.out.println("Task name: " + subtask.getTaskProjectName());

        String insertIntoTimeSpentSubTasks = "INSERT INTO time_spent_subtasks (days_date, time_spent, sub_task_name) VALUES (CURRENT_DATE, ?, ?)";
        jdbcTemplate.update(insertIntoTimeSpentSubTasks, subtask.getTimeSpent(), subtask.getSubTaskName());
    }

    // Opdaterer et projekt i databasen
    public void updateProject(int id, Project project) {
        String updateProjectSql = "UPDATE projects SET project_name = ?, duration = DATEDIFF(?, ?), planned_start_date = ?, planned_finish_date = ?, expected_time_in_total = ? WHERE id = ?";
        jdbcTemplate.update(updateProjectSql,
                project.getProjectTaskName(),
                project.getPlannedFinishDate(),
                project.getPlannedStartDate(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                project.getExpectedTimeInTotal(),
                id);
    }

    // Sletter et projekt i databasen
    public void deleteProject(int id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        jdbcTemplate.update(sql, id);

        updateProjectsTable();
    }

    // Opdaterer IDs i projects tabellen
    public void updateProjectsTable() {
        String sql = "SELECT * FROM projects ORDER BY id";
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> project : projects) {
            int originalID = (int) project.get("id");

            String updateSql = "UPDATE projects SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            newId++;
        }
    }

    // Sletter en task i databasen
    public void deleteTask(int id) {
        // Hent detaljer for opgaven baseret på ID
        String selectSql = "SELECT time_spent, project_name, time_to_spend FROM tasks WHERE id = ?";
        Map<String, Object> taskDetails = jdbcTemplate.queryForMap(selectSql, id);

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
        } else {
            // Kast en undtagelse, hvis opgaven ikke findes
            throw new IllegalArgumentException("Task with ID " + id + " was not found.");
        }

        // Opdater opgavetabellen (hvis nødvendigt)
        updateTasksTable();
    }

    // Opdaterer IDs i tasks tabellen
    public void updateTasksTable() {
        String sql = "SELECT * FROM tasks ORDER BY id";
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> task : tasks) {
            int originalID = (int) task.get("id");

            String updateSql = "UPDATE tasks SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            String updateSql2 = "UPDATE resources_tasks SET task_id = ? WHERE task_id = ?";
            jdbcTemplate.update(updateSql2, newId, originalID);

            newId++;
        }
    }

    // Sletter en subtask i databasen
    // Sletter en subtask i databasen
    public void deleteSubTask(int id) {
        // Hent detaljer for underopgaven baseret på ID
        String selectSql = "SELECT time_spent, project_name, task_name, time_to_spend FROM subtasks WHERE id = ?";
        Map<String, Object> subTaskDetails = jdbcTemplate.queryForMap(selectSql, id);

        if (subTaskDetails != null) {
            // Ekstraher værdier fra den hentede underopgave
            Double subTaskTimeSpent = (Double) subTaskDetails.get("time_spent");
            String projectName = (String) subTaskDetails.get("project_name");
            String taskName = (String) subTaskDetails.get("task_name");
            Double subTaskTimeToSpend = (Double) subTaskDetails.get("time_to_spend");

            // Log oplysninger for debugging (kan fjernes i produktion)
            System.out.println("SubTask Time Spent: " + subTaskTimeSpent);
            System.out.println("Project Name: " + projectName);
            System.out.println("Task Name: " + taskName);
            System.out.println("SubTask Time To Spend: " + subTaskTimeToSpend);

            // Kontroller om projektets navn og taskens navn ikke er null
            if (projectName != null && taskName != null) {
                // Opdater projektets time_spent og expected_time_in_total
                String updateProjectSql = "UPDATE projects SET time_spent = time_spent - ?, expected_time_in_total = expected_time_in_total - ? WHERE project_name = ?";
                int rowsUpdated = jdbcTemplate.update(updateProjectSql, subTaskTimeSpent, subTaskTimeToSpend, projectName);
                System.out.println("Rows updated in projects table: " + rowsUpdated);

                // Hent antal subtasks for den givne task_name
                String countSubTasksSql = "SELECT COUNT(*) FROM subtasks WHERE task_name = ?";
                Integer subTaskCount = jdbcTemplate.queryForObject(countSubTasksSql, new Object[]{taskName}, Integer.class);

                // Hent den aktuelle time_to_spend fra tasks
                String getTaskTimeToSpendSql = "SELECT time_to_spend FROM tasks WHERE task_name = ?";
                Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeToSpendSql, new Object[]{taskName}, Double.class);

                // Opdater taskens time_to_spend (det skal altid opdateres)
                String updateTaskSql = "UPDATE tasks SET time_spent = time_spent - ?, time_to_spend = time_to_spend - ? WHERE task_name = ?";
                jdbcTemplate.update(updateTaskSql, subTaskTimeSpent, subTaskTimeToSpend, taskName);
                System.out.println("Rows updated in tasks table: " + updateTaskSql);

                // Hvis projektopdateringen lykkedes, slet underopgaven
                if (rowsUpdated > 0) {
                    String deleteSubTaskSql = "DELETE FROM subtasks WHERE id = ?";
                    jdbcTemplate.update(deleteSubTaskSql, id);
                } else {
                    throw new IllegalStateException("Ingen matchende projekt fundet til at opdatere time_spent.");
                }
            } else {
                // Kast en undtagelse, hvis projektets eller taskens navn er null
                throw new IllegalStateException("Projektets eller taskens navn er null for subtask ID: " + id);
            }
        } else {
            // Kast en undtagelse, hvis underopgaven ikke findes
            throw new IllegalArgumentException("Subtask med ID " + id + " blev ikke fundet.");
        }

        // Opdater underopgavetabellen (hvis nødvendigt)
        updateSubTasksTable();
    }

    // Opdaterer IDs i subtasks tabellen
    public void updateSubTasksTable() {
        String sql = "SELECT * FROM subtasks ORDER BY id";
        List<Map<String, Object>> subtasks = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> subtask : subtasks) {
            int originalID = (int) subtask.get("id");

            String updateSql = "UPDATE subtasks SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            String updateSql2 = "UPDATE resources_subtasks SET sub_task_id = ? WHERE sub_task_id = ?";
            jdbcTemplate.update(updateSql2, newId, originalID);

            newId++;
        }
    }

    // Finder projekter
    public List<Project> findAllProjects() {
        String sql = "SELECT * FROM projects";
        return jdbcTemplate.query(sql, new ProjectRowMapper());
    }

    public int getHighestWbsIndexFromProjects(String mainProjectWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra projekter-tabellen
        String sql = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";
        // Udfør forespørgslen og få resultatet som en Integer
        Integer highestTaskIndex = jdbcTemplate.queryForObject(sql, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
        // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
        return highestTaskIndex != null ? highestTaskIndex : 0;
    }

    public int getHighestWbsIndexFromTasks(String mainProjectWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra opgaver-tabellen
        String sql = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM tasks WHERE WBS LIKE CONCAT(?, '.%')";
        // Udfør forespørgslen og få resultatet som en Integer
        Integer highestTaskIndex = jdbcTemplate.queryForObject(sql, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
        // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
        return highestTaskIndex != null ? highestTaskIndex : 0;
    }

    public int getHighestWbsIndexFromSubTasks(String mainTaskWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra opgaver-tabellen
        String sql = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM subtasks WHERE WBS LIKE CONCAT(?, '.%')";
        // Udfør forespørgslen og få resultatet som en Integer
        Integer highestSubtaskIndex = jdbcTemplate.queryForObject(sql, new Object[]{mainTaskWBS, mainTaskWBS}, Integer.class);
        // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
        return highestSubtaskIndex != null ? highestSubtaskIndex : 0;
    }

    // Mapper resultatet af SQL-spørgsmål til et Project-objekt
    private static class ProjectRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setWbs(rs.getString("WBS"));
            project.setProjectTaskName(rs.getString("project_name"));
            project.setTimeSpent(rs.getDouble("time_spent"));
            project.setExpectedTimeInTotal(rs.getDouble("expected_time_in_total"));
            project.setDuration(rs.getInt("duration"));
            project.setStatus(rs.getString("status"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setAssigned(rs.getString("assigned"));
            return project;  // Returner det mapperede Project-objekt
        }
    }

    // Mapper resultatet af SQL-spørgsmål til et Task-objekt
    private static class TaskRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project task = new Project();
            task.setId(rs.getInt("id"));
            task.setWbs(rs.getString("WBS"));
            task.setMainProjectName(rs.getString("project_name"));
            task.setTaskProjectName(rs.getString("task_name"));
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

    // Mapper resultatet af SQL-spørgsmål til et Subtask-objekt
    private static class SubTaskRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project subTask = new Project();
            subTask.setId(rs.getInt("id"));
            subTask.setWbs(rs.getString("WBS"));
            subTask.setTaskProjectName(rs.getString("task_name"));
            subTask.setSubTaskName(rs.getString("sub_task_name"));
            subTask.setProjectTaskName(rs.getString("project_name"));
            subTask.setTimeSpent(rs.getDouble("time_spent"));
            subTask.setTimeToSpend(rs.getDouble("time_to_spend"));
            subTask.setStatus(rs.getString("status"));
            subTask.setDuration(rs.getInt("duration"));
            subTask.setPlannedStartDate(rs.getString("planned_start_date"));
            subTask.setPlannedFinishDate(rs.getString("planned_finish_date"));
            subTask.setAssigned(rs.getString("assigned"));
            return subTask; // Returner det mapperede Subtask-objekt
        }
    }
}