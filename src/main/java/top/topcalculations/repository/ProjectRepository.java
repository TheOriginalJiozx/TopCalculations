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
public class ProjectRepository {
    private JdbcTemplate jdbcTemplate;

    // Konstruktør, der initialiserer JdbcTemplate med en DataSource
    public ProjectRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Gemmer et projekt i databasen
    // Hvis opgaven ikke har et navn, genereres en ny WBS (Work Breakdown Structure)
    public void saveProject(Task task, Project project) {
        // Hvis projektet ikke har et navn, genereres en ny WBS
        if (task.getTaskName() == null || task.getTaskName().isEmpty()) {
            String newWBS = generateNewWBS();  // Generer en ny WBS
            project.setWbs(newWBS);  // Sæt den nye WBS for projektet
        }

        // SQL forespørgsel til at gemme projektet i databasen
        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) " +
                "VALUES (?, ?, DATEDIFF(?, ?), ?, ?, ?, 0)";

        try {
            // Forsøg at gemme projektet med den oprindelige SQL forespørgsel
            jdbcTemplate.update(sql,
                    project.getWbs(),
                    project.getProjectName(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getAssigned());
        } catch (Exception e) {
            // Hvis den første SQL forespørgsel fejler, brug en alternativ SQL
            System.out.println("Første SQL forespørgsel fejlede, prøver alternativ forespørgsel.");

            // Alternativ SQL forespørgsel
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

    // Opdaterer den tid, der skal bruges på en opgave, hvis det er nødvendigt
    // Ved at summere tiden brugt på subtasks
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

        // Beregn den samlede tid brugt på subtasks
        Double totalSubtaskTime = jdbcTemplate.queryForObject(calculateSubtaskTimeQuery, Double.class, name.getTaskName());
        if (totalSubtaskTime == null) {
            totalSubtaskTime = 0.0;
        }

        // Hent den oprindelige tid afsat til opgaven
        Double taskTimeToSpend = jdbcTemplate.queryForObject(getTaskTimeQuery, Double.class, name.getTaskName());
        if (taskTimeToSpend == null) {
            taskTimeToSpend = 0.0;
        }

        // Hvis den samlede tid brugt på subtasks er større end den oprindelige tid, opdater opgavens tid
        if (totalSubtaskTime > taskTimeToSpend) {
            jdbcTemplate.update(updateTaskTimeQuery, totalSubtaskTime, name.getTaskName());
        }
    }

    // Genererer en ny WBS (Work Breakdown Structure)
    // Ved at finde den højeste WBS i databasen og inkrementere den
    private String generateNewWBS() {
        // SQL forespørgsel for MySQL til at finde den højeste WBS
        String sqlMySQL = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        // SQL forespørgsel for H2 til at finde den højeste WBS
        String sqlH2 = "SELECT MAX(CAST(WBS AS INT)) FROM projects";

        try {
            // Forsøg at køre MySQL-forespørgslen
            Integer highestWBS = jdbcTemplate.queryForObject(sqlMySQL, Integer.class);
            // Hvis ingen WBS er fundet, start med 1, ellers inkrementer den højeste WBS
            int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
            return String.valueOf(newWBSValue);
        } catch (Exception eMySQL) {
            // Hvis MySQL-forespørgslen fejler, forsøg H2-forespørgslen
            System.err.println("MySQL-forespørgsel mislykkedes, forsøger H2-forespørgsel: " + eMySQL.getMessage());
            eMySQL.printStackTrace();

            try {
                // Kør H2-specifik forespørgsel
                Integer highestWBS = jdbcTemplate.queryForObject(sqlH2, Integer.class);
                int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;
                return String.valueOf(newWBSValue);
            } catch (Exception eH2) {
                // Hvis H2-forespørgslen fejler, log fejlen og returner en standardværdi
                System.err.println("H2-forespørgsel mislykkedes: " + eH2.getMessage());
                eH2.printStackTrace();
                return "1"; // Standard WBS-værdi
            }
        }
    }

    // Finder et projekt baseret på dets navn
    public Project findProjectByName(String projectName) {
        String sql = "SELECT * FROM projects WHERE project_name = ?";
        List<Project> projects = jdbcTemplate.query(sql, new Object[]{projectName}, new ProjectRowMapper());
        return projects.isEmpty() ? null : projects.get(0);  // Returner projektet, eller null hvis ikke fundet
    }

    // Henter alle projekter fra databasen
    public List<Project> getAllProjects() {
        String sql = "SELECT *, expected_time_in_total, status FROM projects";
        List<Project> projects = jdbcTemplate.query(sql, new ProjectRowMapper());

        // Behandl projekterne for at justere værdierne
        for (Project project : projects) {
            if (project.getProjectName() != null && !project.getProjectName().isEmpty() &&
                    (project.getTaskName() == null || project.getTaskName().isEmpty())) {
                project.setProjectName(project.getProjectName());
            } else {
                project.setProjectName(project.getTaskName() != null && !project.getTaskName().isEmpty()
                        ? project.getTaskName()
                        : project.getProjectName());
            }

            project.setTimeToSpend(project.getExpectedTimeInTotal());
            project.setStatus(project.getStatus());
        }

        return projects;
    }

    // Finder et projekt baseret på dets ID
    public List<Project> findProjectByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.query(sql, new ProjectRowMapper(), id);
    }

    // Finder et projekt baseret på dets ID og opdaterer dets status
    public Project findProjectByIDForStatus(Long id) {
        String sql = "SELECT p.id, p.wbs, p.project_name, p.time_spent, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.expected_time_in_total, status " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.project_name IS NOT NULL AND p.project_name != ''";
        return jdbcTemplate.queryForObject(sql, new ProjectRowMapper(), id);
    }

    // Opdaterer status for et projekt baseret på projektets ID
    public void updateProjectStatusByID(Long id, String status) {
        Project project = findProjectByIDForStatus(id);
        if (project != null) {
            project.setStatus(status);
            String sql = "UPDATE projects SET status = ? WHERE id = ?";
            jdbcTemplate.update(sql, status, id);
        } else {
            // Hvis projektet ikke findes, kast en undtagelse
            throw new RuntimeException("Projekt med ID " + id + " blev ikke fundet");
        }
    }




    // Opdaterer et projekt i databasen
    public void updateProject(int id, Project project) {
        // SQL forespørgsel til at opdatere projektet med DATEDIFF, hvor vi beregner forskellen mellem start- og slutdato
        String updateProjectSql = "UPDATE projects SET project_name = ?, duration = DATEDIFF(?, ?), planned_start_date = ?, planned_finish_date = ?, expected_time_in_total = ? WHERE id = ?";

        try {
            // Forsøg at opdatere projektet med den oprindelige SQL forespørgsel
            jdbcTemplate.update(updateProjectSql,
                    project.getProjectName(),
                    project.getPlannedFinishDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedStartDate(),
                    project.getPlannedFinishDate(),
                    project.getExpectedTimeInTotal(),
                    id);
        } catch (Exception e) {
            // Hvis den første SQL forespørgsel fejler, brug en alternativ SQL forespørgsel
            System.out.println("First SQL query failed, trying to execute alternate query.");

            // Alternativ SQL forespørgsel, der bruger DATEDIFF(DAY, ?, ?)
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
        // SQL forespørgsel for at slette et projekt baseret på dets ID
        String sql = "DELETE FROM projects WHERE id = ?";
        jdbcTemplate.update(sql, id);  // Udfør sletningen af projektet

        // Opdater tabellen efter sletning for at omorganisere projekterne
        updateProjectsTable();
    }

    // Opdaterer IDs i projects tabellen for at sikre, at de er i rækkefølge
    public void updateProjectsTable() {
        // Hent alle projekter sorteret efter ID
        String sql = "SELECT * FROM projects ORDER BY id";
        List<Map<String, Object>> projects = jdbcTemplate.queryForList(sql);

        int newId = 1;  // Start med ID 1 for at opdatere IDs i rækkefølge
        for (Map<String, Object> project : projects) {
            int originalID = (int) project.get("id");

            // Opdater projektets ID og WBS (Work Breakdown Structure)
            String updateSql = "UPDATE projects SET id = ?, WBS = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, newId, originalID);

            newId++;  // Øg ID'et for næste projekt
        }

        // Hent opgaverne og deres WBS for at opdatere dem
        String tasksSql = "SELECT id, wbs FROM tasks";
        List<Map<String, Object>> tasks = jdbcTemplate.queryForList(tasksSql);

        // Opdater WBS for hver opgave
        for (Map<String, Object> task : tasks) {
            int taskId = (int) task.get("id");
            String originalWBS = (String) task.get("wbs");

            // Hvis WBS ikke er tomt, opdater det
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\.");
                int firstDigit = Integer.parseInt(wbsParts[0]);
                if (firstDigit != 1) {
                    wbsParts[0] = String.valueOf(firstDigit - 1);  // Opdater første tal i WBS
                    String updatedWBS = String.join(".", wbsParts);

                    // Opdater opgavens WBS i databasen
                    String updateTasksWbsSql = "UPDATE tasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateTasksWbsSql, updatedWBS, taskId);
                }
            }
        }

        // Hent underopgaverne og deres WBS for at opdatere dem
        String subtasksSql = "SELECT id, wbs FROM subtasks";
        List<Map<String, Object>> subtasks = jdbcTemplate.queryForList(subtasksSql);

        // Opdater WBS for hver underopgave
        for (Map<String, Object> subtask : subtasks) {
            int subtaskId = (int) subtask.get("id");
            String originalWBS = (String) subtask.get("wbs");

            // Hvis WBS ikke er tomt, opdater det
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\.");
                int firstDigit = Integer.parseInt(wbsParts[0]);
                if (firstDigit != 1) {
                    wbsParts[0] = String.valueOf(firstDigit - 1);  // Opdater første tal i WBS
                    String updatedWBS = String.join(".", wbsParts);

                    // Opdater underopgavens WBS i databasen
                    String updateSubtasksWbsSql = "UPDATE subtasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateSubtasksWbsSql, updatedWBS, subtaskId);
                }
            }
        }
    }

    // Finder alle projekter, der ikke er afsluttet (status != 'done')
    public List<Project> findAllProjects() {
        // SQL forespørgsel for at finde projekter, der ikke er afsluttet
        String sql = "SELECT * FROM projects WHERE status != 'done'";
        return jdbcTemplate.query(sql, new ProjectRowMapper());  // Returner projekterne, der matcher betingelsen
    }

    // Finder det højeste WBS-indeks for et projekt baseret på hovedprojektets WBS
    public int getHighestWbsIndexFromProjects(String mainProjectWBS) {
        // SQL forespørgsel for MySQL til at finde det højeste WBS-indeks
        String sqlMySQL = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";
        String sqlH2 = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS INT)) FROM projects WHERE WBS LIKE CONCAT(?, '.%')";

        try {
            // Forsøg at køre MySQL forespørgslen
            Integer highestTaskIndex = jdbcTemplate.queryForObject(sqlMySQL, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
            return highestTaskIndex != null ? highestTaskIndex : 0;  // Returner det højeste indeks eller 0, hvis resultatet er null
        } catch (Exception eMySQL) {
            // Hvis MySQL forespørgslen fejler, prøv H2 forespørgslen
            System.err.println("MySQL query failed, attempting H2 query: " + eMySQL.getMessage());
            eMySQL.printStackTrace();

            try {
                // Kør H2 forespørgslen
                Integer highestTaskIndex = jdbcTemplate.queryForObject(sqlH2, new Object[]{mainProjectWBS, mainProjectWBS}, Integer.class);
                return highestTaskIndex != null ? highestTaskIndex : 0;  // Returner det højeste indeks eller 0, hvis resultatet er null
            } catch (Exception eH2) {
                // Hvis begge forespørgsler fejler, log fejlen og returner 0
                System.err.println("H2 query failed: " + eH2.getMessage());
                eH2.printStackTrace();
                return 0;  // Standard værdi, hvis begge forespørgsler fejler
            }
        }
    }

    // Mapper resultatet af SQL forespørgsler til et Project objekt
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
            return project;  // Returner det mapperede Project objekt
        }
    }
}