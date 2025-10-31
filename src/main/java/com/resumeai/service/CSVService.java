package com.resumeai.service;

import com.opencsv.CSVWriter;
import com.resumeai.model.Candidate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service class for CSV export functionality
 */
@Service
public class CSVService {

    private static final Logger logger = LoggerFactory.getLogger(CSVService.class);

    /**
     * Generate CSV content from candidate list
     * @param candidates list of candidates to export
     * @return CSV content as string
     */
    public String generateCandidateCSV(List<Candidate> candidates) {
        logger.info("Generating CSV for {} candidates", candidates.size());

        StringWriter stringWriter = new StringWriter();

        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Write CSV header
            String[] header = {
                    "Rank",
                    "Name",
                    "Email",
                    "Match Score (%)",
                    "Skills",
                    "Matched Skills",
                    "Email Sent",
                    "Processed Date"
            };
            csvWriter.writeNext(header);

            // Write candidate data
            int rank = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Candidate candidate : candidates) {
                String[] row = {
                        String.valueOf(rank++),
                        candidate.getName() != null ? candidate.getName() : "",
                        candidate.getEmail() != null ? candidate.getEmail() : "",
                        String.format("%.1f", candidate.getMatchScore()),
                        candidate.getSkills() != null ? candidate.getSkills() : "",
                        candidate.getMatchedSkills() != null ? candidate.getMatchedSkills() : "",
                        candidate.getEmailSent() ? "Yes" : "No",
                        candidate.getProcessedAt().format(dateFormatter)
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();

        } catch (Exception e) {
            logger.error("Error generating CSV", e);
            throw new RuntimeException("Failed to generate CSV: " + e.getMessage());
        }

        String csvContent = stringWriter.toString();
        logger.info("CSV generation completed. Generated {} lines", candidates.size() + 1);

        return csvContent;
    }

    /**
     * Generate filename for CSV export
     * @return formatted filename with timestamp
     */
    public String generateCSVFilename() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String timestamp = java.time.LocalDateTime.now().format(formatter);
        return "candidate_rankings_" + timestamp + ".csv";
    }

    /**
     * Generate CSV content with statistics summary
     * @param candidates list of candidates
     * @param stats candidate statistics
     * @return CSV content with summary
     */
    public String generateCandidateCSVWithStats(List<Candidate> candidates, ResumeService.CandidateStats stats) {
        logger.info("Generating CSV with statistics for {} candidates", candidates.size());

        StringWriter stringWriter = new StringWriter();

        try (CSVWriter csvWriter = new CSVWriter(stringWriter)) {

            // Write summary statistics first
            csvWriter.writeNext(new String[]{"RESUME SCREENING SUMMARY"});
            csvWriter.writeNext(new String[]{"Total Candidates", String.valueOf(stats.getTotalCandidates())});
            csvWriter.writeNext(new String[]{"Qualified Candidates (≥60%)", String.valueOf(stats.getQualifiedCandidates())});
            csvWriter.writeNext(new String[]{"Emails Sent", String.valueOf(stats.getEmailsSent())});
            csvWriter.writeNext(new String[]{"Average Score", String.format("%.1f%%", stats.getAverageScore())});
            csvWriter.writeNext(new String[]{"Generated On", java.time.LocalDateTime.now().toString()});
            csvWriter.writeNext(new String[]{""}); // Empty row for separation

            // Write candidate data header
            String[] header = {
                    "Rank",
                    "Name",
                    "Email",
                    "Match Score (%)",
                    "Skills",
                    "Matched Skills",
                    "Email Sent",
                    "Processed Date"
            };
            csvWriter.writeNext(header);

            // Write candidate data
            int rank = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Candidate candidate : candidates) {
                String[] row = {
                        String.valueOf(rank++),
                        candidate.getName() != null ? candidate.getName() : "",
                        candidate.getEmail() != null ? candidate.getEmail() : "",
                        String.format("%.1f", candidate.getMatchScore()),
                        candidate.getSkills() != null ? candidate.getSkills() : "",
                        candidate.getMatchedSkills() != null ? candidate.getMatchedSkills() : "",
                        candidate.getEmailSent() ? "Yes" : "No",
                        candidate.getProcessedAt().format(dateFormatter)
                };
                csvWriter.writeNext(row);
            }

            csvWriter.flush();

        } catch (Exception e) {
            logger.error("Error generating CSV with stats", e);
            throw new RuntimeException("Failed to generate CSV with stats: " + e.getMessage());
        }

        String csvContent = stringWriter.toString();
        logger.info("CSV with stats generation completed");

        return csvContent;
    }
}