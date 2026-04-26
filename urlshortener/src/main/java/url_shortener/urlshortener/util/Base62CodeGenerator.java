package url_shortener.urlshortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates Base62 short codes using a cryptographically-secure random source.
 * Character set: [0-9 A-Z a-z] → 62 symbols → 62^7 ≈ 3.5 trillion unique codes at length 7.
 */
@Component
public class Base62CodeGenerator {

    private static final String ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int DEFAULT_LENGTH = 7;
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        return generate(DEFAULT_LENGTH);
    }

    public String generate(int length) {
        if (length < 4 || length > 12) {
            throw new IllegalArgumentException("Code length must be between 4 and 12");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
