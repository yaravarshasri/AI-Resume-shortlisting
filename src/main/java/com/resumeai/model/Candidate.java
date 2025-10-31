package com.resumeai.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a candidate's resume screening results
 */
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    @Column(nullable = false)
    private Double matchScore;

    @Column(nullable = false)
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private Boolean emailSent = false;

    // Default constructor
    public Candidate() {
        this.processedAt = LocalDateTime.now();
    }

    // Constructor with essential fields
    public Candidate(String name, String email, String skills, String matchedSkills, Double matchScore) {
        this.name = name;
        this.email = email;
        this.skills = skills;
        this.matchedSkills = matchedSkills;
        this.matchScore = matchScore;
        this.processedAt = LocalDateTime.now();
        this.emailSent = false;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public String getMatchedSkills() {
        return matchedSkills;
    }

    public void setMatchedSkills(String matchedSkills) {
        this.matchedSkills = matchedSkills;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Boolean getEmailSent() {
        return emailSent;
    }

    public void setEmailSent(Boolean emailSent) {
        this.emailSent = emailSent;
    }
}