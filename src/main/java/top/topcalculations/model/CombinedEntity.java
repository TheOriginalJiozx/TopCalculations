package top.topcalculations.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CombinedEntity {
    private String wbs;
    private String name;
    private int duration;
    private String plannedStartDate;
    private String plannedFinishDate;
    private String assigned;
    private double timeToSpend;
    private String status;

    public CombinedEntity(String wbs, String name, int duration, String plannedStartDate,
                          String plannedFinishDate, String assigned, double timeToSpend, String status) {
        this.wbs = wbs;
        this.name = name;
        this.duration = duration;
        this.plannedStartDate = plannedStartDate;
        this.plannedFinishDate = plannedFinishDate;
        this.assigned = assigned;
        this.timeToSpend = timeToSpend;
        this.status = status;
    }

    public List<Integer> getWbsParts() {
        return Arrays.stream(wbs.split("\\."))
                .map(Integer::parseInt)
                .collect(Collectors.toList());
    }

    // Getters and setters
    public String getWbs() { return wbs; }
    public void setWbs(String wbs) { this.wbs = wbs; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(String plannedStartDate) { this.plannedStartDate = plannedStartDate; }
    public String getPlannedFinishDate() { return plannedFinishDate; }
    public void setPlannedFinishDate(String plannedFinishDate) { this.plannedFinishDate = plannedFinishDate; }
    public String getAssigned() { return assigned; }
    public void setAssigned(String assigned) { this.assigned = assigned; }
    public double getTimeToSpend() { return timeToSpend; }
    public void setTimeToSpend(double timeToSpend) { this.timeToSpend = timeToSpend; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
