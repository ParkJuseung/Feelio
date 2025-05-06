package com.test.feelio.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "CHATBOT_TYPES")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotType {
    @Id
    @Column(name = "CHATBOT_TYPE_ID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "CHATBOT_TYPE_SEQ_GENERATOR")
    @SequenceGenerator(name = "CHATBOT_TYPE_SEQ_GENERATOR", sequenceName = "CHATBOT_TYPE_SEQ", allocationSize = 1)
    private Long id;

    @Column(name = "TYPE_NAME", nullable = false, length = 50)
    private String typeName;

    @Column(length = 500)
    private String description;

    @Lob
    @Column(name = "PERSONA_DESCRIPTION")
    private String personaDescription;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    // 연관 관계 매핑
    @OneToMany(mappedBy = "chatbotType", cascade = CascadeType.ALL)
    private List<ChatSession> chatSessions = new ArrayList<>();

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
}