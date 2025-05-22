package com.test.feelio.oauth;

import com.test.feelio.entity.Role;
import com.test.feelio.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String nickname;
    private String email;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String nickname, String email) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.nickname = nickname;
        this.email = email;
    }

    // OAuth2User에서 반환하는 사용자 정보는 Map이기 때문에 값 하나하나를 변환해야 함
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        // 구글 로그인
        if ("google".equals(registrationId)) {
            return ofGoogle(userNameAttributeName, attributes);
        }

        // 추후 네이버, 카카오 로그인 추가 시 구현
        return ofGoogle(userNameAttributeName, attributes); // 기본값은 구글로 설정
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .nickname((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    public User toEntity(String provider) {
        return User.builder()
                .nickname(nickname)
                .email(email)
                .role(Role.User)
                .provider(provider)
                .providerId((String) attributes.get(nameAttributeKey))
                // OAuth 사용자는 자동으로 활성화되고 약관에 동의한 것으로 처리
                .enabled(true)
                .termsAgree(true)
                .privacyAgree(true)
                .marketingAgree(false)  // 마케팅 동의는 기본 false
                .build();
    }
}