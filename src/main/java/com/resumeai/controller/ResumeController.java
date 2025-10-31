package com.resumeai.controller;

import com.resumeai.model.Candidate;
import com.resumeai.service.ResumeService;
import com.resumeai.service.CSVService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Main controller for resume screening application
 */
@Controller
public class ResumeController {


    private static final Logger logger = LoggerFactory.getLogger(ResumeController.class);

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private CSVService csvService;

    /**
     * Display the main upload form
     */
    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("stats", resumeService.getCandidateStats());
        return "index";
    }


    /**
     * Handle resume upload and processing
     */
    @PostMapping("/upload")
    public String uploadResumes(
            @RequestParam("jobDescription") String jobDescription,
            @RequestParam("resumeFiles") MultipartFile[] resumeFiles,
            RedirectAttributes redirectAttributes) {

        logger.info("Received upload request with {} resume files", resumeFiles.length);

        try {
            // Validate input
            if (jobDescription == null || jobDescription.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Job description is required");
                return "redirect:/";
            }

            if (resumeFiles.length == 0 || (resumeFiles.length == 1 && resumeFiles[0].isEmpty())) {
                redirectAttributes.addFlashAttribute("error", "At least one resume file is required");
                return "redirect:/";
            }

            // Process resumes
            List<Candidate> candidates = resumeService.processResumes(jobDescription, resumeFiles);

            if (candidates.isEmpty()) {
                redirectAttributes.addFlashAttribute("warning",
                        "No candidates could be processed. Please check if the PDF files are valid and contain readable text.");
                return "redirect:/";
            }

            redirectAttributes.addFlashAttribute("success",
                    String.format("Successfully processed %d resumes", candidates.size()));

            return "redirect:/results";

        } catch (Exception e) {
            logger.error("Error processing resumes", e);
            redirectAttributes.addFlashAttribute("error",
                    "An error occurred while processing resumes: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * Display results page with candidate rankings
     */
    @GetMapping("/results")
    public String showResults(Model model) {
        try {
            List<Candidate> candidates = resumeService.getAllCandidatesRanked();
            ResumeService.CandidateStats stats = resumeService.getCandidateStats();

            model.addAttribute("candidates", candidates);
            model.addAttribute("stats", stats);

            if (candidates.isEmpty()) {
                model.addAttribute("message", "No candidates found. Please upload some resumes first.");
            }

            return "results";

        } catch (Exception e) {
            logger.error("Error loading results", e);
            model.addAttribute("error", "Error loading results: " + e.getMessage());
            return "results";
        }
    }

    /**
     * Download candidate rankings as CSV
     */
    @GetMapping("/download-csv")
    public ResponseEntity<byte[]> downloadCSV() {
        try {
            List<Candidate> candidates = resumeService.getAllCandidatesRanked();

            if (candidates.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ResumeService.CandidateStats stats = resumeService.getCandidateStats();
            String csvContent = csvService.generateCandidateCSVWithStats(candidates, stats);
            String filename = csvService.generateCSVFilename();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);

            logger.info("CSV download requested. Generated file: {}", filename);

            return new ResponseEntity<>(
                    csvContent.getBytes(StandardCharsets.UTF_8),
                    headers,
                    HttpStatus.OK
            );

        } catch (Exception e) {
            logger.error("Error generating CSV download", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Clear all candidate data
     */
    @PostMapping("/clear")
    public String clearData(RedirectAttributes redirectAttributes) {
        try {
            resumeService.clearAllCandidates();
            redirectAttributes.addFlashAttribute("success", "All candidate data cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing data", e);
            redirectAttributes.addFlashAttribute("error", "Error clearing data: " + e.getMessage());
        }
        return "redirect:/";
    }

    /**
     * Show candidate details (REST endpoint)
     */
    @GetMapping("/api/candidate/{id}")
    @ResponseBody
    public ResponseEntity<Candidate> getCandidateDetails(@PathVariable Long id) {
        try {
            return resumeService.getAllCandidatesRanked()
                    .stream()
                    .filter(c -> c.getId().equals(id))
                    .findFirst()
                    .map(candidate -> ResponseEntity.ok(candidate))
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            logger.error("Error fetching candidate details for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get application statistics (REST endpoint)
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<ResumeService.CandidateStats> getStats() {
        try {
            ResumeService.CandidateStats stats = resumeService.getCandidateStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error fetching statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Resume Screening AI is running");
    }
    @PostConstruct
    public void init() {
        System.out.println("✅ ResumeController bean loaded successfully.");
    }
}