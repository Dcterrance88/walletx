package com.walletx.authservice.repository;

import com.walletx.authservice.domain.entity.Role;
import com.walletx.authservice.domain.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}
