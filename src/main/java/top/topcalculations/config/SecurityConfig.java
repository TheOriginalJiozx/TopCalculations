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
// Angiver, at dette er en konfigurationsklasse for sikkerhed og aktiverer metode- og web-sikkerhed.
public class SecurityConfig {

    @Bean
    // Definerer en bean til JdbcTemplate, som bruges til at udføre SQL-spørgsmål på databasen.
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    // Definerer en bean til en MySQL-datasource med værdier hentet fra miljøvariabler.
    public DataSource dataSource(
            @Value("${DB_URL:jdbc:mysql://localhost:3306/kalkulationsvaerktoej?useSSL=false}") String dbUrl,
            @Value("${DB_USER}") String username,
            @Value("${DB_PASSWORD}") String password
    ) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    // H2-databasen konfigureres med standardværdier for testformål.
    private String dbUrl = "jdbc:h2:~/test;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:h2init.sql'";
    private String dbUsername = "sa";
    private String dbPassword = "";

    @Bean
    @Profile("h2")
    // Definerer en H2-datasource til testmiljøet. Bruges kun, når profilen "h2" er aktiv.
    public DataSource h2DataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }

    @Bean
    public DataSource prodDataSource(
            @Value("${DB_URL:jdbc:mysql://localhost:3306/kalkulationsvaerktoej}") String dbUrl,
            @Value("${DB_USER}") String username,
            @Value("${DB_PASSWORD}") String password
    ) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    @Bean
    // Definerer en sikkerhedsfilterkæde, som håndterer autorisation og autentifikation for HTTP-forespørgsler.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/header.html", "/footer.html", "/h2-console/**", "/css/**").permitAll()
                        .requestMatchers("/login", "/signup").anonymous()
                        //.requestMatchers("/view-projects").hasRole("ADMIN") // Kommentarer indikerer mulig rollebaseret adgangskontrol.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login") // Angiver login-siden.
                        .defaultSuccessUrl("/", true) // Angiver siden, der vises efter vellykket login.
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Angiver logout-URL.
                        .logoutSuccessUrl("/login?logout") // Angiver siden efter logout.
                )
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**") // Deaktiverer CSRF-beskyttelse for H2-konsollen.
                )
                .headers(headers -> headers
                        .frameOptions().sameOrigin() // Tillader iframe-indlejring fra samme oprindelse (for H2-konsolens skyld).
                        .httpStrictTransportSecurity(hsts -> hsts.disable()) // Deaktiverer HSTS (anbefales ikke til produktion).
                );

        return http.build();
    }

    @Bean
    // Definerer en autentifikationsprovider, som bruger JdbcTemplate til at hente brugeroplysninger fra databasen.
    public DaoAuthenticationProvider authenticationProvider(JdbcTemplate jdbcTemplate) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(username -> {
            String sql = "SELECT username, password, enabled, role FROM users WHERE username = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
                String user = rs.getString("username");
                String password = rs.getString("password");
                boolean enabled = rs.getBoolean("enabled");
                String role = rs.getString("role");
                return org.springframework.security.core.userdetails.User.withUsername(user)
                        .password(password)
                        .accountLocked(!enabled)
                        .authorities("USER") // Standardautorisation som "USER".
                        .roles(role) // Roller hentes fra databasen.
                        .build();
            });
        });

        authProvider.setPasswordEncoder(passwordEncoder()); // Sætter en BCrypt-password-encoder.
        return authProvider;
    }

    @Bean
    // Definerer en password-encoder baseret på BCrypt.
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}