package com.walletx.authservice.utils.message;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    public String getMessage(String key) {
        return messageSource.getMessage(
                key,
                null,
                LocaleContextHolder.getLocale()
        );
    }

    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(
                key,
                args,
                LocaleContextHolder.getLocale()
        );
    }

}
