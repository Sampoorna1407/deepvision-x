package com.aidetector.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetectionResult {

    public enum ContentType {
        TEXT,
        IMAGE,
        VIDEO
    }

    public enum Verdict {
        AI_GENERATED,
        LIKELY_AI,
        UNCERTAIN,
        LIKELY_HUMAN,
        HUMAN
    }

    private ContentType contentType;
    private Verdict verdict;

    private double aiProbability;
    private double humanProbability;
    private double confidenceScore;

    private String summary;

    private List<String> signals;

    private Map<String, Double> featureScores;

    private String fileName;
    private long fileSizeBytes;

    private String processingTimeMs;

    private String modelUsed;

    private boolean success;

    private String errorMessage;
}