package com.walletx.authservice.config.message;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Configuration for internationalization (i18n) message resolution.
 *
 * <p>Sets up the {@link MessageSource} to load messages from property files
 * localed in {@code src/main/resources/i18n/}. the locale is determined
 * per request using the {@code Accept-Language} HTTP header.</p>
 *
 * <p>Supported locales:</p>
 * <ul>
 *     <li>{@code en} - English (default)</li>
 *     <li>{@code es} - Spanish</li>
 * </ul>
 *
 */
@Configuration
public class MessageConfig {

    /**
     * Configures the {@link MessageSource} bean for loading i18n message bundles.
     *
     * <p>Resolves messages from the following files based on the active locale:</p>
     * <ul>
     *   <li>{@code i18n/messages.properties} — English (default)</li>
     *   <li>{@code i18n/messages_es.properties} — Spanish</li>
     * </ul>
     *
     * <p>{@code useCodeAsDefaultMessage} is set to {@code true} so that if a message
     * key is not found, the key itself is returned instead of throwing an exception —
     * useful during development to identify missing translations.</p>
     *
     * @return a configured {@link ResourceBundleMessageSource}
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages", "ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        return messageSource;
    }

    /**
     * Configures the locale resolver that reads the {@code Accept-Language} header
     * from each HTTP request to determine the response language.
     *
     * <p>If the requested locale is not supported or the header is absent,
     * English is used as the default locale.</p>
     *
     * @return a configured {@link AcceptHeaderLocaleResolver}
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of(
                Locale.ENGLISH,
                new Locale("es")
        ));
        return resolver;
    }

}
