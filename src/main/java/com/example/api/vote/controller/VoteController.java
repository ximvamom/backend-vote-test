package com.example.api.vote.controller;

import com.example.api.vote.dto.VoteRequest;
import com.example.api.vote.dto.VoteResultResponse;
import com.example.api.vote.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping("/api/vote")
    public ResponseEntity<Void> vote(
            @Valid @RequestBody VoteRequest request
    ) {
        voteService.vote(request);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/api/result")
    public VoteResultResponse getResult() {
        return voteService.getResult();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
