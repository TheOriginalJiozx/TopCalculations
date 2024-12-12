
package top.topcalculations.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import top.topcalculations.model.Project;

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

    private void addExpectedTimeToProjectFromSubTask(Project subTask) {
        String sql = "UPDATE projects SET expected_time_in_total = expected_time_in_total + ? WHERE project_name = (SELECT t.project_name FROM tasks t WHERE t.task_name = ?)";
        jdbcTemplate.update(sql, subTask.getTimeToSpend(), subTask.getTaskProjectName());

       projectRepository.updateTimeToSpendIfNecessary(subTask);
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

    public int getHighestWbsIndexFromSubTasks(String mainTaskWBS) {
        // SQL-forespørgsel for at finde det højeste WBS-indeks fra opgaver-tabellen
        String sql = "SELECT MAX(CAST(SUBSTRING(WBS, LENGTH(?) + 2) AS UNSIGNED)) FROM subtasks WHERE WBS LIKE CONCAT(?, '.%')";
        // Udfør forespørgslen og få resultatet som en Integer
        Integer highestSubtaskIndex = jdbcTemplate.queryForObject(sql, new Object[]{mainTaskWBS, mainTaskWBS}, Integer.class);
        // Returner det højeste WBS-indeks eller 0, hvis resultatet er null
        return highestSubtaskIndex != null ? highestSubtaskIndex : 0;
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
