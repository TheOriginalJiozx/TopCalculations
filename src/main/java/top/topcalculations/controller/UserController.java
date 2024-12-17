package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.topcalculations.model.*;
import top.topcalculations.service.UserService;

import java.util.ArrayList;
import java.util.List;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Håndterer GET-anmodning til login-siden
    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        // Henter brugernavn fra sessionen
        if (session.getAttribute("user") != null) {
            return "redirect:/";  // Omdirigerer til forsiden, hvis brugeren allerede er logget ind
        }
        String username = userService.getAuthenticatedUsername(session);
        model.addAttribute("username", username);  // Tilføjer brugernavn til modellen
        return "login";  // Returnerer login-siden
    }

    // Håndterer POST-anmodning til login-siden
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        User user = userService.authenticate(username, password);

        if (user != null) {
            session.setAttribute("user", user);  // Sætter bruger i sessionen, hvis autentificering er succesfuld
            return "redirect:profile";  // Omdirigerer til forsiden
        } else {
            model.addAttribute("error", "Invalid username or password. Please try again.");  // Tilføjer fejlmeddelelse til modellen
            return "login";  // Forbliver på login-siden
        }
    }

    // Håndterer GET-anmodning til signup-siden
    @GetMapping("/signup")
    public String showSignupForm(Model model, HttpSession session) {
        if (session.getAttribute("user") != null) {
            return "redirect:/";  // Omdirigerer til forsiden, hvis brugeren allerede er logget ind
        }

        model.addAttribute("username", userService.getAuthenticatedUsername(session));  // Viser brugernavn fra sessionen
        model.addAttribute("user", new User());  // Tilføjer en ny bruger til modellen
        return "signup";  // Returnerer signup-siden
    }

    // Håndterer POST-anmodning til signup-siden
    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user, Model model) {
        return userService.signUp(user, model);  // Kalder signUp-metoden fra UserService
    }

    // Håndterer POST-anmodning til logout
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Invaliderer sessionen (logger brugeren ud)
        return "redirect:/";  // Omdirigerer til forsiden efter logout
    }

    // Håndterer GET-anmodning til admin panel
    @GetMapping("/admin")
    public String getUsers(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);
            } else {
                model.addAttribute("isAdmin", false);
                return "redirect:/";
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
            return "redirect:/";
        }

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin";
    }

    // Håndterer GET-anmodning til user profile
    @GetMapping("/profile")
    public String getProfile(Model model, HttpSession session) {
        // Hent brugerobjektet fra sessionen
        User user = (User) session.getAttribute("user");

        // Tjek om brugeren er logget ind
        if (user != null) {
            // Hvis brugeren er logget ind, tilføj brugerens navn og rolle (Admin eller ikke) til modellen
            model.addAttribute("username", user.getUsername());
            model.addAttribute("isAdmin", "Admin".equals(user.getRole()));
        } else {
            // Hvis brugeren ikke er logget ind, sættes brugernavnet til "Guest" og isAdmin til false
            // Redirect til login siden
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
            return "redirect:/login";
        }

        // Hent projekter, opgaver og subtasks for den pågældende bruger
        List<Project> projects = userService.getProjectsForUser(user.getUsername());
        List<Task> tasks = userService.getTasksForUser(user.getUsername());
        List<Subtask> subtasks = userService.getSubTasksForUser(user.getUsername());

        // Hvis der ikke er nogen projekter, opgaver eller subtasks, initialiser tomme lister
        if (projects == null) projects = new ArrayList<>();
        if (tasks == null) tasks = new ArrayList<>();
        if (subtasks == null) subtasks = new ArrayList<>();

        // Tilføj projekter, opgaver og subtasks til modellen
        model.addAttribute("projects", projects);
        model.addAttribute("tasks", tasks);
        model.addAttribute("subtasks", subtasks);

        // Beregn den samlede tid, der er tilbage at bruge (for projekter, opgaver og subtasks)
        double totalTimeToSpend = projects.stream()
                .filter(project -> !"done".equalsIgnoreCase(project.getStatus())) // Filter projekter, der ikke er færdige
                .mapToDouble(Project::getExpectedTimeInTotal) // Summér den forventede tid for projekterne
                .sum()
                + tasks.stream()
                .filter(task -> !"done".equalsIgnoreCase(task.getStatus())) // Filter opgaver, der ikke er færdige
                .mapToDouble(Task::getTimeToSpend) // Summér tiden for opgaverne
                .sum()
                + subtasks.stream()
                .filter(subtask -> !"done".equalsIgnoreCase(subtask.getStatus())) // Filter subtasks, der ikke er færdige
                .mapToDouble(Subtask::getTimeToSpend) // Summér tiden for subtasks
                .sum();

        // Tilføj den samlede tid til modellen
        model.addAttribute("totalTimeToSpend", totalTimeToSpend);

        // Returner viewet for profil siden
        return "profile";
    }

    // Opdaterer en tasks status
    @PostMapping("/anonymize-user/{id}/{anonymize}")
    public String updateTaskStatus(@PathVariable("id") Long id,
                                   @PathVariable("anonymize") String anonymous, HttpSession session) {
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Redirect til login-siden
        }

        userService.updateAnonymization(id, anonymous);
        return "redirect:/admin";  // Redirect tilbage til admin
    }
}