package com.walletx.authservice.domain.mapper;

import com.walletx.authservice.domain.dto.response.UserResponse;
import com.walletx.authservice.domain.entity.User;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Mapper responsible for converting {@link User} entities to response DTOs.
 *
 * <p>Keeps the transformation logic isolated from the service layer,
 * ensuring each class has a single responsibility. The service layer
 * focuses on business logic while the mapper handles data shaping.</p>
 *
 * @see UserResponse
 * @see User
 */
@Component
public class UserMapper {

    /**
     * Converts a {@link User} entity to a {@link UserResponse} DTO.
     *
     * <p>Maps the user's roles from {@link com.walletx.authservice.domain.enums.RoleType}
     * enum values to their string representation for API exposure.</p>
     *
     * @param user the user entity to convert
     * @return a fully populated {@link UserResponse} DTO
     */
    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }

}
