package com.test.feelio.controller;

import com.test.feelio.entity.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class MypageController {

    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // 세션에서 User 꺼내기
        if (user != null) {
            model.addAttribute("user", user); // Thymeleaf에 전달
        }
        return "pages/mypage";
    }

    @GetMapping("/mypage/profile")
    public String showProfile(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // 세션에서 User 꺼내기
        if (user != null) {
            model.addAttribute("user", user); // Thymeleaf에 전달
        }
        return "pages/profile";
    }

    @PostMapping("/mypage/profile")
    public String updateProfile() {
        return "redirect:/mypage";
    }

    @GetMapping("/mypage/gallery")
    public String showGallery(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // 세션에서 User 꺼내기
        if (user != null) {
            model.addAttribute("user", user); // Thymeleaf에 전달
        }
        return "pages/gallery";
    }
}
