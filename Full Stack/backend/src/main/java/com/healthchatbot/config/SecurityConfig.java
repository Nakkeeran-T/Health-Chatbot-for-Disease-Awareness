package com.healthchatbot.config;

import com.healthchatbot.security.JwtAuthFilter;
import com.healthchatbot.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthFilter jwtAuthFilter;
        private final UserDetailsServiceImpl userDetailsService;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .headers(headers -> headers
                                                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**", "/h2-console/**", "/actuator/**")
                                                .permitAll()
                                                // Twilio webhook — no JWT (Twilio calls from external)
                                                .requestMatchers("/api/webhook/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                                                "/api/diseases", "/api/diseases/**",
                                                                "/api/vaccines", "/api/vaccines/**",
                                                                "/api/alerts", "/api/alerts/**")
                                                .permitAll()
                                                .requestMatchers("/api/admin", "/api/admin/**").permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                                                "/api/diseases", "/api/diseases/**",
                                                                "/api/vaccines", "/api/vaccines/**",
                                                                "/api/alerts", "/api/alerts/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                                                                "/api/diseases", "/api/diseases/**",
                                                                "/api/vaccines", "/api/vaccines/**",
                                                                "/api/alerts", "/api/alerts/**")
                                                .permitAll()
                                                .requestMatchers(org.springframework.http.HttpMethod.DELETE,
                                                                "/api/diseases", "/api/diseases/**",
                                                                "/api/vaccines", "/api/vaccines/**",
                                                                "/api/alerts", "/api/alerts/**")
                                                .permitAll()
                                                .anyRequest().permitAll())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider())
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
                provider.setUserDetailsService(userDetailsService);
                provider.setPasswordEncoder(passwordEncoder());
                return provider;
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
