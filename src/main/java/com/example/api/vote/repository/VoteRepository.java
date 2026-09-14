package com.example.api.vote.repository;

import com.example.api.vote.entity.Vote;
import com.example.api.vote.entity.VoteChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    long countByChoice(VoteChoice choice);

}
