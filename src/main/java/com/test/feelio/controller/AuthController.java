package com.test.feelio.controller;

import com.test.feelio.dto.UserLoginDto;
import com.test.feelio.dto.UserRegisterDto;
import com.test.feelio.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
        //model.addAttribute("userLoginDto", new UserLoginDto());
        return "pages/login";
    }

    /*
    @PostMapping("/login")
    public String processLogin(@ModelAttribute @Valid UserLoginDto userLoginDto,
                               BindingResult bindingResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        System.out.println("======== 로그인 컨트롤러 호출됨 ========");
        System.out.println("입력된 이메일: " + userLoginDto.getEmail());


        // 유효성 검사 실패 시 로그인 페이지로 이동
        if (bindingResult.hasErrors()) {
            return "pages/login";
        }

        // 로그인 처리
        boolean loginSuccess = userService.loginUser(userLoginDto, session);

        if (loginSuccess) {
            return "redirect:/"; // 로그인 성공 시 메인 페이지로 이동
        } else {
            // 로그인 실패 메시지 추가
            redirectAttributes.addFlashAttribute("loginError", "이메일 또는 비밀번호가 일치하지 않습니다.");
            return "redirect:/login";
        }
    }
    */



    // 로그아웃 처리
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }


}