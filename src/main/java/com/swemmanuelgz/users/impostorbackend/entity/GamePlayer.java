package com.swemmanuelgz.users.impostorbackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@Setter
@Entity
@Table(name = "game_players")
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ColumnDefault("0")
    @Column(name = "is_impostor")
    private Boolean isImpostor;

    @ColumnDefault("0")
    @Column(name = "is_winner")
    private Boolean isWinner;
    
    // ===== CAMPOS PARA SISTEMA DE VOTACIÓN =====
    
    @ColumnDefault("0")
    @Column(name = "has_voted")
    private Boolean hasVoted = false;
    
    @Column(name = "voted_for_id")
    private Long votedForId;
    
    @ColumnDefault("'ACTIVE'")
    @Column(name = "status", length = 20)
    private String status = "ACTIVE"; // ACTIVE, ELIMINATED, DISCONNECTED

}