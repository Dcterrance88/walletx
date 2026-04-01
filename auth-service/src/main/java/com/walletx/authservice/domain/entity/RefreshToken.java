package com.walletx.authservice.domain.entity;

import com.walletx.authservice.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 255)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    private Boolean isRevoked;


    @PrePersist
    protected void onCreate() {
        this.isRevoked = false;
    }

    @Override
    public String toString() {
        return "RefreshToken{" +
                "id=" + id +
                ", isRevoked=" + isRevoked +
                ", expiresAt=" + expiresAt +
                '}';
    }
}
