package com.walletx.authservice.security;
import com.walletx.authservice.config.security.SecurityConfig;
import com.walletx.authservice.domain.entity.User;
import com.walletx.common.exception.WalletXException;
import com.walletx.authservice.repository.UserRepository;
import com.walletx.authservice.utils.constant.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom implementation of {@link UserDetailsService} for the WalletX platform.
 *
 * <p>Loads user details from the database using the email address as the
 * unique identifier. Used by Spring Security during the authentication process.</p>
 *
 * @see JwtAuthenticationFilter
 * @see SecurityConfig
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address for Spring Security authentication.
     *
     * <p>Retrieves the user from the database and maps their role to
     * {@link GrantedAuthority} objects. The {@code isActive} flag is used
     * to determine whether the account is enabled - disabled accounts are
     * authomatically rejected by Spring Security without additional logic.</p>
     *
     * @param email the email address used as the username identifier
     * @return a fully populated {@link UserDetails} object with credentials and authorities
     * @throws UsernameNotFoundException if no user is found with the given email
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new WalletXException(
                        HttpStatus.UNAUTHORIZED,
                        Constants.MessageKeys.USER_NOT_FOUND
                ));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive(),
                true,
                true,
                true,
                authorities
        );
    }

}
