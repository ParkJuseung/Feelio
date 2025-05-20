package com.test.feelio;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderTest {

    @Test
    void 비밀번호_매치_테스트() {
        System.out.println("테스트 시작");
        try {
            String raw = "wwww1111";
            String hash = "$2a$10$RzJPK9KuVb4pY1UZZJdSke0Muu2wYdN7dwBEdNp854EVaoVgG2bdW";

            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

            boolean result = passwordEncoder.matches(raw, hash);
            System.out.println("매치 결과: " + result);
        } catch (Exception e) {
            System.out.println("예외 발생!");
            e.printStackTrace();
        }
        System.out.println("테스트 종료됨");
    }
}
