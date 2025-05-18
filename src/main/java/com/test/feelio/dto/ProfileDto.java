package com.test.feelio.dto;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProfileDto {
    private Long userId;
    private String email;
    private String nickname;

    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}
