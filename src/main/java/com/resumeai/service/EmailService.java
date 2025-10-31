package com.resumeai.service;


import com.resumeai.model.Candidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class for handling email operations
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Send email to candidate (shortlist OR rejection) based on threshold
     * @param candidate the candidate to send email to
     * @param threshold score threshold
     * @return true if email sent successfully, false otherwise
     */
    public boolean sendShortlistedMail(Candidate candidate, double threshold) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(candidate.getEmail());

            String emailBody;

            if (candidate.getMatchScore() >= threshold) {
                // Shortlisted 🎉
                message.setSubject("Resume Shortlisted");
                emailBody = String.format(
                        "Hi %s,\n\n" +
                                "Congratulations! Your resume has been shortlisted for our position.\n\n" +
                                "Your matching score: %.1f%%\n" +
                                "Matched skills: %s\n\n" +
                                "We'll contact you soon with next steps.\n\n" +
                                "Best regards,\nHR Team",
                        candidate.getName(),
                        candidate.getMatchScore(),
                        candidate.getMatchedSkills()
                );
            } else {
                // Rejected 🙏
                message.setSubject("Application Update - Thank You");
                emailBody = String.format(
                        "Hi %s,\n\n" +
                                "Thank you for applying. After evaluating your resume, " +
                                "your score was %.1f%%, which is below our shortlisting threshold of %.1f%%.\n\n" +
                                "Although you were not shortlisted this time, we truly appreciate your interest " +
                                "and encourage you to apply for future opportunities with us.\n\n" +
                                "Best regards,\nHR Team",
                        candidate.getName(),
                        candidate.getMatchScore(),
                        threshold
                );
            }

            message.setText(emailBody);
            mailSender.send(message);

            logger.info("Email (shortlist/rejection) sent successfully to: {}", candidate.getEmail());
            return true;

        } catch (Exception e) {
            logger.error("Failed to send email to: {}", candidate.getEmail(), e);
            return false;
        }
    }

    /**
     * Validate email configuration
     * @return true if email configuration is valid
     */
    public boolean isEmailConfigured() {
        try {
            return fromEmail != null && !fromEmail.trim().isEmpty() && mailSender != null;
        } catch (Exception e) {
            logger.error("Email configuration validation failed", e);
            return false;
        }
    }
}
