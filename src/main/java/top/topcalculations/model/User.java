package top.topcalculations.model;

public class User {
    private Long id;
    private String username;
    private String password;
    private boolean enabled;
    private String role;
    private String projects;
    private String tasks;
    private String subTasks;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setProjects(String projects) {
        this.projects = projects;
    }

    public String getProjects() {
        return projects;
    }

    public void setTasks(String tasks) {
        this.tasks = tasks;
    }

    public String getTasks() {
        return tasks;
    }

    public void setSubTasks(String subTasks) {
        this.subTasks = subTasks;
    }

    public String getSubTasks() {
        return subTasks;
    }
}
