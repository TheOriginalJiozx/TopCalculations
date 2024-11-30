package top.topcalculations.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import top.topcalculations.model.User;
import top.topcalculations.service.UserService;

@Controller
public class UserController {
    private final UserService userService; // Service til at håndtere brugere

    // Konstruktor til at injectere dependencies
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Vist loginformularen
    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("username", getAuthenticatedUsername()); // Tilføjer brugernavnet til modellen
        return "login"; // Returnerer login-siden
    }

    // Håndterer login-formularens indsendelse
    @PostMapping("/login")
    public String doLogin(@RequestParam String username, @RequestParam String password) {
        return "redirect:/"; // Omdirigerer til startsiden efter login
    }

    // Hjælpefunktion til at hente det autentificerede brugernavn
    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // Hvis brugeren er autentificeret, returneres brugernavnet, ellers returneres "Guest"
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    // Vist signupformularen
    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("username", getAuthenticatedUsername()); // Tilføjer brugernavnet til modellen
        model.addAttribute("user", new User()); // Tilføjer en ny tom User til modellen
        return "signup"; // Returnerer signup-siden
    }

    // Håndterer signup-formularens indsendelse og registrerer brugeren
    @PostMapping("/signup")
    public String registerUser(@ModelAttribute User user, Model model) {
        userService.signUp(user, model); // Kalder signUp-metoden fra UserService til at oprette brugeren
        return "redirect:/login"; // Omdirigerer til login-siden efter registrering
    }
}