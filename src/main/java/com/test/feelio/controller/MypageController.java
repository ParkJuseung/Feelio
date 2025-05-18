package com.test.feelio.controller;

import com.test.feelio.dto.DiaryGalleryDTO;
import com.test.feelio.entity.User;
import com.test.feelio.service.GalleryService;
import com.test.feelio.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Controller
public class MypageController {

    private final GalleryService galleryService;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    public MypageController(GalleryService galleryService,
                            PasswordEncoder passwordEncoder,
                            UserService userService) {
        this.galleryService = galleryService;
        this.passwordEncoder = passwordEncoder;
        this.userService = userService;
    }



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
            if (user.getPassword() == null) {
                return "redirect:/profile-oauth";
            }
        }
        return "pages/profile";
    }

    @PostMapping("/mypage/profile")
    public String updateProfile(
            @RequestParam String nickname,
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User user = (User) session.getAttribute("user");

        if (user == null) return "redirect:/login";

        // 1. 기존 비밀번호 일치 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "현재 비밀번호가 일치하지 않습니다.");
            return "redirect:/mypage/profile";
        }

        // 2. 새 비밀번호 일치 확인
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "새 비밀번호가 일치하지 않습니다.");
            return "redirect:/mypage/profile";
        }

        // 3. 닉네임, 비밀번호 업데이트
        user.setNickname(nickname);
        user.setPassword(passwordEncoder.encode(newPassword));
        //userService.updateUser(user); // DB 반영

        // 4. 세션 갱신
        session.setAttribute("user", user);

        return "redirect:/mypage";
    }


    @PostMapping("/profile-oauth")
    public String updateNickname(@RequestParam String nickname, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user != null) {
            user.setNickname(nickname);
            //userService.updateNickname(user.getUserId(), nickname); // DB 반영
            session.setAttribute("user", user); // 세션 갱신
        }
        return "redirect:/mypage/profile";
    }


    @GetMapping("/mypage/gallery")
    public String showGallery(HttpSession session, Model model) {
        User user = (User) session.getAttribute("user"); // 세션에서 User 꺼내기
        if (user != null) {
            model.addAttribute("user", user); // Thymeleaf에 전달
        }


        List<DiaryGalleryDTO> photoList = galleryService.getGalleryPhotosByUser(user.getId());

        Map<String, List<DiaryGalleryDTO>> photoMap = photoList.stream()
                .collect(Collectors.groupingBy(
                        DiaryGalleryDTO::getYearMonth,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        model.addAttribute("photoMap", photoMap);


        return "pages/gallery";
    }
}
