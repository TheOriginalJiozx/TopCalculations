package top.topcalculations.model;

public class Project {
    private int id;
    private String wbs;
    private String projectTaskName;
    private String mainProjectName;
    private String taskProjectName;
    private int duration;
    private String plannedStartDate;
    private String plannedFinishDate;
    private String assigned;
    private String subTaskName;
    private int timeSpent;
    private Double expectedTimeInTotal;
    private Double timeToSpend;
    private String resource_name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getProjectTaskName() {
        return projectTaskName;
    }

    public void setProjectTaskName(String projectTaskName) {
        this.projectTaskName = projectTaskName;
    }

    public String getSubTaskName() {
        return subTaskName;
    }

    public void setSubTaskName(String subTaskName) {
        this.subTaskName = subTaskName;
    }

    public void setTimeSpent(int timeSpent) {
        this.timeSpent = timeSpent;
    }

    public int getTimeSpent() {
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

    public String getMainProjectName() {
        return mainProjectName;
    }

    public void setMainProjectName(String projectName) {
        this.mainProjectName = projectName;
    }

    public String getTaskProjectName() {
        return taskProjectName;
    }

    public void setTaskProjectName(String taskProjectName) {
        this.taskProjectName = taskProjectName;
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
}