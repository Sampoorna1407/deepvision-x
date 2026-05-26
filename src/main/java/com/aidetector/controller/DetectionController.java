package com.aidetector.controller;

import com.aidetector.model.DetectionResult;
import com.aidetector.service.VideoDetectionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/detect")
@CrossOrigin(origins = "*")
public class DetectionController {

    @Autowired
    private VideoDetectionService videoDetectionService;

    @PostMapping("/video")
    public ResponseEntity<?> detectVideo(
            @RequestParam("file") MultipartFile file) {

        try {

            DetectionResult result =
                    videoDetectionService.detectVideo(file);

            return ResponseEntity.ok(result);

        } catch (Exception e) {

            return ResponseEntity.badRequest()
                    .body("Error processing video: " + e.getMessage());
        }
    }
}