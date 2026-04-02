package com.walletx.authservice.config.security;

import com.walletx.authservice.security.JwtAuthenticationEntryPoint;
import com.walletx.authservice.security.JwtAuthenticationFilter;
import com.walletx.authservice.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central security configuration for the WalletX auth service.
 *
 * <p>Defines the security filter chain, authentication provider, password-encoding
 * strategy, and public endpoint rules. Uses JWT-based stateless authentication -
 * no server-side sessions are created or maintained.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    /**
     * Provides a {@link BCryptPasswordEncoder} bean for hashing user passwords.
     *
     * <p>BCrypt is the industry standard for password hashing - it applies a
     * random salt and is deliberately slow to resist brute-force attacks.</p>
     *
     * @return a {@link PasswordEncoder} backed by BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures the {@link DaoAuthenticationProvider} that connects user loading,
     * password verification, and the authentication process.
     *
     * <p>Wires together: </p>
     * <ul>
     *     <li>The {@link UserDetailsServiceImpl} - loads the user from the database</li>
     *     <li>The {@link PasswordEncoder} - verifies the BCrypt-hashed password</li>
     * </ul>
     *
     * @return a configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsServiceImpl);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Exposes the {@link AuthenticationManager} as a Spring bean.
     *
     * <p>Required by the authentication service to programmatically trigger
     * the authentication process during login.</p>
     *
     * @param authenticationConfiguration the Spring Security authentication configuration
     * @return the application's {@link AuthenticationManager}
     * @throws Exception if the manager cannot be built
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Defines the security filter chain that governs all HTTP request authentication.
     *
     * <p>Security decisions applied:</p>
     * <ul>
     *     <li>CSRF disabled - not needed for stateless JWT-based APIS</li>
     *     <li>Public endpoints: register, login, and refresh-token</li>
     *     <li>All other endpoints require a valid JWT token</li>
     *     <li>Session policy set to STATELESS - no server-side sessions</li>
     *     <li>{@link JwtAuthenticationFilter} executed before the default username/password filter</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} object to configure
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the security configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh-token").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
