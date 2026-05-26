package com.aidetector.service;

import com.aidetector.model.DetectionResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class VideoDetectionService {

    private static final Set<String> AI_VIDEO_TOOLS = Set.of(
        "sora", "runway", "pika", "gen-2", "stable video", "kling",
        "animate diff", "modelscope", "zeroscope", "synthesia", "heygen", "d-id"
    );

    public DetectionResult detectVideo(MultipartFile file) throws IOException {
        long start = System.currentTimeMillis();
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        byte[] bytes = file.getBytes();

        Map<String, Double> scores = new LinkedHashMap<>();
        List<String> signals = new ArrayList<>();

        scores.put("Filename Pattern",       analyzeFilename(filename, signals));
        scores.put("File Size Profile",      analyzeSize(file.getSize(), filename, signals));
        scores.put("Container Format",       analyzeHeader(bytes, signals));
        scores.put("Embedded Metadata Scan", analyzeMetadata(bytes, signals));
        scores.put("Format Consistency",     analyzeConsistency(filename, bytes, signals));

        double aiProb = weightedScore(scores);
        DetectionResult.Verdict verdict = verdict(aiProb);

        return DetectionResult.builder()
            .contentType(DetectionResult.ContentType.VIDEO)
            .verdict(verdict)
            .aiProbability(r(aiProb)).humanProbability(r(1-aiProb)).confidenceScore(0.55)
            .summary(summary(verdict, aiProb, file.getSize(), signals))
            .signals(signals).featureScores(roundMap(scores))
            .fileName(file.getOriginalFilename()).fileSizeBytes(file.getSize())
            .processingTimeMs((System.currentTimeMillis()-start)+"ms")
            .modelUsed("VideoMetadata-v1.0").success(true).build();
    }

    private double analyzeFilename(String fn, List<String> signals) {
        for (String t : AI_VIDEO_TOOLS) if (fn.contains(t)) {
            signals.add("AI tool reference in filename: \"" + t + "\""); return 0.90;
        }
        if (fn.matches(".*[a-f0-9]{8}-[a-f0-9]{4}-.*")) { signals.add("UUID filename — AI generation naming"); return 0.70; }
        if (fn.matches("(output|generated|result|render|clip)[-_]?\\d*\\..*")) { signals.add("Generic generation filename"); return 0.55; }
        return 0.20;
    }

    private double analyzeSize(long size, String fn, List<String> signals) {
        double mb = size / (1024.0 * 1024.0);
        String ext = fn.contains(".") ? fn.substring(fn.lastIndexOf('.')+1) : "";
        if (mb < 5.0 && Set.of("mp4","mov","avi").contains(ext)) {
            signals.add(String.format("Unusually small video (%.1f MB)", mb)); return 0.65;
        }
        if (mb > 500) return 0.15;
        return 0.35;
    }

    private double analyzeHeader(byte[] b, List<String> signals) {
        if (b.length > 4 && b[0]==0x1A && b[1]==0x45 && b[2]==(byte)0xDF && b[3]==(byte)0xA3) {
            signals.add("WebM container — common in web-based AI generators"); return 0.55;
        }
        return 0.30;
    }

    private double analyzeMetadata(byte[] bytes, List<String> signals) {
        String raw = new String(bytes, 0, Math.min(bytes.length, 65536)).toLowerCase();
        for (String t : AI_VIDEO_TOOLS) if (raw.contains(t)) {
            signals.add("AI tool found in metadata: \"" + t + "\""); return 0.92;
        }
        if (raw.contains("openai") || raw.contains("runway ml")) { signals.add("AI company in metadata"); return 0.85; }
        boolean hasCamera = raw.contains("make") || raw.contains("canon") || raw.contains("nikon")
            || raw.contains("sony") || raw.contains("apple") || raw.contains("gps");
        if (!hasCamera) { signals.add("No camera manufacturer metadata — atypical for real recordings"); return 0.60; }
        return 0.20;
    }

    private double analyzeConsistency(String fn, byte[] b, List<String> signals) {
        String ext = fn.contains(".") ? fn.substring(fn.lastIndexOf('.')+1).toLowerCase() : "";
        boolean isWebM = b.length > 4 && b[0]==0x1A && b[1]==0x45;
        boolean isMp4  = b.length > 8 && new String(b, 4, 4).contains("ftyp");
        if (ext.equals("mp4") && isWebM) { signals.add("Extension (.mp4) vs container (WebM) mismatch"); return 0.80; }
        if (ext.equals("webm") && isMp4) { signals.add("Extension (.webm) vs container (MP4) mismatch"); return 0.75; }
        return 0.25;
    }

    private double weightedScore(Map<String, Double> scores) {
        Map<String, Double> w = Map.of(
            "Filename Pattern", 0.15, "File Size Profile", 0.15,
            "Container Format", 0.15, "Embedded Metadata Scan", 0.40, "Format Consistency", 0.15
        );
        double num = 0, den = 0;
        for (var e : scores.entrySet()) { double wt = w.getOrDefault(e.getKey(),0.1); num+=e.getValue()*wt; den+=wt; }
        return den > 0 ? num/den : 0.5;
    }

    private DetectionResult.Verdict verdict(double p) {
        if (p >= 0.78) return DetectionResult.Verdict.AI_GENERATED;
        if (p >= 0.58) return DetectionResult.Verdict.LIKELY_AI;
        if (p >= 0.40) return DetectionResult.Verdict.UNCERTAIN;
        if (p >= 0.22) return DetectionResult.Verdict.LIKELY_HUMAN;
        return DetectionResult.Verdict.HUMAN;
    }

    private String summary(DetectionResult.Verdict v, double p, long size, List<String> signals) {
        double mb = size / (1024.0 * 1024.0);
        return switch (v) {
            case AI_GENERATED -> String.format("%.1f MB video: Strong AI indicators (%.0f%%).", mb, p*100);
            case LIKELY_AI    -> String.format("%.1f MB video: Likely AI origin (%.0f%%).", mb, p*100);
            case UNCERTAIN    -> String.format("%.1f MB video: Inconclusive (%.0f%% AI). Frame-level analysis recommended.", mb, p*100);
            default           -> String.format("%.1f MB video: Appears authentic (%.0f%% human).", mb, (1-p)*100);
        };
    }

    private double r(double v) { return Math.round(v * 1000.0) / 1000.0; }
    private Map<String, Double> roundMap(Map<String, Double> m) {
        Map<String, Double> r = new LinkedHashMap<>();
        m.forEach((k,v)->r.put(k,r(v))); return r;
    }
}