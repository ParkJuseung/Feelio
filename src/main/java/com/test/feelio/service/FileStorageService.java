package com.test.feelio.service;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload.dir}")
    private String uploadDir;

    public String storeFile(MultipartFile file) {
        try {
            System.out.println("파일 저장 시작: " + file.getOriginalFilename());
            System.out.println("업로드 경로: " + uploadDir);

            // 업로드 디렉토리 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("디렉토리 생성됨: " + uploadPath.toAbsolutePath());
            } else {
                System.out.println("디렉토리 이미 존재: " + uploadPath.toAbsolutePath());
            }

            // 파일명 생성 (UUID + 원본파일명)
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path targetLocation = uploadPath.resolve(fileName);
            System.out.println("저장 대상 경로: " + targetLocation.toAbsolutePath());

            // 이미지 리사이징 (최대 800x800)
            try {
                Thumbnails.of(file.getInputStream())
                        .size(800, 800)
                        .keepAspectRatio(true)
                        .outputQuality(0.8)
                        .toFile(targetLocation.toFile());
                System.out.println("이미지 리사이징 및 저장 성공");
            } catch (Exception ex) {
                System.err.println("이미지 리사이징 실패, 원본 저장 시도: " + ex.getMessage());
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            if (Files.exists(targetLocation)) {
                System.out.println("파일 저장 확인: " + Files.size(targetLocation) + " bytes");
            } else {
                System.err.println("파일이 저장되지 않음!");
            }

            return fileName;
        } catch (IOException ex) {
            System.err.println("파일 저장 중 예외 발생: " + ex.getMessage());
            ex.printStackTrace();
            throw new RuntimeException("파일 저장에 실패했습니다: " + file.getOriginalFilename(), ex);
        }
    }

    public void deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("파일 삭제에 실패했습니다: " + fileName, ex);
        }
    }
}