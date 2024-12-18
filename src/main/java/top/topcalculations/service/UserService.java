package top.topcalculations.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import top.topcalculations.model.Project;
import top.topcalculations.model.Subtask;
import top.topcalculations.model.Task;
import top.topcalculations.model.User;
import top.topcalculations.repository.UserRepository;

import java.util.List;

@Service
public class UserService {

    // Injektér UserRepository for at kunne tilgå brugerdata i databasen
    @Autowired
    private UserRepository userRepository;

    // Metode til at oprette en ny bruger
    public String signUp(User user, Model model) {
        return userRepository.signUp(user, model);  // Overlad håndtering af password til repository
    }

    // Metode til at finde alle oprettede brugere
    public List<User> getAllUsers() {
        return userRepository.getAllUsers();  // Fetch the list of users
    }

    // Metode til at autentificere en bruger ved login
    public User authenticate(String username, String password) {
        // Hent brugeren fra databasen ved hjælp af brugernavn
        User user = userRepository.findByUsername(username);

        if (user != null) {
            // Sammenlign den indtastede password (efter hashing) med den lagrede hashed password
            String hashedEnteredPassword = hashPassword(password);  // Brug samme hash-logik som ved sign up
            if (hashedEnteredPassword.equals(user.getPassword())) {
                // Opdater last_login-feltet til den nuværende dato
                userRepository.updateLastLogin(user.getUsername());  // Kalder en metode i UserRepository
                return user;  // Autentificering er vellykket
            }
        }
        return null; // Autentificering fejlede
    }

    // Enkel metode til at hash password (for login-sammenligning)
    private String hashPassword(String password) {
        // Genbrug samme hash-logik (MD5, SHA-256, osv.)
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));  // Konverter byte-array til hex-streng
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);  // Håndter fejl ved hashing
        }
    }

    public String getAuthenticatedUsername(HttpSession session) {
        // Hent brugernavnet fra sessionen
        User user = (User) session.getAttribute("user");  // Hent brugerobjektet fra sessionen
        if (user != null) {
            return user.getUsername();  // Hvis brugeren eksisterer, returner deres brugernavn
        } else {
            return "Guest";  // Hvis ingen bruger findes, returner "Guest"
        }
    }

    public List<Project> getProjectsForUser(String username) {
        return userRepository.getProjectsForUser(username);
    }

    public List<Task> getTasksForUser(String username) {
        return userRepository.getTasksForUser(username);
    }

    public List<Subtask> getSubTasksForUser(String username) {
        return userRepository.getSubTasksForUser(username);
    }

    // Opdaterer en opgaves status i databasen
    public void updateAnonymization(Long id, String anonymous) {
        userRepository.updateAnonymizationByUserID(id, anonymous);
    }
}