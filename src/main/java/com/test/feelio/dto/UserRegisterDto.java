package com.test.feelio.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRegisterDto {
    @NotBlank(message = "이메일은 필수 항목입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수 항목입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$",
            message = "비밀번호는 8자 이상의 영문과 숫자 조합이어야 합니다")
    private String password;

    @NotBlank(message = "닉네임은 필수 항목입니다")
    @Size(min = 2, max = 10, message = "닉네임은 2~10자 이내로 입력해주세요")
    private String nickname;

    @AssertTrue(message = "이용약관에 동의해야 합니다")
    private boolean termsAgree;

    @AssertTrue(message = "개인정보 수집 및 이용에 동의해야 합니다")
    private boolean privacyAgree;

    private boolean marketingAgree;
}