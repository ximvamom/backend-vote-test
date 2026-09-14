package com.example.api.vote.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "vote",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vote_voter_id", columnNames = "voter_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voter_id", nullable = false, unique = true)
    private String voterId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoteChoice choice;

    private Vote(String voterId, VoteChoice choice) {
        this.voterId = voterId;
        this.choice = choice;
    }

    public static Vote create(String voterId, VoteChoice choice) {
        return new Vote(voterId, choice);
    }
}