package com.example.guias.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.guias.service.UsuarioDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UsuarioDetailsService userDetailsService;
    private final RolSuccessHandler rolSuccessHandler;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService, 
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          UsuarioDetailsService userDetailsService,
                          RolSuccessHandler rolSuccessHandler) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
        this.rolSuccessHandler = rolSuccessHandler;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // Filtro 1: Para la API REST de Flutter (/api/**) - STATELESS
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configure(http))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(authProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
        return http.build();
    }

    // Filtro 2: Para la Aplicación Web Original (Thymeleaf) - STATEFUL
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authenticationProvider(authProvider())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/login",
                    "/registro",
                    "/css/**",
                    "/js/**",
                    "/imagenes/**",
                    "/oauth2/**",
                    "/login/oauth2/code/**",
                    "/h2-console/**",
                    "/forgot-password/**",
                    "/reset-password/**",
                    "/logout",
                    "/soporte/**",
                    "/rastreo/**",
                    "/chat/ia",
                    "/chat/ia/spring-ai",
                    "/chat/ia/enviar-codigo",
                    "/chat/ia/verificar-codigo",
                    "/chat/ia/consultar-guia",
                    "/cotizacion/calcular",
                    "/conocenos",
                    "/error"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/guias/todas").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(rolSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(rolSuccessHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            )
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/logout", "/api/**"))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }
}
