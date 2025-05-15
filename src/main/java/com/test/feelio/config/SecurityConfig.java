package com.test.feelio.config;

import com.test.feelio.oauth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;

    // PasswordEncoder 빈 제거 (이미 PasswordConfig에 정의되어 있음)

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 접근 허용
                        .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // diary-write.html에 대한 접근 명시적으로 허용
                        .requestMatchers("/diary/write").authenticated()  // 확인 필요
                        // REST API 접근 설정
                        .requestMatchers(HttpMethod.GET, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/diary/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/diary/**").authenticated()
                        // 일기 관련 페이지는 인증 필요
                        .requestMatchers("/diary/**").authenticated()
                        .requestMatchers("/home").authenticated()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login-process")
                        .defaultSuccessUrl("/home")  // 로그인 후 일기 목록 페이지로 이동
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/home")  // 로그인 후 일기 목록 페이지로 이동
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService))
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // CSRF 보호 설정 - REST API에서는 토큰 사용
                .csrf(csrf -> csrf
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // AJAX 호출에는 CSRF 토큰이 필요하므로 비활성화하지 않음
                )
                // 세션 관리
                .sessionManagement(session -> session
                        .invalidSessionUrl("/login")
                        .maximumSessions(1)  // 동시 세션 제한
                        .maxSessionsPreventsLogin(false)  // 이전 세션 만료
                        .expiredUrl("/login?expired")
                );

        return http.build();
    }
}