package com.example.api.vote.service;

import com.example.api.vote.dto.VoteRequest;
import com.example.api.vote.dto.VoteResultResponse;
import com.example.api.vote.entity.Vote;
import com.example.api.vote.entity.VoteChoice;
import com.example.api.vote.repository.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;

    @Transactional
    public void vote(VoteRequest request) {

        Vote vote = Vote.create(
                request.voterId(),
                request.choice()
        );

        voteRepository.save(vote);
    }

    public VoteResultResponse getResult() {

        long jajang = voteRepository.countByChoice(VoteChoice.jajang);
        long jjamppong = voteRepository.countByChoice(VoteChoice.jjamppong);

        return new VoteResultResponse(
                jajang,
                jjamppong,
                jajang + jjamppong
        );
    }
}
