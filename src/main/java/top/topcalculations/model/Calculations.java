package top.topcalculations.model;

public class Calculations {
    private Long id;
    private Long userId;
    private String title;
    private String calculationData;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setCalculationData(String calculationData) {
        this.calculationData = calculationData;
    }

    public String getCalculationData() {
        return calculationData;
    }
}
