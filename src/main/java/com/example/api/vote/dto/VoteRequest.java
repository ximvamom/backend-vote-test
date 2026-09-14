package com.example.api.vote.dto;

import com.example.api.vote.entity.VoteChoice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotBlank(message = "voterId는 필수입니다.")
        String voterId,

        @NotNull(message = "choice는 필수입니다.")
        VoteChoice choice
) {
}
