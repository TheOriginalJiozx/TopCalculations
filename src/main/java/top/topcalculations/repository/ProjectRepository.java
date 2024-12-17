package top.topcalculations.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;
import top.topcalculations.model.Task;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

@Repository
public class ProjectRepository {
    private JdbcTemplate jdbcTemplate;

    // Konstruktør, der initialiserer JdbcTemplate med en DataSource
    // The constructor initializes the JdbcTemplate with a DataSource.
    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Gemmer et projekt i databasen
    // Saves a project in the database.
    public void saveProject(Task task, Project project) {
        // Generer en ny WBS, hvis nødvendigt
        // If the task name is not provided, generate a new WBS for the project.
        if (task.getTaskName() == null || task.getTaskName().isEmpty()) {
            String newWBS = generateNewWBS();  // Generer en ny WBS
            project.setWbs(newWBS);  // Sæt den nye WBS for projektet
        }

        System.out.println("Gemmer projekt: " + project);

        // Første SQL forespørgsel for at gemme projektet i databasen
        // First SQL query to insert the project into the database.
        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) " +
                "VALUES (?, ?, DATEDIFF(?, ?), ?, ?, ?, 0)";

        try {
            // Forsøg at udføre den første SQL
            // Attempt to execute the first SQL query
            jdbcTemplate.update(sql,
                    project.getWbs(),
                    project.getProjectName(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getAssigned());
        } catch (Exception e) {
            // Hvis den første SQL fejler, udfør den alternative forespørgsel
            // If the first SQL query fails, try the fallback query.
            System.out.println("First SQL query failed, trying to execute alternate query.");

            String fallbackSql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) " +
                    "VALUES (?, ?, DATEDIFF(DAY, ?, ?), ?, ?, ?, 0)";
            jdbcTemplate.update(fallbackSql,
                    project.getWbs(),
                    project.getProjectName(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getAssigned());
        }
    }

    // Opdaterer tid for opgave hvis nødvendigt
    // Updates the time for a task if necessary.
    void updateTimeToSpendIfNecessary(Task name) {
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

        // Beregn den samlede tid for subtasks
        // Calculate the total time for subtasks.
        Double totalSubtaskTime = jdbcTemplate.queryForObject(calculateSubtaskTimeQuery, Double.class, name.getTaskName());
        if (totalSubtaskTime == null) {
            totalSubtaskTime = 0.0;
        }

        // Hent den eksisterende tid for opgaven
        // Get the current time to spend for the task.
        Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeQuery, Double.class, name.getTaskName());
        if (taskTimeToSpend == null) {
            taskTimeToSpend = 0.0;
        }

        // Opdater tid, hvis nødvendigt
        // Update the time if the subtask time is greater than the current task time.
        if (totalSubtaskTime > taskTimeToSpend) {
            jdbcTemplate.update(updateTaskTimeQuery, totalSubtaskTime, name.getTaskName());
        }
    }

    // Genererer en ny WBS ved at finde den højeste WBS-værdi og øge den
    // Generates a new WBS by finding the highest WBS value and incrementing it.
    private String generateNewWBS() {
        // MySQL-specifik forespørgsel
        // MySQL-specific query
        String sqlMySQL = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        // H2-specifik forespørgsel
        // H2-specific query
        String sqlH2 = "SELECT MAX(CAST(WBS AS INT)) FROM projects";

        try {
            // Forsøg at køre MySQL-forespørgslen først
            // Try running the MySQL query first.
            Integer highestWBS = jdbcTemplate.queryForObject(sqlMySQL, Integer.class);
            // Hvis ingen WBS findes, start med 1, ellers inkrementer den højeste WBS
            // If no WBS is found, start with 1, otherwise increment the highest WBS.
            int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
            // Returner den nye WBS-værdi
            // Return the new WBS value.
            return String.valueOf(newWBSValue);
        } catch (Exception eMySQL) {
            // Hvis MySQL-forespørgslen fejler, forsøg H2-forespørgslen i stedet
            // If the MySQL query fails, try the H2 query instead.
            System.err.println("MySQL-forespørgsel mislykkedes, forsøger H2-forespørgsel: " + eMySQL.getMessage());
            eMySQL.printStackTrace();

            try {
                // Kør H2-specifik forespørgsel
                // Run the H2-specific query.
                Integer highestWBS = jdbcTemplate.queryForObject(sqlH2, Integer.class);
                // Hvis ingen WBS findes, start med 1, ellers inkrementer den højeste WBS
                // If no WBS is found, start with 1, otherwise increment the highest WBS.
                int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
                // Returner den nye WBS-værdi
                // Return the new WBS value.
                return String.valueOf(newWBSValue);
            } catch (Exception eH2) {
                // Hvis H2-forespørgslen også fejler, log fejlen og returner en standardværdi
                // If the H2 query also fails, log the error and return a default value.
                System.err.println("H2-forespørgsel mislykkedes: " + eH2.getMessage());
                eH2.printStackTrace();

                // Returner en standard WBS-værdi, hvis begge forespørgsler fejler
                // Return a default WBS value if both queries fail.
                return "1"; // Standard WBS-værdi
            }
        }
    }

    // Finder et projekt baseret på dets navn
    // Finds a project based on its name.
    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);  // Returner projektet eller null, hvis ikke fundet
        // Return the project or null if not found.
    }

    // Henter alle projekter fra databasen
    // Retrieves all projects from the database.
    public List<Project> getAllProjects() {
        String sql = "SELECT *, expected_time_in_total, status FROM projects";
        List<Project> projects = jdbcTemplate.query(sql, new ProjectRowMapper());

        for (Project project : projects) {
            // Hvis projektet har et navn, men ikke en opgave, sættes projektets navn til det
            // If the project has a name but no task, set the project name to it.
            if (project.getProjectName() != null && !project.getProjectName().isEmpty() &&
                    (project.getTaskName() == null || project.getTaskName().isEmpty())) {
                project.setProjectName(project.getProjectName());
            } else {
                // Hvis der er en opgave, brug opgavenavnet
                // If there is a task, use the task name.
                project.setProjectName(project.getTaskName() != null && !project.getTaskName().isEmpty()
                        ? project.getTaskName()
                        : project.getProjectName());
            }

            // Sæt den estimerede tid og status
            // Set the expected time and status.
            project.setTimeToSpend(project.getExpectedTimeInTotal());
            project.setStatus(project.getStatus());
        }

        return projects;
    }



    // Henter et projekt baseret på dens ID
    public List<Project> findProjectByID(Long id) {
        // SQL-forespørgsel til at hente projektet baseret på ID
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.query(sql, new ProjectRowMapper(), id); // Returner resultatet som en liste af projekter
    }

    // Henter et projekt baseret på dens ID (specifikt projekt)
    public Project findProjectByIDForStatus(Long id) {
        // SQL-forespørgsel til at hente projektet baseret på ID
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.queryForObject(sql, new ProjectRowMapper(), id); // Returner det første fundne projekt
    }

    // Opdaterer et projekts status
    public void updateProjectStatusByID(Long id, String status) {
        Project project = findProjectByIDForStatus(id); // Hent projektet baseret på ID
        if (project != null) {
            project.setStatus(status); // Opdater status på projektet
            // SQL-forespørgsel for at opdatere status på projektet i databasen
            String sql = "UPDATE projects SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id); // Udfør SQL opdatering
        } else {
            throw new RuntimeException("Project with ID " + id + " not found"); // Hvis projektet ikke findes, kast undtagelse
        }
    }

    // Opdaterer et projekt i databasen
    public void updateProject(int id, Project project) {
        // SQL-forespørgsel til at opdatere projekt med DATEDIFF
        String updateProjectSql = "UPDATE projects SET project_name = ?, duration = DATEDIFF(?, ?), planned_start_date = ?, planned_finish_date = ?, expected_time_in_total = ? WHERE id = ?";

        try {
            // Forsøg at opdatere projektet med den første DATEDIFF
            jdbcTemplate.update(updateProjectSql,
                    project.getProjectName(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getExpectedTimeInTotal(),
                    id);
        } catch (Exception e) {
            // Hvis den første SQL fejler, udfør alternativ forespørgsel med DATEDIFF(DAY, ?, ?)
            System.out.println("First SQL query failed, trying to execute alternate query.");

            String fallbackUpdateSql = "UPDATE projects SET project_name = ?, duration = DATEDIFF(DAY, ?, ?), planned_start_date = ?, planned_finish_date = ?, expected_time_in_total = ? WHERE id = ?";
            jdbcTemplate.update(fallbackUpdateSql,
                    project.getProjectName(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getExpectedTimeInTotal(),
                    id);
        }
    }

    // Sletter et projekt i databasen
    public void deleteProject(int id) {
        // SQL-forespørgsel til at slette projektet fra databasen
        String sql = "DELETE FROM projects WHERE id = ?";
        jdbcTemplate.update(sql, id); // Udfør SQL opdatering

        // Opdater projekttabellen efter sletning
        updateProjectsTable();
    }

    // Opdaterer IDs i projects tabellen
    public void updateProjectsTable() {
        // SQL-forespørgsel til at hente alle projekter sorteret efter ID
        String sql = "SELECT * FROM projects ORDER BY id";
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(sql);

        int newId = 1;
        // Opdater ID og WBS for hvert projekt
        for (Map<String, Object> project : projects) {
            int originalID = (int) project.get("id");

            // SQL-forespørgsel til at opdatere ID og WBS
            String updateSql = "UPDATE projects SET id = ?, WBS = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, newId, originalID);

            newId++;
        }

        // Opdater WBS for alle tasks
        String tasksSql = "SELECT id, wbs FROM tasks";
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(tasksSql);

        for (Map<String, Object> task : tasks) {
            int taskId = (int) task.get("id");
            String originalWBS = (String) task.get("wbs");

            // Hvis WBS findes, opdater den
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\.");
                int firstDigit = Integer.parseInt(wbsParts[0]);
                if (firstDigit != 1) {
                    wbsParts[0] = String.valueOf(firstDigit - 1);
                    String updatedWBS = String.join(".", wbsParts);

                    // SQL-forespørgsel for at opdatere WBS på task
                    String updateTasksWbsSql = "UPDATE tasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateTasksWbsSql, updatedWBS, taskId);
                }
            }
        }

        // Opdater WBS for alle subtasks
        String subtasksSql = "SELECT id, wbs FROM subtasks";
        List<Map<String, Object>> subtasks = jdbcTemplate.queryForList(subtasksSql);

        for (Map<String, Object> subtask : subtasks) {
            int subtaskId = (int) subtask.get("id");
            String originalWBS = (String) subtask.get("wbs");

            // Hvis WBS findes, opdater den
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\.");
                int firstDigit = Integer.parseInt(wbsParts[0]);
                if (firstDigit != 1) {
                    wbsParts[0] = String.valueOf(firstDigit - 1);
                    String updatedWBS = String.join(".", wbsParts);

                    // SQL-forespørgsel for at opdatere WBS på subtask
                    String updateSubtasksWbsSql = "UPDATE subtasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateSubtasksWbsSql, updatedWBS, subtaskId);
                }
            }
        }
    }

    // Finder projekter, der ikke er markeret som 'done'
    public List<Project> findAllProjects() {
        // SQL-forespørgsel til at hente projekter, der ikke er færdige ('done')
        String sql = "SELECT * FROM projects WHERE status != 'done'";
        return jdbcTemplate.query(sql, new ProjectRowMapper()); // Returner projekterne som en liste
    }

    // Henter det højeste WBS-indeks fra projekter, der matcher hovedprojektets WBS
    public int getHighestWbsIndexFromProjects(String mainProjectWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks for projekter, der matcher hovedprojektets WBS
        String sqlMySQL = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";
        String sqlH2 = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS INT)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";

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

    // Mapper resultatet af SQL-spørgsmål til et Project-objekt
    private static class ProjectRowMapper implements RowMapper<Project> {
        @Override
        public Project mapRow(ResultSet rs, int rowNum) throws SQLException {
            Project project = new Project();
            project.setId(rs.getInt("id"));
            project.setWbs(rs.getString("WBS"));
            project.setProjectName(rs.getString("project_name"));
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
}