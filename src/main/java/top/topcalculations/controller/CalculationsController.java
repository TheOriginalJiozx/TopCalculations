package top.topcalculations.controller;

import top.topcalculations.model.Calculations;
import top.topcalculations.repository.CalculationsRepository;
import top.topcalculations.repository.UserRepository;
import top.topcalculations.service.CalculationsService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/")
public class CalculationsController {
    private final CalculationsService calculationsService;
    private final UserRepository userRepository;

    public CalculationsController(CalculationsService calculationsService, JdbcTemplate jdbcTemplate, CalculationsRepository calculationsRepository, UserRepository userRepository) {
        this.calculationsService = calculationsService;
        this.userRepository = userRepository;
    }

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName()))
                ? auth.getName() : "Guest";
    }

    @GetMapping("/")
    public String showIndexPage(Model model) {
        model.addAttribute("username", getAuthenticatedUsername());
        return "index";
    }

    @GetMapping("/calculations/view")
    public String viewCalculations(@RequestParam(required = false) String user, Model model) {
        String authenticatedUsername = getAuthenticatedUsername();

        if (user == null || user.isEmpty()) {
            return "redirect:/calculations/view?=user" + authenticatedUsername;
        }

        model.addAttribute("username", authenticatedUsername);
        model.addAttribute("wishListOwner", user);

        Long userId = userRepository.getUserIdByUsername(user);
        if (userId == null) {
            return "redirect:/calculations/view?user=" + authenticatedUsername;
        }

        model.addAttribute("calculationsUserId", userId);

        List<Calculations> calculations = calculationsService.getCalculations();
        model.addAttribute("calculations", calculations);

        return "view";
    }

    @PostMapping("/calculations/add")
    public ModelAndView addCalculations(@RequestParam Long calculationId, String calculationData) {
        Long userId = calculationsService.getCurrentUserId();
        ModelAndView modelAndView = new ModelAndView("project");

        String authenticatedUsername = getAuthenticatedUsername();
        modelAndView.addObject("username", authenticatedUsername);

        if (userId == null) {
            modelAndView.addObject("message", "Error: User not authenticated.");
            return modelAndView;
        }

        Calculations calculations = new Calculations();
        calculations.setId(calculationId);
        calculations.setUserId(userId);
        calculations.setCalculationData(calculationData);

        calculationsService.saveCalculation(calculations);

        modelAndView.addObject("message", "Calculation added successfully.");
        return modelAndView;
    }
}
