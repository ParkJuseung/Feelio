package com.test.feelio.config;

import com.test.feelio.oauth.CustomOAuth2UserService;
import com.test.feelio.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 및 공개 페이지 접근 허용
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // OAuth2 관련 URL 허용
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        // 일기 관련 페이지는 인증 필요
                        .requestMatchers("/diary/**").authenticated()
                        .requestMatchers("/home").authenticated()
                        // REST API 접근 설정
                        .requestMatchers(HttpMethod.GET, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/diary/**").authenticated()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // UserDetailsService 명시적 설정
                .userDetailsService(customUserDetailsService)
                // CSRF 보호 설정
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/api/**")
                )
                // 세션 관리
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(false)
                );

        return http.build();
    }
}