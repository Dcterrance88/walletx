package com.walletx.accountservice.config.message;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

/**
 * Configuration for internationalization (i18n) message resolution.
 *
 * <p>Sets up the {@link MessageSource} to load messages from property files
 * located in {@code src/main/resources/i18n/}. The locale is determined
 * per request using the {@code Accept-Language} HTTP header.</p>
 *
 * <p>Supported locales:</p>
 * <ul>
 *     <li>{@code en} - English (default)</li>
 *     <li>{@code es} - Spanish</li>
 * </ul>
 */
@Configuration
public class MessageConfig implements WebMvcConfigurer {

    /**
     * Configures the {@link MessageSource} bean for loading i18n message bundles.
     *
     * <p>Resolves messages from the following files based on the active locale:</p>
     * <ul>
     *   <li>{@code i18n/messages.properties} — English (default)</li>
     *   <li>{@code i18n/messages_es.properties} — Spanish</li>
     * </ul>
     *
     * <p>{@code setDefaultLocale} is set to {@link Locale#ENGLISH} to ensure that
     * when the JVM default locale is not English, the fallback still resolves to
     * the English message bundle instead of the system locale.</p>
     *
     * <p>{@code useCodeAsDefaultMessage} is set to {@code true} so that if a message
     * key is not found, the key itself is returned instead of throwing an exception —
     * useful during development to identify missing translations.</p>
     *
     * @return a configured {@link ResourceBundleMessageSource}
     */
    @Bean(name = "messageSource")
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("i18n/messages", "ValidationMessages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultLocale(Locale.ENGLISH);
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

    /**
     * Configures the interceptor that detects locale change requests.
     *
     * <p>Works in conjunction with {@link #addInterceptors(InterceptorRegistry)}
     * to ensure the locale is properly resolved and set in the request context
     * before message resolution occurs.</p>
     *
     * @return a configured {@link LocaleChangeInterceptor}
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        return new LocaleChangeInterceptor();
    }

    /**
     * Registers the {@link LocaleChangeInterceptor} in the Spring MVC interceptor chain.
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

}
