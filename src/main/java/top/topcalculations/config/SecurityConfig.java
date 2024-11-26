package top.topcalculations.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/header.html", "/footer.html", "/h2-console/**", "/css/**", "/wishlist/reserve", "/project").permitAll()
                        .requestMatchers("/login", "/signup").anonymous()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/project")
                        .ignoringRequestMatchers("/h2-console/**", "/wishlist/add", "/wishlist/reserve")
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin()
                        .httpStrictTransportSecurity(hsts -> hsts.disable())
                );

        return http.build();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(JdbcTemplate jdbcTemplate) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(username -> {
            String sql = "SELECT username, password, enabled FROM users WHERE username = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
                String user = rs.getString("username");
                String password = rs.getString("password");
                boolean enabled = rs.getBoolean("enabled");
                return org.springframework.security.core.userdetails.User.withUsername(user)
                        .password(password)
                        .accountLocked(!enabled)
                        .authorities("USER")
                        .build();
            });
        });
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}