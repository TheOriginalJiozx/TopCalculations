package top.topcalculations;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import top.topcalculations.service.ProjectService;

import jakarta.servlet.http.HttpSession;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ProjectControllerIntegrationsTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc til at simulere HTTP-anmodninger

    @MockBean
    private ProjectService projectService;  // Mock af ProjectService

    @Test
    public void testDeleteProject_UserNotLoggedIn() throws Exception {
        // Act & Assert: Simuler en anmodning uden en bruger i sessionen
        mockMvc.perform(post("/delete-project/1"))
                .andExpect(status().is3xxRedirection())  // Bekræft at status er en omdirigering
                .andExpect(redirectedUrl("/login"));  // Bekræft at brugeren bliver omdirigeret til login-siden

        // Verificer at service-metoden aldrig blev kaldt
        verify(projectService, never()).deleteProject(anyInt());
    }

    @Test
    public void testDeleteProject_UserLoggedIn() throws Exception {
        // Arrange: Mock session-attributten for en logget-in bruger
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn("loggedInUser");  // Simuler at brugeren er logget ind

        // Act & Assert: Simuler en anmodning med en gyldig session
        mockMvc.perform(post("/delete-project/1").sessionAttr("user", "loggedInUser"))
                .andExpect(status().is3xxRedirection())  // Bekræft at status er en omdirigering
                .andExpect(redirectedUrl("/view"));  // Bekræft at brugeren bliver omdirigeret til "view" siden

        // Verificer at deleteProject metoden blev kaldt én gang med den korrekte ID
        verify(projectService, times(1)).deleteProject(1);
    }
}