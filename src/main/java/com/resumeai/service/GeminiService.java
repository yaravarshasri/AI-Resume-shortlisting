package com.resumeai.service;


import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ Updated to the latest endpoint
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s";

    public List<String> extractSkillsFromJD(String jobDescription) {
        String prompt = "Extract only the list of required skills from the following job description:\n\n"
                + jobDescription
                + "\n\nReturn skills as a comma-separated list.";
        String response = askGemini(prompt);
        return parseSkillsFromResponse(response);
    }
    public CandidateInfo extractCandidateInfo(String resumeText) {
        String prompt = "Extract the following from the resume text below in plain text only, "
                + "without adding asterisks, bullet points, or markdown formatting. "
                + "Return exactly in this format:\n"
                + "Name: <full name>\n"
                + "Email: <email address>\n"
                + "Skills: <comma-separated skills>\n\n"
                + "Resume:\n"
                + resumeText;

        String response = askGemini(prompt);

        // Clean markdown formatting (remove **, *, extra spaces, etc.)
        response = response.replaceAll("\\*+", "").trim();

        String name = extractField(response, "name");
        String email = extractField(response, "email");
        List<String> skills = parseSkillsFromResponse(response);

        return new CandidateInfo(name, email, skills);
    }


    public SkillMatch calculateSkillMatch(List<String> jdSkills, List<String> candidateSkills) {
        Set<String> jd = jdSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());
        Set<String> candidate = candidateSkills.stream().map(String::toLowerCase).collect(Collectors.toSet());

        Set<String> matched = new HashSet<>(jd);
        matched.retainAll(candidate);

        double score = jd.isEmpty() ? 0.0 : ((double) matched.size() / jd.size()) * 100.0;

        return new SkillMatch(new ArrayList<>(matched), score);
    }

    private String askGemini(String prompt) {
        Map<String, Object> content = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(content, headers);

        String url = String.format(GEMINI_API_URL, geminiApiKey);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                JSONObject json = new JSONObject(response.getBody());
                JSONArray candidates = json.getJSONArray("candidates");
                JSONObject first = candidates.getJSONObject(0);
                JSONArray parts = first.getJSONObject("content").getJSONArray("parts");
                return parts.getJSONObject(0).getString("text").trim();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse Gemini response: " + response.getBody(), e);
            }
        } else {
            throw new RuntimeException("Failed to call Gemini API: " + response.getStatusCode());
        }
    }

    private String extractField(String response, String fieldName) {
        String[] lines = response.split("\n");
        for (String line : lines) {
            if (line.toLowerCase().contains(fieldName.toLowerCase())) {
                int idx = line.indexOf(":");
                if (idx != -1) {
                    return line.substring(idx + 1).trim();
                }
            }
        }
        return "";
    }

    private List<String> parseSkillsFromResponse(String response) {
        if (response.contains(":")) {
            response = response.substring(response.lastIndexOf(":") + 1);
        }
        return Arrays.stream(response.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static class CandidateInfo {
        private final String name;
        private final String email;
        private final List<String> skills;

        public CandidateInfo(String name, String email, List<String> skills) {
            this.name = name;
            this.email = email;
            this.skills = skills;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public List<String> getSkills() { return skills; }
    }

    public static class SkillMatch {
        private final List<String> matchedSkills;
        private final double score;

        public SkillMatch(List<String> matchedSkills, double score) {
            this.matchedSkills = matchedSkills;
            this.score = score;
        }

        public List<String> getMatchedSkills() { return matchedSkills; }
        public double getScore() { return score; }
    }
}
