package com.test.feelio.oauth;

import com.test.feelio.entity.Role;
import com.test.feelio.entity.User;
import com.test.feelio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        // OAuth2 서비스 ID (google, naver, kakao)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // OAuth2 로그인 진행시 키가 되는 필드 값 (PK) (구글의 경우 'sub')
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // OAuth2UserService를 통해 가져온 데이터를 담을 클래스
        OAuthAttributes attributes = OAuthAttributes.of(registrationId, userNameAttributeName, oAuth2User.getAttributes());

        User user = saveOrUpdate(attributes, registrationId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(existingUser -> {
                    // 기존 사용자인 경우 nickname만 업데이트
                    existingUser.setNickname(attributes.getNickname());
                    // OAuth 사용자는 항상 활성화 상태로 유지하고 약관 동의 처리
                    existingUser.setEnabled(true);
                    existingUser.setTermsAgree(true);
                    existingUser.setPrivacyAgree(true);
                    // provider 정보도 업데이트 (소셜 로그인으로 전환된 경우)
                    if (existingUser.getProvider() == null) {
                        existingUser.setProvider(registrationId);
                        existingUser.setProviderId((String) attributes.getAttributes().get(attributes.getNameAttributeKey()));
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 새 사용자 생성
                    User newUser = attributes.toEntity(registrationId);

                    // 소셜 로그인 사용자를 위한 임의 비밀번호 설정
                    String randomPassword = UUID.randomUUID().toString();
                    newUser.setPassword(passwordEncoder.encode(randomPassword));

                    return newUser;
                });

        System.out.println("===== OAuth 사용자 저장 =====");
        System.out.println("이메일: " + user.getEmail());
        System.out.println("닉네임: " + user.getNickname());
        System.out.println("제공자: " + user.getProvider());
        System.out.println("termsAgree: " + user.isTermsAgree());
        System.out.println("privacyAgree: " + user.isPrivacyAgree());
        System.out.println("marketingAgree: " + user.isMarketingAgree());
        System.out.println("enabled: " + user.isEnabled());
        System.out.println("========================");

        return userRepository.save(user);
    }
}