package com.test.feelio.controller;

import com.test.feelio.dto.UserRegisterDto;
import com.test.feelio.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/signup")
    public String showSignupForm(Model model) {
        model.addAttribute("userRegisterDto", new UserRegisterDto());
        return "pages/signup";
    }

    @PostMapping("/signup")
    public String registerUser(@ModelAttribute @Valid UserRegisterDto userRegisterDto,
                               BindingResult bindingResult, Model model) {
        // 이메일 중복 확인
        if (userService.isEmailExists(userRegisterDto.getEmail())) {
            bindingResult.rejectValue("email", "error.userRegisterDto", "이미 사용중인 이메일입니다");
        }

        // 닉네임 중복 확인
        if (userService.isNicknameExists(userRegisterDto.getNickname())) {
            bindingResult.rejectValue("nickname", "error.userRegisterDto", "이미 사용중인 닉네임입니다");
        }

        if (bindingResult.hasErrors()) {
            return "pages/signup";
        }

        userService.registerUser(userRegisterDto);
        return "redirect:/login?registered";
    }

    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "pages/login";
    }
}