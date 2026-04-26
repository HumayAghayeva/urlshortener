package url_shortener.urlshortener.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public redirect
                        .requestMatchers(HttpMethod.GET, "/{shortCode:[a-zA-Z0-9-]{3,50}}").permitAll()
                        // Public URL info
                        .requestMatchers(HttpMethod.GET, "/api/v1/urls/{shortCode}").permitAll()
                        // Swagger / OpenAPI
                        .requestMatchers("/swagger-ui/**", "/v1/api-docs/**").permitAll()
                        // Actuator health
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        // Everything else requires auth
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> {});   // Replace with JWT filter in production

        return http.build();
    }

    /**
     * In-memory users for demo purposes.
     * Replace with a proper UserDetailsService backed by your database.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var user = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();
        var admin = User.withDefaultPasswordEncoder()
                .username("admin")
                .password("admin")
                .roles("USER", "ADMIN")
                .build();
        return new InMemoryUserDetailsManager(user, admin);
    }
}
