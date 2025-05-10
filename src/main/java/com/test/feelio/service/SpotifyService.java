package com.test.feelio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SpotifyService {

    @Value("${spotify.client.id}")
    private String clientId;

    @Value("${spotify.client.secret}")
    private String clientSecret;

    @Value("${spotify.token.url}")
    private String tokenUrl;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.spotify.com")
            .build();

    private String accessToken;
    private long tokenExpiryTime;

    public String getAccessToken() {
        // 토큰이 없거나 만료되었으면 새로 발급
        if (accessToken == null || System.currentTimeMillis() >= tokenExpiryTime) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        try {
            // Client Credentials 인코딩
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

            // 토큰 요청
            Map<String, String> params = new HashMap<>();
            params.put("grant_type", "client_credentials");

            String response = WebClient.create()
                    .post()
                    .uri(tokenUrl)
                    .header("Authorization", "Basic " + encodedCredentials)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue("grant_type=client_credentials")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 응답 파싱
            ObjectMapper mapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = mapper.readValue(response, Map.class);

            this.accessToken = (String) tokenResponse.get("access_token");

            // 만료 시간 설정 (실제 만료 시간보다 5분 일찍 갱신)
            Integer expiresIn = (Integer) tokenResponse.get("expires_in");
            this.tokenExpiryTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L;

            log.info("Spotify 토큰 갱신 완료");

        } catch (Exception e) {
            log.error("Spotify 토큰 갱신 실패: ", e);
            throw new RuntimeException("Spotify 토큰 갱신에 실패했습니다", e);
        }
    }

    // 추가로 필요한 경우 Spotify API 호출 메서드들...
}