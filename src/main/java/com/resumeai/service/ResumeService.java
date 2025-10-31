package com.resumeai.service;
import com.resumeai.model.Candidate;
import com.resumeai.repository.CandidateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Main service class for resume screening functionality
 */
@Service
public class ResumeService {

    private static final Logger logger = LoggerFactory.getLogger(ResumeService.class);

    // Email threshold for shortlisting
    public static final double EMAIL_THRESHOLD = 20.0;

    @Autowired
    private GeminiService OllamaService; // name can stay the same to avoid more changes

    @Autowired
    private PDFService pdfService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CandidateRepository candidateRepository;

    /**
     * Process job description and resumes to generate candidate rankings
     */
    public List<Candidate> processResumes(String jobDescription, MultipartFile[] resumeFiles) {
        logger.info("Starting resume processing with {} resume files", resumeFiles.length);

        // Extract skills from job description using GEMINI
        List<String> jdSkills = OllamaService.extractSkillsFromJD(jobDescription);
        logger.info("Extracted {} skills from job description: {}", jdSkills.size(), jdSkills);

        List<Candidate> candidates = new ArrayList<>();

        for (MultipartFile resumeFile : resumeFiles) {
            try {
                if (resumeFile.isEmpty()) {
                    logger.warn("Skipping empty resume file");
                    continue;
                }

                logger.info("Processing resume: {}", resumeFile.getOriginalFilename());

                // Validate PDF
                pdfService.validatePDFFile(resumeFile);

                // Extract text from PDF
                String resumeText = pdfService.extractTextFromPDF(resumeFile);

                if (resumeText.isEmpty()) {
                    logger.warn("No text extracted from resume: {}", resumeFile.getOriginalFilename());
                    continue;
                }

                // Extract candidate info using Ollama
                GeminiService.CandidateInfo candidateInfo = OllamaService.extractCandidateInfo(resumeText);

                if (candidateInfo.getName().isEmpty() || candidateInfo.getEmail().isEmpty()) {
                    logger.warn("Could not extract name/email from: {}", resumeFile.getOriginalFilename());
                    continue;
                }

                // Match skills
                GeminiService.SkillMatch skillMatch = OllamaService.calculateSkillMatch(jdSkills, candidateInfo.getSkills());

                Candidate candidate = new Candidate(
                        candidateInfo.getName(),
                        candidateInfo.getEmail(),
                        String.join(", ", candidateInfo.getSkills()),
                        String.join(", ", skillMatch.getMatchedSkills()),
                        skillMatch.getScore()
                );

                candidate = candidateRepository.save(candidate);
                candidates.add(candidate);

                logger.info("Processed candidate: {} - Score: {}%", candidate.getName(), candidate.getMatchScore());

            } catch (Exception e) {
                logger.error("Error processing resume: {}", resumeFile.getOriginalFilename(), e);
            }
        }

        // Send emails to ALL candidates (shortlist + rejection)
        sendEmailsToAllCandidates(candidates);

        // Sort by match score descending
        candidates.sort((c1, c2) -> Double.compare(c2.getMatchScore(), c1.getMatchScore()));
        logger.info("Finished processing {} candidates", candidates.size());

        return candidates;
    }

    private void sendEmailsToAllCandidates(List<Candidate> candidates) {
        if (candidates.isEmpty()) {
            logger.info("No candidates to send emails.");
            return;
        }

        if (!emailService.isEmailConfigured()) {
            logger.warn("Email config missing. Skipping email notifications.");
            return;
        }

        logger.info("Sending emails to {} candidates (shortlisted + rejected)", candidates.size());

        for (Candidate candidate : candidates) {
            boolean sent = emailService.sendShortlistedMail(candidate, EMAIL_THRESHOLD);
            candidate.setEmailSent(sent);
            candidateRepository.save(candidate);
        }
    }

    public List<Candidate> getAllCandidatesRanked() {
        return candidateRepository.findAllByOrderByMatchScoreDesc();
    }

    public List<Candidate> getQualifiedCandidates() {
        return candidateRepository.findByMatchScoreGreaterThanEqual(EMAIL_THRESHOLD);
    }

    public void clearAllCandidates() {
        candidateRepository.deleteAll();
        logger.info("All candidate data cleared.");
    }

    public CandidateStats getCandidateStats() {
        List<Candidate> all = candidateRepository.findAll();
        long total = all.size();
        long qualified = all.stream().filter(c -> c.getMatchScore() >= EMAIL_THRESHOLD).count();
        long emailsSent = all.stream().filter(Candidate::getEmailSent).count();
        double avg = all.stream().mapToDouble(Candidate::getMatchScore).average().orElse(0.0);

        return new CandidateStats(total, qualified, emailsSent, avg);
    }

    public static class CandidateStats {
        private final long totalCandidates;
        private final long qualifiedCandidates;
        private final long emailsSent;
        private final double averageScore;

        public CandidateStats(long total, long qualified, long emailsSent, double avg) {
            this.totalCandidates = total;
            this.qualifiedCandidates = qualified;
            this.emailsSent = emailsSent;
            this.averageScore = avg;
        }

        public long getTotalCandidates() { return totalCandidates; }
        public long getQualifiedCandidates() { return qualifiedCandidates; }
        public long getEmailsSent() { return emailsSent; }
        public double getAverageScore() { return averageScore; }
    }
}
