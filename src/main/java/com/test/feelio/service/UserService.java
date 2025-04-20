package com.test.feelio.service;

import com.test.feelio.dto.UserLoginDto;
import com.test.feelio.dto.UserRegisterDto;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserRegisterDto registerDto) {
        User user = new User();
        user.setEmail(registerDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerDto.getPassword()));
        user.setNickname(registerDto.getNickname());
        user.setTermsAgree(registerDto.isTermsAgree());
        user.setPrivacyAgree(registerDto.isPrivacyAgree());
        user.setMarketingAgree(registerDto.isMarketingAgree());

        userRepository.save(user);
    }

    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public boolean isNicknameExists(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    /**
     * 사용자 로그인을 처리합니다.
     * @param loginDto 로그인 정보
     * @param session 세션 객체
     * @return 로그인 성공 여부
     */
    public boolean loginUser(UserLoginDto loginDto, HttpSession session) {
        logger.info("로그인 시도: {}", loginDto.getEmail());

        Optional<User> userOptional = userRepository.findByEmail(loginDto.getEmail());

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
                // 세션에 사용자 정보 저장
                session.setAttribute("user", user);
                logger.info("로그인 성공: {}", user.getEmail());

                return true;
            }
            logger.info("비밀번호 불일치: {}", loginDto.getEmail());

        }else {
            logger.info("사용자 없음: {}", loginDto.getEmail());
        }
            return false;
    }

}