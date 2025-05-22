package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "USERS")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "USER_SEQ_GENERATOR")
    @SequenceGenerator(name = "USER_SEQ_GENERATOR", sequenceName = "USER_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = true, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "TERMS_AGREE", nullable = false)
    private boolean termsAgree;

    @Column(name = "PRIVACY_AGREE", nullable = false)
    private boolean privacyAgree;

    @Column(name = "MARKETING_AGREE")
    private boolean marketingAgree;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    // 소셜 로그인을 위한 필드
    @Column(length = 20)
    private String provider;  // KAKAO, GOOGLE, NAVER, 또는 null(일반 가입)

    @Column(name = "PROVIDER_ID", length = 255)
    private String providerId;

    // 역할 정보 (문자열 열거형으로 저장)
    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role = Role.User;  // 기본값은 USER로 설정

    // JPA 이벤트 리스너
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // update 메소드 추가
    public User update(String nickname, String email) {
        this.nickname = nickname;
        this.email = email;
        return this;
    }

    // 기존 빌더 메서드 제거하고 Lombok @Builder로 통일
}