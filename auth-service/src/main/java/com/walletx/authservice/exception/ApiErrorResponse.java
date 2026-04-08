package com.walletx.authservice.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {

    private String code;
    private String message;
    private String userMessage;
    private String path;
    private LocalDateTime timestamp;

}
