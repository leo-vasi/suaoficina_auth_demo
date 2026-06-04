package com.pfc.suaoficina.authdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

// REQ 3.1 - Comunicação protegida por TLS/HTTPS
// REQ 3.2 - Bloqueio de conexões não seguras
// REQ 3.8 - Justificativa técnica das escolhas
@Configuration
public class SecurityConfig {

    // Rotas públicas declaradas explicitamente — qualquer rota não listada exige autenticação.
    // CSRF desabilitado pois a API é stateless (tokens UUID no header Authorization).
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/auth/forgot-password",
                                "/auth/reset-password",
                                "/auth/verify-2fa",
                                "/auth/enable-2fa",
                                "/auth/validate-session",
                                "/auth/consent",
                                "/auth/export-data",
                                "/auth/revoke-consent",
                                "/auth/delete-account"
                        ).permitAll()
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    // CORS restrito às origens do frontend — impede requisições de domínios não autorizados.
    // REQ 3.1 - Comunicação protegida por TLS/HTTPS
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "https://localhost:3000",
                "https://localhost:5500",
                "http://localhost:5500",
                "http://localhost:63342"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}