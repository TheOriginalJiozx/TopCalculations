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
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;  // Mocking the ProjectService

    @Test
    public void testDeleteProject_UserNotLoggedIn() throws Exception {
        // Act & Assert: Simulate a request without a session user
        mockMvc.perform(post("/delete-project/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // Verify that the service method is never called
        verify(projectService, never()).deleteProject(anyInt());
    }

    @Test
    public void testDeleteProject_UserLoggedIn() throws Exception {
        // Arrange: Mock the session attribute for a logged-in user
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute("user")).thenReturn("loggedInUser");

        // Act & Assert: Simulate a request with a valid session
        mockMvc.perform(post("/delete-project/1").sessionAttr("user", "loggedInUser"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/view"));

        // Verify that the deleteProject method is called once with the correct ID
        verify(projectService, times(1)).deleteProject(1);
    }
}