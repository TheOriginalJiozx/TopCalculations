package top.topcalculations.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import top.topcalculations.model.Calculations;
import top.topcalculations.model.User;
import top.topcalculations.repository.CalculationsRepository;
import top.topcalculations.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalculationsService {
    @Autowired
    private UserRepository userRepository;
    private CalculationsRepository calculationsRepository;

    public CalculationsService(UserRepository userRepository, CalculationsRepository calculationsRepository) {
        this.calculationsRepository = calculationsRepository;
        this.userRepository = userRepository;
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName());
            return user != null ? user.getId() : null;
        }
        return null;
    }

    public List<Calculations> getCalculations() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return List.of();
        }
        try {
            return calculationsRepository.findByUserId(userId);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<Calculations> getCalculationsForUser(String username) {

        User user = userRepository.findByUsername(username);

        if (user == null) {
            return List.of();
        }

        Long userId = user.getId();

        List<Calculations> calculations = calculationsRepository.findAll();
        return calculations.stream()
                .filter(calculation -> calculation.getUserId().equals(userId))
                .collect(Collectors.toList());
    }

    public void saveCalculation(Calculations calculations) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            calculations.setUserId(userId);
            calculationsRepository.save(calculations);
        }
    }

    public void updateCalculation(Calculations calculations) {
        Long userId = getCurrentUserId();
        if (userId != null && calculations.getUserId().equals(userId)) {
            calculationsRepository.update(calculations);
        }
    }

    public void deleteCalculation(Long calculationsId) {
        Long userId = getCurrentUserId();
        if (userId != null) {
            List<Calculations> calculationsList = calculationsRepository.findByUserId(userId);
            for (Calculations calculations : calculationsList) {
                if (calculations.getId().equals(calculationsId)) {
                    calculationsRepository.delete(calculationsId);
                    break;
                }
            }
        }
    }
}
