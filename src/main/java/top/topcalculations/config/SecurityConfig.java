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

    private String dbUrl = "jdbc:h2:~/test;MODE=MYSQL;DATABASE_TO_LOWER=TRUE;INIT=runscript from 'classpath:h2init.sql'";
    private String dbUsername = "sa";
    private String dbPassword = "";

    @Bean
    @Profile("h2")
    public DataSource h2DataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl(dbUrl);
        dataSource.setUsername(dbUsername);
        dataSource.setPassword(dbPassword);
        return dataSource;
    }

    @Bean
    public DataSource h2dataSource(
            @Value("") String dbUrl,
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/header.html", "/footer.html", "/h2-console/**", "/css/**", "/wishlist/reserve", "/project").permitAll()
                        .requestMatchers("/login", "/signup").anonymous()
                        //.requestMatchers("/view-projects").hasRole("ADMIN")
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
                        .ignoringRequestMatchers("/h2-console/**")
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
            String sql = "SELECT username, password, enabled, role FROM users WHERE username = ?";
            return jdbcTemplate.queryForObject(sql, new Object[]{username}, (rs, rowNum) -> {
                String user = rs.getString("username");
                String password = rs.getString("password");
                boolean enabled = rs.getBoolean("enabled");
                String role = rs.getString("role");
                return org.springframework.security.core.userdetails.User.withUsername(user)
                        .password(password)
                        .accountLocked(!enabled)
                        .authorities("USER")
                        .roles(role)
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