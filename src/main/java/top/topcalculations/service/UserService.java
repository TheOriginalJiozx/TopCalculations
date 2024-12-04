package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import top.topcalculations.model.User;
import top.topcalculations.repository.UserRepository;

@Service
public class UserService {

    // Injicerer UserRepository for at kunne bruge metoderne til at interagere med databasen
    @Autowired
    private UserRepository userRepository;

    // Injicerer PasswordEncoder for at kunne kode adgangskoder
    @Autowired
    private PasswordEncoder passwordEncoder;

    // Metode til at registrere en bruger
    public String signUp(User user, Model model) {
        // Kalder signUp metoden fra UserRepository og returnerer resultatet
        return userRepository.signUp(user, model, passwordEncoder);
    }

    // Metode til at hente ID'et af den nuværende bruger
    public Long getCurrentUserId() {
        // Henter autentificeringen fra SecurityContext
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Hvis brugeren er autentificeret, hentes deres information
        if (authentication != null && authentication.isAuthenticated()) {
            // Henter brugeren fra databasen ved hjælp af brugernavnet (authentication.getName())
            User user = userRepository.findByUsername(authentication.getName());
            return user != null ? user.getId() : null;  // Returnerer brugerens ID eller null, hvis brugeren ikke findes
        }
        return null;  // Returnerer null, hvis brugeren ikke er autentificeret
    }
}
