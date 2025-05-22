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
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
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
     * @return 로그인 성공 여부
     */
    /*
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

    /**
     * 이메일로 사용자를 찾습니다.
     * @param email 사용자 이메일
     * @return 사용자 정보
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * ID로 사용자를 찾습니다.
     * @param id 사용자 ID
     * @return 사용자 정보
     */
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: ID=" + id));
    }

    @Transactional
    public void updateProfile(Long userId, String newNickname, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 닉네임 변경
        if (newNickname != null && !newNickname.isBlank()) {
            user.setNickname(newNickname);
        }

        // 비밀번호 변경 (현재 비밀번호 & 새 비밀번호 둘 다 있는 경우)
        if (currentPassword != null && !currentPassword.isBlank()
                && newPassword != null && !newPassword.isBlank()) {

            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }

            String encodedNewPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedNewPassword);
        }

        userRepository.save(user);
    }

    @Transactional
    public void updateNickname(Long userId, String newNickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (newNickname != null && !newNickname.isBlank()) {
            user.setNickname(newNickname);
            userRepository.save(user);
        }
    }

    public void updateUser(User user) {
        userRepository.save(user);
    }


}