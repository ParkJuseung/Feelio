package com.test.feelio.service;

import com.test.feelio.entity.Role;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Test
    void 닉네임만_수정하면_비밀번호는_그대로() {

        // given
        User user = User.builder()
                .email("test@email.com")
                .nickname("기존닉")
                .password(passwordEncoder.encode("1234")) // 초기 비밀번호
                .role(Role.User)
                .build();

        userRepository.save(user);

        // when
        userService.updateProfile(user.getId(), "수정된닉", "", "");

        // then
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assertEquals("수정된닉", updatedUser.getNickname());
        assertTrue(passwordEncoder.matches("1234", updatedUser.getPassword()));
    }
}


