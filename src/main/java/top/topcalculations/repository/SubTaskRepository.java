
package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
public class SubTaskRepository {
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private ProjectRepository projectRepository;

    public SubTaskRepository(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    // Gemmer en delopgave i databasen
    public void saveSubTask(Subtask subTask, Task task) {

        // Udskriver hvilken delopgave der gemmes
        System.out.println("Saving subtask: " + subTask);

        // Sammenkæd task_name og sub_task_name for at danne taskname_sub_task_name
        String fullSubTaskName = task.getTaskName() + "_" + subTask.getSubTaskName();

        // Første SQL-spørgsmål for at indsætte en delopgave i subtasks-tabellen med DATEDIFF(?, ?)
        String sql = "INSERT INTO subtasks (WBS, task_name, sub_task_name, project_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                "VALUES (?, ?, ?, (SELECT project_name FROM tasks WHERE task_name = ?), ?, ?, DATEDIFF(?, ?), ?, ?)";

        try {
            // Udfører SQL-spørgsmålet og gemmer delopgaven i databasen
            jdbcTemplate.update(sql, subTask.getWbs(), task.getTaskName(), fullSubTaskName, subTask.getTaskName(),
                    subTask.getTimeToSpend(), subTask.getAssigned(), subTask.getPlannedFinishDate(), subTask.getPlannedStartDate(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate());
        } catch (Exception e) {
            // Hvis den første SQL fejler, udfør alternativ forespørgsel med DATEDIFF(DAY, ?, ?)
            System.out.println("First SQL query failed, trying to execute alternate query.");

            // Anden SQL-spørgsmål for at indsætte en delopgave med DATEDIFF(DAY, ?, ?)
            String fallbackSql = "INSERT INTO subtasks (WBS, task_name, sub_task_name, project_name, time_to_spend, assigned, duration, planned_start_date, planned_finish_date) " +
                    "VALUES (?, ?, ?, (SELECT prorject_name FROM tasks WHERE task_name = ?), ?, ?, DATEDIFF(DAY, ?, ?), ?, ?)";

            // Udfør den alternative SQL forespørgsel
            jdbcTemplate.update(fallbackSql, subTask.getWbs(), subTask.getTaskName(), fullSubTaskName, subTask.getTaskName(),
                    subTask.getTimeToSpend(), subTask.getAssigned(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate());
        }

        // SQL-spørgsmål for at finde ID'et for den gemte delopgave
        String findSubTaskIdQuery = "SELECT id FROM subtasks WHERE WBS = ? AND sub_task_name = ?";
        // Henter ID'et for delopgaven baseret på WBS og sub_task_name
        Long subTaskId = jdbcTemplate.queryForObject(findSubTaskIdQuery, Long.class, subTask.getWbs(), fullSubTaskName);

        // Hvis subTaskId ikke er gyldigt, kastes en undtagelse
        if (subTaskId == null || subTaskId <= 0) {
            throw new IllegalStateException("Failed to retrieve valid subtask ID after insert.");
        }

        // Udskriver det fundne subtask ID
        System.out.println("Subtask ID: " + subTaskId);

        // SQL-spørgsmål for at indsætte relationen mellem delopgaven og ressourcen i resources_subtasks-tabellen
        String sql2 = "INSERT INTO resources_subtasks (resource_name, sub_task_id) VALUES (?, ?)";
        // Gemmer ressourcen for delopgaven i databasen
        jdbcTemplate.update(sql2, subTask.getResource_name(), subTaskId);

        // Opdaterer den forventede tid for projektet baseret på delopgaven
        addExpectedTimeToProjectFromSubTask(subTask, task);
    }

    private void addExpectedTimeToProjectFromSubTask(Subtask subTask, Task task) {
        String sql = "UPDATE projects SET expected_time_in_total = expected_time_in_total + ? WHERE project_name = (SELECT t.project_name FROM tasks t WHERE t.task_name = ?)";
        jdbcTemplate.update(sql, subTask.getTimeToSpend(), subTask.getTaskName());

       projectRepository.updateTimeToSpendIfNecessary(task);
    }

    // Henter alle underopgaver fra databasen
    public List<Subtask> getAllSubTasks() {
        String sql = "SELECT *, status FROM subtasks";
        List<Subtask> subTasks = jdbcTemplate.query(sql, new SubTaskRowMapper());

        for (Subtask subTask : subTasks) {
            if (subTask.getTaskName() != null && !subTask.getTaskName().isEmpty() &&
                    (subTask.getSubTaskName() == null || subTask.getSubTaskName().isEmpty())) {
                subTask.setTaskName(subTask.getTaskName());
            } else {
                subTask.setProjectName(subTask.getSubTaskName() != null && !subTask.getSubTaskName().isEmpty()
                        ? subTask.getSubTaskName()
                        : subTask.getTaskName());
            }

            subTask.setStatus(subTask.getStatus());

        }

        return subTasks; // Returner alle projekter
    }

    // Henter en underopgave baseret på dens ID
    public List<Subtask> findSubTaskByID(Long id) {
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.assigned, st.task_name, st.project_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
                "FROM subtasks st " +
                "WHERE st.id = ? AND st.sub_task_name IS NOT NULL AND st.sub_task_name != ''";
        return jdbcTemplate.query(sql, new SubTaskRowMapper(), id);
    }

    // Henter en underopgave baseret på dens ID (specifik underopgave)
    public Subtask findSubTaskByIDForStatus(Long id) {
        String sql = "SELECT st.id, st.wbs, st.sub_task_name, st.project_name, st.assigned, st.task_name, st.time_spent, st.time_to_spend, st.duration, st.planned_start_date, st.planned_finish_date, st.status " +
                "FROM subtasks st " +
                "WHERE st.id = ? AND st.sub_task_name IS NOT NULL AND st.sub_task_name != ''";
        return jdbcTemplate.queryForObject(sql, new SubTaskRowMapper(), id);
    }

    // Opdaterer en underopgaves status
    public void updateSubTaskStatusByID(Long id, String status) {
        Subtask subTask = findSubTaskByIDForStatus(id);
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

    // Opdaterer en underopgave i databasen
    public void updateSubTask(int id, Subtask subTask, Project project, Task task) {
        String sql = "UPDATE subtasks SET sub_task_name = ?, time_spent = time_spent + ?, planned_start_date = ?, planned_finish_date = ? WHERE id = ?";
        jdbcTemplate.update(sql, subTask.getSubTaskName(), subTask.getTimeSpent(), subTask.getPlannedStartDate(), subTask.getPlannedFinishDate(), id);

        String updateTaskSql = "UPDATE tasks SET time_spent = time_spent + ? WHERE task_name = ?";
        jdbcTemplate.update(updateTaskSql, subTask.getTimeSpent(), task.getTaskName());

        String updateProjectSql = "UPDATE projects SET time_spent = time_spent + ? WHERE project_name = ?";
        jdbcTemplate.update(updateProjectSql, subTask.getTimeSpent(), subTask.getProjectName());

        System.out.println("Task name: " + task.getTaskName());
        System.out.println("Project name: " + subTask.getProjectName());

        String insertIntoTimeSpentSubTasks = "INSERT INTO time_spent_subtasks (days_date, time_spent, sub_task_name) VALUES (CURRENT_DATE, ?, ?)";
        jdbcTemplate.update(insertIntoTimeSpentSubTasks, subTask.getTimeSpent(), subTask.getSubTaskName());
    }

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

                // Opdater taskens time_spent (det skal altid opdateres)
                String updateTaskSql = "UPDATE tasks SET time_spent = time_spent - ? WHERE task_name = ?";
                jdbcTemplate.update(updateTaskSql, subTaskTimeSpent, taskName);
                System.out.println("Rows updated in tasks table for time_spent: " + updateTaskSql);

                // Update time_to_spend only if subTaskTimeToSpend is greater than the task's current time_to_spend
                if (subTaskTimeToSpend < taskTimeToSpend) {
                    String updateTaskTimeToSpendSql = "UPDATE tasks SET time_to_spend = time_to_spend - ? WHERE task_name = ?";
                    jdbcTemplate.update(updateTaskTimeToSpendSql, subTaskTimeToSpend, taskName);
                    System.out.println("Rows updated in tasks table for time_to_spend: " + updateTaskTimeToSpendSql);
                }

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
        // Hent alle underopgaver fra subtasks-tabellen, sorteret efter id
        String sql = "SELECT * FROM subtasks ORDER BY id";
        List<Map<String, Object>> subtasks = jdbcTemplate.queryForList(sql);

        int newId = 1;
        for (Map<String, Object> subtask : subtasks) {
            int originalID = (int) subtask.get("id");

            // Opdater id i subtasks-tabellen
            String updateSql = "UPDATE subtasks SET id = ? WHERE id = ?";
            jdbcTemplate.update(updateSql, newId, originalID);

            // Opdater sub_task_id i resources_subtasks-tabellen
            String updateSql2 = "UPDATE resources_subtasks SET sub_task_id = ? WHERE sub_task_id = ?";
            jdbcTemplate.update(updateSql2, newId, originalID);

            // Hent og opdater WBS (Work Breakdown Structure) i subtasks-tabellen
            String originalWBS = (String) subtask.get("wbs");
            if (originalWBS != null && !originalWBS.isEmpty()) {
                String[] wbsParts = originalWBS.split("\\."); // Opdel WBS i dele baseret på '.'
                int lastDigit = Integer.parseInt(wbsParts[wbsParts.length - 1]); // Hent sidste tal i WBS
                if (lastDigit != 1) {
                    wbsParts[wbsParts.length - 1] = String.valueOf(lastDigit - 1); // Reducer sidste tal med 1
                    String updatedWBS = String.join(".", wbsParts); // Saml WBS igen

                    // Opdater WBS i subtasks-tabellen
                    String updateWbsSql = "UPDATE subtasks SET wbs = ? WHERE id = ?";
                    jdbcTemplate.update(updateWbsSql, updatedWBS, newId);
                }
            }

            newId++; // Forøg newId for den næste underopgave
        }
    }

    public int getHighestWbsIndexFromSubTasks(String mainTaskWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra subtasks-tabellen
        String sqlMySQL = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM subtasks WHERE WBS LIKE CONCAT(?, '.%')";
        String sqlH2 = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS INT)) FROM subtasks WHERE WBS LIKE CONCAT(?, '.%')";

        try {
            // Forsøg at køre MySQL-specifik forespørgsel
            Integer highestSubtaskIndex = jdbcTemplate.queryForObject(sqlMySQL, new Object[]{mainTaskWBS, mainTaskWBS}, Integer.class);
            // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
            return highestSubtaskIndex != null ? highestSubtaskIndex : 0;
        } catch (Exception eMySQL) {
            // Hvis MySQL-forespørgslen fejler, forsøg H2-forespørgslen i stedet
            System.err.println("MySQL query failed, attempting H2 query: " + eMySQL.getMessage());
            eMySQL.printStackTrace();

            try {
                // Kør H2-specifik forespørgsel
                Integer highestSubtaskIndex = jdbcTemplate.queryForObject(sqlH2, new Object[]{mainTaskWBS, mainTaskWBS}, Integer.class);
                // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
                return highestSubtaskIndex != null ? highestSubtaskIndex : 0;
            } catch (Exception eH2) {
                // Hvis H2-forespørgslen også fejler, log fejlen og returner 0
                System.err.println("H2 query failed: " + eH2.getMessage());
                eH2.printStackTrace();

                // Returner 0, hvis begge forespørgsler fejler
                return 0; // Standard WBS-værdi
            }
        }
    }

    // Mapper resultatet af SQL-spørgsmål til et Subtask-objekt
    private static class SubTaskRowMapper implements RowMapper<Subtask> {
        @Override
        public Subtask mapRow(ResultSet rs, int rowNum) throws SQLException {
            Subtask subTask = new Subtask();
            subTask.setId(rs.getInt("id"));
            subTask.setWbs(rs.getString("WBS"));
            subTask.setTaskName(rs.getString("task_name"));
            subTask.setSubTaskName(rs.getString("sub_task_name"));
            subTask.setProjectName(rs.getString("project_name"));
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
