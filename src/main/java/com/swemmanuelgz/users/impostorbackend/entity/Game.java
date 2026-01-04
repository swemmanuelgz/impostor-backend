package com.swemmanuelgz.users.impostorbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "games")
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "room_code", nullable = false, length = 10)
    private String roomCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "word", length = 100)
    private String word;

    @Column(name = "max_players")
    @ColumnDefault("8")
    private Integer maxPlayers = 8;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "duration")
    @ColumnDefault("10")
    private Integer duration = 10;

    @Column(name = "started_at")
    private Instant startedAt;

}