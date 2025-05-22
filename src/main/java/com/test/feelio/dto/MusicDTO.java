package com.test.feelio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MusicDTO {
    private Long id;
    private String title;
    private String artist;
    private String spotifyLink;
}