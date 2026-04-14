package com.walletx.common.message;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Service for resolving internationalized messages from the configured
 * {@link MessageSource} within the WalletX platform.
 *
 * <p>Messages are resolved based on the active locale, which is determined
 * either from the {@link LocaleContextHolder} or from an explicitly provided
 * {@link Locale}. The latter is preferred in exception handlers where the
 * locale is extracted directly from the {@code Accept-Language} request header.</p>
 *
 * @see MessageSource
 */
@Component
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Resolves a message for the given key using the locale from the current
     * request context via {@link LocaleContextHolder}.
     *
     * @param key the message key to resolve
     * @return the resolved message string
     */
    public String getMessage(String key) {
        return messageSource.getMessage(
                key,
                null,
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * Resolves a message for the given key with dynamic arguments, using the
     * locale from the current request context via {@link LocaleContextHolder}.
     *
     * <p>Use this method when the message contains placeholders, for example:
     * {@code "Account {0} not found"}.</p>
     *
     * @param key  the message key to resolve
     * @param args the arguments to substitute into the message
     * @return the resolved message string with arguments applied
     */
    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(
                key,
                args,
                LocaleContextHolder.getLocale()
        );
    }

    /**
     * Resolves a message for the given key using an explicitly provided {@link Locale}.
     *
     * <p>Preferred over {@link #getMessage(String)} in exception handlers where
     * the locale is extracted directly from the {@code Accept-Language} header
     * of the incoming request, ensuring accurate language resolution regardless
     * of the thread-local context.</p>
     *
     * @param key    the message key to resolve
     * @param locale the locale to use for message resolution
     * @return the resolved message string
     */
    public String getMessage(String key, Locale locale) {
        return messageSource.getMessage(key, null, locale);
    }

}
