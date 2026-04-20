package com.walletx.accountservice.repository;

import com.walletx.accountservice.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByAccountNumber(String accountNumber);
}
