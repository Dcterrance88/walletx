package com.walletx.accountservice.utils.generator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class AccountNumberGenerator {

    private static final String ACCOUNT_NUMBER_PREFIX = "WLT-";
    private static final long MIN_ACCOUNT_NUMBER = 1_000_000L;
    private static final long ACCOUNT_NUMBER_RANGE = 9_000_000L;
    private static final SecureRandom random = new SecureRandom();

    public String generate(Predicate<String> alreadyExists) {
        String candidate;
        do {
            candidate = ACCOUNT_NUMBER_PREFIX + (MIN_ACCOUNT_NUMBER + (Math.abs(random.nextLong()) % ACCOUNT_NUMBER_RANGE));
        } while (alreadyExists.test(candidate));
        return candidate;
    }

}
