package br.com.central.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SegurancaConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    SecurityFilterChain seguranca(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/demonstracao/entrar", "/error", "/css/**").permitAll()
                .requestMatchers("/permissoes", "/permissoes/**").hasRole("ADMINISTRADOR")
                .requestMatchers(HttpMethod.GET, "/incidentes/novo").hasAnyRole("ADMINISTRADOR", "ANALISTA")
                .requestMatchers(HttpMethod.POST, "/api/incidentes/**", "/incidentes/**").hasAnyRole("ADMINISTRADOR", "ANALISTA")
                .anyRequest().authenticated())
                .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.permitAll())
                .build();
    }
}
