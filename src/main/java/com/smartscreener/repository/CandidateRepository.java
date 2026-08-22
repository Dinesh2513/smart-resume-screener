package com.smartscreener.repository;

import com.smartscreener.entity.Candidate;
import com.smartscreener.entity.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    List<Candidate> findAllByOrderByCreatedAtDesc();

    List<Candidate> findAllByOrderByMatchScoreDesc();

    List<Candidate> findByStatusOrderByMatchScoreDesc(
            CandidateStatus status
    );
}