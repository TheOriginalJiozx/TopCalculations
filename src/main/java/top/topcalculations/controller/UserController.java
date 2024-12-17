package top.topcalculations.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import top.topcalculations.model.User;
import top.topcalculations.service.UserService;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;  // Bruges til at håndtere brugergodkendelse og -registrering

    // Konstruktør for at injicere UserService
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Håndterer GET-anmodning til login-siden
    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        // Tjekker om brugeren allerede er logget ind, hvis ja, omdirigeres til forsiden
        if (session.getAttribute("user") != null) {
            return "redirect:/";  // Omdiriger til forsiden hvis brugeren er logget ind
        }

        // Hent brugernavnet fra sessionen
        String username = userService.getAuthenticatedUsername(session);
        model.addAttribute("username", username);  // Tilføj brugernavn til modellen
        return "login";  // Returnerer login-siden
    }

    // Håndterer POST-anmodning til login-siden
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        // Autentificering af brugeren baseret på username og password
        User user = userService.authenticate(username, password);

        // Hvis brugeren er godkendt, opret en session og omdiriger til profil-siden
        if (user != null) {
            session.setAttribute("user", user);  // Sætter brugeren i sessionen
            return "redirect:profile";  // Omdiriger til profil-siden
        } else {
            // Hvis autentificeringen mislykkes, vis fejlmeddelelse
            model.addAttribute("error", "Invalid username or password. Please try again.");
            return "login";  // Bliv på login-siden
        }
    }

    // Håndterer GET-anmodning til signup-siden
    @GetMapping("/signup")
    public String showSignupForm(Model model, HttpSession session) {
        // Hvis brugeren allerede er logget ind, omdiriger til forsiden
        if (session.getAttribute("user") != null) {
            return "redirect:/";  // Omdiriger til forsiden hvis brugeren allerede er logget ind
        }

        model.addAttribute("username", userService.getAuthenticatedUsername(session));  // Viser brugernavnet fra sessionen
        model.addAttribute("user", new User());  // Opret en ny bruger og tilføj til modellen
        return "signup";  // Returnerer signup-siden
    }

    // Håndterer POST-anmodning til signup-siden
    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user, Model model) {
        return userService.signUp(user, model);  // Kalder signUp-metoden fra UserService for at oprette en bruger
    }

    // Håndterer POST-anmodning til logout
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();  // Invaliderer sessionen, hvilket logger brugeren ud
        return "redirect:/";  // Omdiriger til forsiden efter logout
    }

    // Håndterer GET-anmodning til admin-panel
    @GetMapping("/admin")
    public String getUsers(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        // Tjekker om brugeren er logget ind og om brugeren har admin-rolle
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            // Hvis brugeren er admin, vis admin-relateret indhold
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true);
            } else {
                model.addAttribute("isAdmin", false);
                return "redirect:/";  // Hvis brugeren ikke er admin, omdiriger til forsiden
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false);
            return "redirect:/";  // Hvis brugeren ikke er logget ind, omdiriger til forsiden
        }

        // Hent alle brugere fra UserService og tilføj dem til modellen
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        return "admin";  // Returner admin-siden
    }

    // Håndterer GET-anmodning til brugerprofil
    @GetMapping("/profile")
    public String getProfile(Model model, HttpSession session) {
        User user = (User) session.getAttribute("user");
        // Tjekker om brugeren er logget ind
        if (user != null) {
            model.addAttribute("username", user.getUsername());

            // Hvis brugeren er admin, tilføj isAdmin som true
            if ("Admin".equals(user.getRole())) {
                model.addAttribute("isAdmin", true); // isAdmin er true for admins
            } else {
                model.addAttribute("isAdmin", false);
            }
        } else {
            model.addAttribute("username", "Guest");
            model.addAttribute("isAdmin", false); // isAdmin er false for gæster
        }

        // Hvis brugeren ikke er logget ind, omdiriger til login-siden
        if (user == null) {
            return "redirect:/login";
        }

        // Tilføj brugerens projekter og opgaver til modellen
        model.addAttribute("username", user.getUsername());
        model.addAttribute("projects", userService.getProjectsForUser(user.getUsername()));
        model.addAttribute("tasks", userService.getTasksForUser(user.getUsername()));
        model.addAttribute("subtasks", userService.getSubTasksForUser(user.getUsername()));

        return "profile";  // Returner profil-siden
    }

    // Opdaterer anonymisering af en bruger
    @PostMapping("/anonymize-user/{id}/{anonymize}")
    public String updateTaskStatus(@PathVariable("id") Long id,
                                   @PathVariable("anonymize") String anonymous, HttpSession session) {
        // Tjekker om brugeren er logget ind
        if (session.getAttribute("user") == null) {
            return "redirect:/login";  // Omdiriger til login-siden hvis ikke logget ind
        }

        // Anonymiser brugerens data
        userService.updateAnonymization(id, anonymous);
        return "redirect:/admin";  // Omdiriger til admin-siden efter opdatering
    }
}
