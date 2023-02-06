package ru.yandex.direct.web;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class PerlCsrfTokens {
    public static final String SECRET_KEY = "secret";

    public static final List<TokenMetadata> TOKENS = createTokens();

    /**
     * Тест {@link CSRFPerlCompatTest} проверяет, что значения csrf-токенов из Perl и Java совпадают,
     * поэтому ожидаемые значения надо генерировать Perl'ом:
     * <p>
     * {@code perl -I protected/ -I perl/settings/ -MCSRFCheck -E '$Settings::SECRET_PHRASE = "secret"; say CSRFCheck::get_csrf_token(1000, 1480321327)'}
     */
    private static List<TokenMetadata> createTokens() {
        long startTime = 1480321327L;
        return ImmutableList.copyOf(new TokenMetadata[]{
                t(0x0L, startTime, "i7pyuR2uofbTgZuW"),
                t(0x1L, startTime, "DxHbtR8GVzVXKjKa"),
                t(1000L, startTime, "b-PMr7TkKvo32CWA"),
                t(0x000000000000FFL, startTime, "igMsxZX2I8DSOMXq"),
                t(0x0000000000FFFFL, startTime, "sQ0uAURdhFTpNscu"),
                t(0x00000000FFFFFFL, startTime, "FP5FhWICAaRMxayq"),
                t(0x000000FFFFFFFFL, startTime, "63Uv06_enQuzTsb8"),
                t(0x0000FFFFFFFFFFL, startTime, "yb47WF-S-aqRhdJ3"),
                t(0x00FFFFFFFFFFFFL, startTime, "V6BowZ7pi_UPm4Hu"),
                t(0xFFFFFFFFFFFFFFL, startTime, "5ozetg8f2qu-tzeZ"),
                t(Long.MAX_VALUE, startTime, "CprFj7dLfexSoSyg"),
        });
    }

    private static TokenMetadata t(long uid, long issuingTime, String csrfToken) {
        return new TokenMetadata(uid, issuingTime, csrfToken);
    }

    static class TokenMetadata {
        public long getUid() {
            return uid;
        }

        public String getToken() {
            return token;
        }

        private final long uid;

        public long getIssuingTime() {
            return issuingTime;
        }

        private final long issuingTime;
        private final String token;

        TokenMetadata(long uid, long issuingTime, String token) {
            this.uid = uid;
            this.token = token;
            this.issuingTime = issuingTime;
        }
    }
}
