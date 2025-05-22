package com.test.feelio.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // LoginCheckInterceptor를 비활성화 (Spring Security가 인증을 처리하므로)
        // Spring Security와 중복되는 인증 로직이므로 주석처리

        /*
        registry.addInterceptor(new LoginCheckInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/login", "/signup", "/css/**", "/js/**", "/images/**", "/error");
        */
    }
}