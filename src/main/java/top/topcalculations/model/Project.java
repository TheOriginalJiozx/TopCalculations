package top.topcalculations.model;

public class Project {
    private int id;
    private String wbs;
    private String projectName;
    private String taskName;
    private int duration;
    private String plannedStartDate;
    private String plannedFinishDate;
    private String assigned;
    private double timeSpent;
    private Double expectedTimeInTotal;
    private Double timeToSpend;
    private String resource_name;
    private String status;

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTimeSpent(double timeSpent) {
        this.timeSpent = timeSpent;
    }

    public double getTimeSpent() {
        return timeSpent;
    }

    public void setExpectedTimeInTotal(Double expectedTimeInTotal) {
        this.expectedTimeInTotal = expectedTimeInTotal;
    }

    public Double getExpectedTimeInTotal() {
        return expectedTimeInTotal;
    }

    public void setTimeToSpend(Double timeToSpend) {
        this.timeToSpend = timeToSpend;
    }

    public Double getTimeToSpend() {
        return timeToSpend;
    }

    public String getWbs() {
        return wbs;
    }

    public void setWbs(String wbs) {
        this.wbs = wbs;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getPlannedStartDate() {
        return plannedStartDate;
    }

    public void setPlannedStartDate(String plannedStartDate) {
        this.plannedStartDate = plannedStartDate;
    }

    public String getPlannedFinishDate() {
        return plannedFinishDate;
    }

    public void setPlannedFinishDate(String plannedFinishDate) {
        this.plannedFinishDate = plannedFinishDate;
    }

    public String getAssigned() {
        return assigned;
    }

    public void setAssigned(String assigned) {
        this.assigned = assigned;
    }

    public void setResource_name(String resource_name) {
        this.resource_name = resource_name;
    }

    public String getResource_name() {
        return resource_name;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}