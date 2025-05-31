package com.test.feelio.service;

import com.test.feelio.dto.DiaryGalleryDTO;
import com.test.feelio.repository.GalleryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GalleryService {
    private final GalleryRepository galleryRepository;

    public GalleryService(GalleryRepository galleryRepository) {
        this.galleryRepository = galleryRepository;
    }

    public List<DiaryGalleryDTO> getGalleryPhotosByUser(Long userId) {
        return galleryRepository.findGalleryPhotosByUser(userId);
    }
}
