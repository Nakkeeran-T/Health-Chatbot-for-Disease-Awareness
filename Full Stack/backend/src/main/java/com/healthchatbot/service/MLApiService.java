package com.healthchatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * HTTP client that calls the Python Flask ML API (port 5001).
 * All methods gracefully return null if the ML API is unreachable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MLApiService {

    private final RestTemplate restTemplate;

    @Value("${ml.api.url:http://localhost:5001}")
    private String mlApiUrl;

    // ─── Inner DTOs ───────────────────────────────────────────────────────────

    public record ChatResult(
            String response,
            String intent,
            String language,
            String languageName,
            double confidence,
            double langConfidence,
            List<String> symptomsFound,
            Map<String, Object> diseasePrediction,
            List<String> suggestions) {
    }

    public record IntentResult(String intent, double confidence, List<String> suggestions) {
    }

    public record LanguageResult(String language, String languageName, double confidence) {
    }

    public record DiseaseResult(
            String disease, double confidence,
            String prevention, String treatment, String helpline,
            List<Map<String, Object>> topDiseases) {
    }

    public record ImagePredictionResult(
            String disease, double confidence,
            String prevention, String treatment, String helpline) {
    }

    // ─── /chat (all-in-one) ───────────────────────────────────────────────────

    /**
     * Calls /chat endpoint — detects language, intent, extracts symptoms,
     * predicts disease and returns a multilingual response in one shot.
     *
     * @return ChatResult or null if the ML API is unavailable
     */
    public ChatResult chat(String message, String languageOverride) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("message", message);
            if (languageOverride != null) {
                body.put("language", languageOverride);
            }

            ResponseEntity<Map> response = post("/chat", body);
            if (response == null || response.getBody() == null)
                return null;

            Map<?, ?> r = response.getBody();
            return new ChatResult(
                    str(r, "response"),
                    str(r, "intent"),
                    str(r, "language"),
                    str(r, "language_name"),
                    dbl(r, "confidence"),
                    dbl(r, "lang_confidence"),
                    list(r, "symptoms_found"),
                    (Map<String, Object>) r.get("disease_prediction"),
                    list(r, "suggestions"));
        } catch (Exception e) {
            log.warn("ML API /chat failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── /predict/intent ─────────────────────────────────────────────────────

    public IntentResult predictIntent(String message) {
        try {
            Map<String, Object> body = Map.of("message", message);
            ResponseEntity<Map> response = post("/predict/intent", body);
            if (response == null || response.getBody() == null)
                return null;
            Map<?, ?> r = response.getBody();
            return new IntentResult(str(r, "intent"), dbl(r, "confidence"), list(r, "suggestions"));
        } catch (Exception e) {
            log.warn("ML API /predict/intent failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── /detect/language ────────────────────────────────────────────────────

    public LanguageResult detectLanguage(String text) {
        try {
            Map<String, Object> body = Map.of("text", text);
            ResponseEntity<Map> response = post("/detect/language", body);
            if (response == null || response.getBody() == null)
                return null;
            Map<?, ?> r = response.getBody();
            return new LanguageResult(str(r, "language"), str(r, "language_name"), dbl(r, "confidence"));
        } catch (Exception e) {
            log.warn("ML API /detect/language failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── /predict/disease ────────────────────────────────────────────────────

    public DiseaseResult predictDisease(List<String> symptoms) {
        try {
            Map<String, Object> body = Map.of("symptoms", symptoms);
            ResponseEntity<Map> response = post("/predict/disease", body);
            if (response == null || response.getBody() == null)
                return null;
            Map<?, ?> r = response.getBody();
            return new DiseaseResult(
                    str(r, "disease"),
                    dbl(r, "confidence"),
                    str(r, "prevention"),
                    str(r, "treatment"),
                    str(r, "helpline"),
                    (List<Map<String, Object>>) r.get("top_diseases"));
        } catch (Exception e) {
            log.warn("ML API /predict/disease failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── /predict_image ──────────────────────────────────────────────────────

    public ImagePredictionResult predictImage(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(mlApiUrl + "/predict_image", requestEntity, Map.class);
            
            if (response == null || response.getBody() == null)
                return null;
            
            Map<?, ?> r = response.getBody();
            if (r.containsKey("error")) {
                log.warn("ML API /predict_image returned error: {}", r.get("error"));
                return null;
            }

            return new ImagePredictionResult(
                    str(r, "disease"),
                    dbl(r, "confidence"),
                    str(r, "prevention"),
                    str(r, "treatment"),
                    str(r, "helpline"));
        } catch (Exception e) {
            log.warn("ML API /predict_image failed: {}", e.getMessage());
            return null;
        }
    }

    // ─── /extract/symptoms ───────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public List<String> extractSymptoms(String text) {
        try {
            Map<String, Object> body = Map.of("text", text);
            ResponseEntity<Map> response = post("/extract/symptoms", body);
            if (response == null || response.getBody() == null)
                return List.of();
            return list(response.getBody(), "symptoms_found");
        } catch (Exception e) {
            log.warn("ML API /extract/symptoms failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── /health ─────────────────────────────────────────────────────────────

    public boolean isHealthy() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    mlApiUrl + "/health", Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(mlApiUrl + path, entity, Map.class);
    }

    private String str(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v == null ? "" : v.toString();
    }

    private double dbl(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v == null)
            return 0.0;
        return ((Number) v).doubleValue();
    }

    @SuppressWarnings("unchecked")
    private List<String> list(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v instanceof List)
            return (List<String>) v;
        return List.of();
    }
}
