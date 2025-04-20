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
    private final PasswordEncoder passwordEncoder; // 필드 추가



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
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes.getAttributes(),
                attributes.getNameAttributeKey()
        );
    }

    private User saveOrUpdate(OAuthAttributes attributes, String registrationId) {
        User user = userRepository.findByEmail(attributes.getEmail())
                .map(entity -> entity.update(attributes.getNickname(), attributes.getEmail()))
                .orElse(attributes.toEntity(registrationId));

        // 소셜 로그인 사용자를 위한 임의 비밀번호 설정
        if (user.getPassword() == null) {
            String randomPassword = UUID.randomUUID().toString();
            // 비밀번호 인코더를 사용하여 암호화
            user.setPassword(passwordEncoder.encode(randomPassword));
        }

        System.out.println("termsAgree: " + user.isTermsAgree());
        System.out.println("privacyAgree: " + user.isPrivacyAgree());
        System.out.println("marketingAgree: " + user.isMarketingAgree());
        System.out.println("enabled: " + user.isEnabled());


        return userRepository.save(user);
    }


}