package top.topcalculations.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

        // Beregn varighed og formater som "x Dag(e)"
        LocalDate startDate = LocalDate.parse(project.getPlannedStartDate());
        LocalDate endDate = LocalDate.parse(project.getPlannedFinishDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        String duration = daysBetween == 1 ? "1 Dag" : daysBetween + " Dage";
        project.setDuration(duration); // Sæt den beregnede varighed i projektet

        System.out.println("Saving project: " + project);

        String sql = "INSERT INTO projects (WBS, project_name, duration, planned_start_date, planned_finish_date, assigned, expected_time_in_total) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, project.getWbs(), project.getProjectTaskName(), project.getDuration(), project.getPlannedStartDate(), project.getPlannedFinishDate(), project.getAssigned(), project.getExpectedTimeInTotal());
    }

    public void saveTask(Project task) {
        LocalDate startDate = LocalDate.parse(task.getPlannedStartDate());
        LocalDate endDate = LocalDate.parse(task.getPlannedFinishDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        task.setDuration(String.valueOf(daysBetween)); // Set the numeric duration (as a String for now)

        System.out.println("Saving task: " + task);

        String sql = "INSERT INTO tasks (WBS, project_name, task_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, task.getWbs(), task.getTaskProjectName(), task.getProjectTaskName(), task.getDuration(), task.getPlannedStartDate(), task.getPlannedFinishDate(), task.getAssigned());

        addTimeToSpendTask(task);
    }

    // Gemmer en delopgave (subtask) i databasen
    public void saveSubTask(Project subTask) {
        LocalDate startDate = LocalDate.parse(subTask.getPlannedStartDate());
        LocalDate endDate = LocalDate.parse(subTask.getPlannedFinishDate());
        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate);
        subTask.setDuration(String.valueOf(daysBetween)); // Set the numeric duration (as a String for now)

        System.out.println("Saving task: " + subTask);

        String sql = "INSERT INTO subtasks (WBS, task_name, sub_task_name, project_name, duration, planned_start_date, planned_finish_date, assigned) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, subTask.getWbs(), subTask.getTaskProjectName(), subTask.getSubTaskName(), subTask.getProjectTaskName(), subTask.getDuration(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate(), subTask.getAssigned());

        addTimeToSpendSubTask(subTask);
    }

    public void addTimeToSpendTask(Project task) {
        // Ekstraherer den grundlæggende WBS for opgaven (f.eks. "x.x" fra "x.x.x")
        String baseWbs = task.getWbs().split("\\.")[0];

        // SQL-forespørgsel for at hente den samlede forventede tid og antallet af opgaver for den grundlæggende WBS
        String sqlForExpectedTimeAndTaskCount = "SELECT MAX(p.expected_time_in_total) AS expected_time_in_total, COUNT(ta.id) AS task_count " +
                "FROM projects p " +
                "LEFT JOIN tasks ta ON ta.wbs LIKE CONCAT(p.wbs, '.%') " +
                "WHERE p.wbs = ? " +
                "GROUP BY p.wbs";

        try {
            // Udfør SQL-forespørgslen for at hente den samlede forventede tid og antallet af opgaver for den grundlæggende WBS
            Map<String, Object> result = jdbcTemplate.queryForMap(sqlForExpectedTimeAndTaskCount, baseWbs);

            // Hent den samlede forventede tid og task_count fra resultatet
            Number expectedTimeInTotal = (Number) result.get("expected_time_in_total");
            Long taskCount = (Long) result.get("task_count");

            // Hvis der ikke findes opgaver eller forventet tid, afslut metoden tidligt
            if (taskCount == 0 || expectedTimeInTotal == null) {
                return;
            }

            // Beregn den tid, der skal bruges pr. opgave
            double expectedTime = expectedTimeInTotal.doubleValue();
            double timeToSpend = expectedTime / taskCount;

            // SQL-forespørgsel for at opdatere time_to_spend for hver opgave
            String sqlUpdateTimeToSpend = "UPDATE tasks t " +
                    "SET t.time_to_spend = ? " +
                    "WHERE t.wbs LIKE CONCAT(?, '.%')";

            // Opdater time_to_spend for alle opgaver
            jdbcTemplate.update(sqlUpdateTimeToSpend, timeToSpend, baseWbs);
        } catch (EmptyResultDataAccessException e) {
            // Håndter tilfælde, hvor der ikke findes et projekt med den angivne WBS
            System.out.println("Ingen projekt fundet med wbs: " + task.getWbs());
        }
    }

    public void addTimeToSpendSubTask(Project subTask) {
        // Ekstraherer den grundlæggende WBS for opgaven (f.eks. "x.x" fra "x.x.x")
        String[] wbsParts = subTask.getWbs().split("\\.");
        String baseWbs = wbsParts[0] + "." + wbsParts[1];  // Sørg for at bruge "x.x" fra "x.x.x"

        // SQL-forespørgsel for at få MAX time_to_spend fra hovedopgaven og tælle antallet af delopgaver
        String sqlForTimeToSpendAndSubtaskCount = "SELECT MAX(t.time_to_spend) AS time_to_spend, COUNT(sta.id) AS subtask_count " +
                "FROM tasks t " +
                "LEFT JOIN subtasks sta ON sta.wbs LIKE CONCAT(t.wbs, '.%') " +
                "WHERE t.wbs = ? " +
                "GROUP BY t.wbs";

        try {
            // Udfør SQL-forespørgslen for at hente time_to_spend og antallet af delopgaver for den grundlæggende WBS
            Map<String, Object> result = jdbcTemplate.queryForMap(sqlForTimeToSpendAndSubtaskCount, baseWbs);

            // Hent time_to_spend og subtask_count fra resultatet
            Number timeToSpend = (Number) result.get("time_to_spend");
            Long subTaskCount = (Long) result.get("subtask_count");

            // Hvis der ikke findes delopgaver eller time_to_spend, afslut metoden tidligt
            if (subTaskCount == 0 || timeToSpend == null) {
                return;
            }

            // Beregn den tid, der skal bruges pr. delopgave
            double expectedDuration = timeToSpend.doubleValue();
            double spendPerSubtask = expectedDuration / subTaskCount;

            // SQL-forespørgsel for at opdatere time_to_spend for hver delopgave
            String sqlUpdateTimeToSpend = "UPDATE subtasks st " +
                    "SET st.time_to_spend = ? " +
                    "WHERE st.wbs LIKE CONCAT(?, '.%')";

            // Opdater time_to_spend for alle delopgaver
            jdbcTemplate.update(sqlUpdateTimeToSpend, spendPerSubtask, baseWbs);
        } catch (EmptyResultDataAccessException e) {
            // Håndter tilfælde, hvor ingen opgave findes med den angivne WBS
            System.out.println("Ingen opgave fundet med wbs: " + subTask.getWbs());
        }
    }

    // Genererer en ny WBS ved at finde den højeste WBS-værdi og øge den
    private String generateNewWBS() {
        String sql = "SELECT MAX(CAST(WBS AS UNSIGNED)) FROM projects";
        Integer highestWBS = jdbcTemplate.queryForObject(sql, Integer.class);

        int newWBSValue = (highestWBS == null) ? 1 : highestWBS + 1;  // Start med 1, hvis ingen WBS findes
        return String.valueOf(newWBSValue);  // Returner den nye WBS
    }

    // Finder alle opgaver tilknyttet et hovedprojekt
    public List<Project> findTasks(String mainProjectName) {
        String sql = "SELECT * FROM tasks WHERE task_name = ?";
        return jdbcTemplate.query(sql, new Object[]{mainProjectName}, new ProjectRowMapper());
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

        // Opdater projectTaskName, hvis det er nødvendigt
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

        return projects;  // Returner alle projekter
    }

    // Henter alle projekter fra databasen
    public List<Project> getAllTasks() {
        String sql = "SELECT * FROM tasks";
        List<Project> tasks = jdbcTemplate.query(sql, new TaskRowMapper());

        // Opdater taskName, hvis det er nødvendigt
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

        return tasks; // Returner alle projekter
    }

    // Henter alle projekter fra databasen
    public List<Project> getAllSubTasks() {
        String sql = "SELECT * FROM subtasks";
        List<Project> tasks = jdbcTemplate.query(sql, new TaskRowMapper());

        // Opdater taskName, hvis det er nødvendigt
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

        return tasks; // Returner alle projekter
    }

    // Finder alle opgaver fra databasen
    public List<Project> findAllTasks() {
        String sql = "SELECT * FROM tasks WHERE task_name IS NOT NULL AND task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper());
    }

    // Henter en opgave baseret på dens ID
    public List<Project> findTaskByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.task_name, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.sub_task_name, p.time_spent " +
                "FROM tasks p " +
                "WHERE p.id = ? AND p.task_name IS NOT NULL AND p.task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Henter en opgave baseret på dens ID
    public List<Project> findSubTaskByID(Long id) {
        String sql = "SELECT p.id, p.wbs, p.sub_task_name, p.duration, p.planned_start_date, p.planned_finish_date, p.assigned, p.task_name, p.time_spent " +
                "FROM projects p " +
                "WHERE p.id = ? AND p.sub_task_name IS NOT NULL AND p.sub_task_name != ''";
        return jdbcTemplate.query(sql, new TaskRowMapper(), id);
    }

    // Opdaterer en opgave i databasen
    public void updateTask(int id, Project project) {
        String sql = "UPDATE tasks SET task_name = ?, duration = ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                project.getTaskProjectName(),
                project.getDuration(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                id);  // Opdater opgave med nye data
    }

    // Opdaterer en underopgave i databasen
    public void updateSubTask(int id, Project project) {
        String sql = "UPDATE subtasks SET sub_task_name = ?, duration = ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                project.getSubTaskName(),
                project.getDuration(),
                project.getPlannedStartDate(),
                project.getPlannedFinishDate(),
                id);  // Opdater underopgave med nye data
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
            project.setTaskProjectName(rs.getString("task_name"));
            project.setSubTaskName(rs.getString("sub_task_name"));
            project.setTimeSpent(rs.getInt("time_spent"));
            project.setExpectedTimeInTotal(rs.getDouble("expected_time_in_total"));
            project.setDuration(rs.getString("duration"));
            project.setPlannedStartDate(rs.getString("planned_start_date"));
            project.setPlannedFinishDate(rs.getString("planned_finish_date"));
            project.setTimeToSpend(rs.getDouble("time_to_spend"));
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
            task.setSubTaskName(rs.getString("sub_task_name"));
            task.setTimeSpent(rs.getInt("time_spent"));
            task.setDuration(rs.getString("duration"));
            task.setTimeToSpend(rs.getDouble("time_to_spend"));
            task.setPlannedStartDate(rs.getString("planned_start_date"));
            task.setPlannedFinishDate(rs.getString("planned_finish_date"));
            task.setAssigned(rs.getString("assigned"));
            return task;  // Returner det mapperede Task-objekt
        }
    }
}
