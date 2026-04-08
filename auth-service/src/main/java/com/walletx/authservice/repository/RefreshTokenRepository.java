package com.walletx.authservice.repository;

import com.walletx.authservice.domain.entity.RefreshToken;
import com.walletx.authservice.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findAllByUserAndIsRevokedFalse(User user);
}
