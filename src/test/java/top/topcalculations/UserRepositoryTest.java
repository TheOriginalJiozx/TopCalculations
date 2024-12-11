package top.topcalculations;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import top.topcalculations.model.User;
import top.topcalculations.service.UserService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class UserRepositoryTest {

    private UserService userService; // Mock for UserService
    private HttpSession session; // Mock for HttpSession
    private RedirectAttributes redirectAttributes; // Mock for RedirectAttributes
    private Model model; // Mock for Model

    @BeforeEach
    void setUp() {
        // Initialiser mock-objekter
        userService = mock(UserService.class);
        session = mock(HttpSession.class);
        redirectAttributes = mock(RedirectAttributes.class);
        model = mock(Model.class);
    }

    @Test
    void testCreateUser() {
        // Arrange: Opret en ny bruger
        User user = new User();
        user.setUsername("Salem");
        user.setPassword("1234");

        when(session.getAttribute("user")).thenReturn(null); // Simuler at ingen bruger er logget ind

        // Act: Hash brugerens password og kald signUp
        String hashedPassword = hashPassword(user.getPassword());
        user.setPassword(hashedPassword);

        // Simuler kaldet til signUp
        doAnswer(invocation -> {
            User argUser = invocation.getArgument(0);
            Model argModel = invocation.getArgument(1);

            // Bekræft, at de korrekte argumenter blev givet
            assertEquals("Salem", argUser.getUsername());
            assertEquals(hashedPassword, argUser.getPassword());
            return null; // Hvis metoden er void, returneres null
        }).when(userService).signUp(any(User.class), any(Model.class));

        // Kald signUp
        userService.signUp(user, model);

        // Simuler flash-besked
        redirectAttributes.addFlashAttribute("message", "User created successfully.");

        // Assert
        verify(userService).signUp(user, model); // Verificer at signUp blev kaldt korrekt
        verify(redirectAttributes).addFlashAttribute("message", "User created successfully."); // Verificer flash-beskeden

        // Bekræft, at password blev korrekt hashed
        assertEquals(hashPassword("1234"), user.getPassword());
    }

    // Hjælpefunktion til password hashing
    public String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Fejl under hashing af password", e);
        }
    }
}