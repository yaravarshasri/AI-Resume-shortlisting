package com.resumeai.repository;


import com.resumeai.model.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for Candidate entity
 */
@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    /**
     * Find all candidates ordered by match score in descending order
     * @return List of candidates sorted by score
     */
    @Query("SELECT c FROM Candidate c ORDER BY c.matchScore DESC")
    List<Candidate> findAllByOrderByMatchScoreDesc();

    /**
     * Find candidates with score >= threshold
     * @param threshold minimum score threshold
     * @return List of qualified candidates
     */
    @Query("SELECT c FROM Candidate c WHERE c.matchScore >= ?1")
    List<Candidate> findByMatchScoreGreaterThanEqual(Double threshold);

    /**
     * Find candidates who haven't been sent emails yet
     * @return List of candidates pending email notification
     */
    List<Candidate> findByEmailSentFalse();
}