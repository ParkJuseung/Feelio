package com.test.feelio.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

// 로그인 세션 체크를 위한 인터셉터
public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            // 로그인되지 않은 사용자는 로그인 페이지로 리다이렉트
            response.sendRedirect("/login");
            return false;
        }

        return true;
    }
}
