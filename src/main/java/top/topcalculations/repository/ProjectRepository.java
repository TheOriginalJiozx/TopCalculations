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
                "VALUES (?, ?, ?, ?, ?, DATEDIFF(DAY, ?,?), ?, ?)";
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
        String sql = "INSERT INTO subtasks (WBS, task_name, sub_task_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                "VALUES (?, ?, ?, ?, ?, DATEDIFF(?,?), ?, ?)";

        // Udfører SQL-spørgsmålet og gemmer delopgaven i databasen
        jdbcTemplate.update(sql, subTask.getWbs(), subTask.getTaskProjectName(), subTask.getSubTaskName(),
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

    private void updateTimeToSpendIfNecessary(Project subTask) {
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

        Double totalSubtaskTime = jdbcTemplate.queryForObject(calculateSubtaskTimeQuery, Double.class, subTask.getTaskProjectName());
        if (totalSubtaskTime == null) {
            totalSubtaskTime = 0.0;
        }

        Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeQuery, Double.class, subTask.getTaskProjectName());
        if (taskTimeToSpend == null) {
            taskTimeToSpend = 0.0;
        }

        if (totalSubtaskTime > taskTimeToSpend) {
            jdbcTemplate.update(updateTaskTimeQuery, totalSubtaskTime, subTask.getTaskProjectName());
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
        String sql = "SELECT * FROM projects";
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
        }

        return projects;
    }

    // Henter alle opgaver fra databasen
    public List<Project> getAllTasks() {
        String sql = "SELECT * FROM tasks";
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
        }

        return tasks;
    }

    // Henter alle underopgaver fra databasen
    public List<Project> getAllSubTasks() {
        String sql = "SELECT * FROM subtasks";
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
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.assigned, st.task_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
                "FROM subtasks st " +
                "WHERE st.id = ? AND st.sub_task_name IS NOT NULL AND st.sub_task_name != ''";
        return jdbcTemplate.query(sql, new SubTaskRowMapper(), id);
    }

    // Henter en underopgave baseret på dens ID (specifik underopgave)
    public Project findSubTaskByIDForStatus(Long id) {
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.assigned, st.task_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
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
    public void updateTaskStatusByID(Long id, String status) {
        Project task = findTaskByIDForStatus(id);
        if (task != null) {
            task.setStatus(status);
            String sql = "UPDATE tasks SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        } else {
            throw new RuntimeException("Task with ID " + id + " not found");
        }
    }

    // Opdaterer en underopgaves status
    public void updateSubTaskStatusByID(Long id, String status) {
        Project subTask = findSubTaskByIDForStatus(id);
        if (subTask != null) {
            subTask.setStatus(status);
            String sql = "UPDATE subtasks SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        } else {
            throw new RuntimeException("Subtask with ID " + id + " not found");
        }
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project task, String oldTaskName) {
        String updateSubtaskSql = "UPDATE subtasks SET task_name = ?, time_spent = time_spent + ? WHERE task_name = ?";
        jdbcTemplate.update(updateSubtaskSql, task.getTaskProjectName(), task.getTimeSpent(), oldTaskName);

        String updateTaskSql = "UPDATE tasks SET task_name = ?, time_spent = time_spent + ? WHERE id = ?";
        jdbcTemplate.update(updateTaskSql, task.getTaskProjectName(), task.getTimeSpent(), id); // Pass time spent and id

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

        String updateProjectSql = "UPDATE projects SET time_spent = time_spent + ? WHERE project_name = (SELECT t.project_name FROM tasks t WHERE t.task_name = ?)";
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
        String sql = "DELETE FROM tasks WHERE id = ?";
        jdbcTemplate.update(sql, id);

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
    public void deleteSubTask(int id) {
        String sql = "DELETE FROM subtasks WHERE id = ?";
        jdbcTemplate.update(sql, id);

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

    /*private String updateWBS(String originalWBS, int newId) {
        String[] parts = originalWBS.split("\\.");
        if (parts.length == 3) {
            parts[2] = String.valueOf(newId);
        }

        return String.join(".", parts);
    }*/

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
            subTask.setTimeSpent(rs.getDouble("time_spent"));
            subTask.setTimeToSpend(rs.getDouble("time_to_spend"));
            subTask.setDuration(rs.getInt("duration"));
            subTask.setPlannedStartDate(rs.getString("planned_start_date"));
            subTask.setPlannedFinishDate(rs.getString("planned_finish_date"));
            subTask.setAssigned(rs.getString("assigned"));
            return subTask; // Returner det mapperede Subtask-objekt
        }
    }
}
