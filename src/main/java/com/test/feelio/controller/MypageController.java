package com.test.feelio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MypageController {

    @GetMapping("/mypage")
    public String mypage() {
        return "pages/mypage";
    }

    @GetMapping("/mypage/profile")
    public String showProfile() {
        return "pages/profile";
    }

    @PostMapping("/mypage/profile")
    public String updateProfile() {
        return "redirect:/mypage";
    }
}
