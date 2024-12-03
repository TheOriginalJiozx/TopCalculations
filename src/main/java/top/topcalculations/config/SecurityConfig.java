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

    @Value("${MYSQL_URL}")
    String mysqlUrl; // Miljøvariabel til MySQL-database-URL.
    @Value("${MYSQL_USER}")
    String mysqlUsername; // Miljøvariabel til MySQL-brugernavn.
    @Value("${MYSQL_PASSWORD}")
    String mysqlPassword; // Miljøvariabel til MySQL-adgangskode.

    @Bean
    @Profile("prod")
    // Definerer en bean til en MySQL-datasource med værdier hentet fra miljøvariabler.
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(mysqlUrl);
        dataSource.setUsername(mysqlUsername);
        dataSource.setPassword(mysqlPassword);
        return dataSource;
    }

    // H2-databasen konfigureres med standardværdier for testformål.
    private String dbUrl = "jdbc:h2:~/test;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:h2init.sql'";
    private String dbUsername = "sa"; // Standard brugernavn for H2-databasen.
    private String dbPassword = ""; // Standard adgangskode for H2-databasen.

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
    // Definerer en sikkerhedsfilterkæde, som håndterer autorisation og autentifikation for HTTP-forespørgsler.
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/header.html", "/footer.html", "/h2-console/**", "/css/**", "/wishlist/reserve", "/project").permitAll()
                        .requestMatchers("/login", "/signup").anonymous() // Tillader kun anonyme brugere at tilgå disse sider.
                        //.requestMatchers("/view-projects").hasRole("ADMIN") // Kommentarer indikerer mulig rollebaseret adgangskontrol.
                        .anyRequest().authenticated() // Kræver autentifikation for alle andre forespørgsler.
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
                return org.springframework.security.core.userdetails.User.withUsername(user) // Starter oprettelsen af en UserDetails-instans med det angivne brugernavn.
                        .password(password) // Indstiller brugerens krypterede adgangskode hentet fra databasen.
                        .accountLocked(!enabled) // Låser kontoen, hvis brugeren ikke er aktiveret.
                        .authorities("USER") // Standardautorisation som "USER".
                        .roles(role) // Roller, der bestemmer adgangsniveauet, hentes fra databasen.
                        .build(); // Bygger en UserDetails-instans baseret på de specificerede oplysninger.
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