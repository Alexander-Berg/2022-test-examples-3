package ru.yandex.market.fintech.creditbroker.system.log.sanitizer;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;
import static ru.yandex.market.fintech.creditbroker.helper.ResourceLoader.loadResourceAsString;

class LogSanitizerTest {
    private LogSanitizer logSanitizer;

    @BeforeEach
    void setup() {
        logSanitizer = LogSanitizerFactory.newLogSanitizer();
    }

    @Test
    void sanitizeBlackboxLogs() {
        String source = loadResourceAsString("system/log/sanitizer/blackbox-log.txt");
        String sanitized = logSanitizer.sanitize(source);

        assertStringDoesNotContainSubstrings(sanitized, "Иван", "Иванов", "1988-06-17", "user-test@ya.ru",
                "user-test@yandex.by", "user-test@yandex.com", "user-test@yandex.kz", "user-test@yandex.ru",
                "user-test@gmail.com", "+79253255550");
    }

    @Test
    void sanitizePassportDocumentsLogs() {
        String source = loadResourceAsString("system/log/sanitizer/passport-documents-log.txt");
        String sanitized = logSanitizer.sanitize(source);

        assertStringDoesNotContainSubstrings(sanitized, "4004205550", "Иван", "Иванов", "Иванович",
                "город Москва", "1988-06-01", "2021-01-13", "Отделением ОФМС Ниичаво Республики Карелия", "500312",
                "1999-07-11", "Долгопрудный", "Институтский переулок", "100500", "корп 2",
                "\"registration_apartment\":\"10\"", "4505123849", "2015-07-11",
                "Отдел ОУФМС Лукоморье Мытищинского района", "300200", "г. Москва, Тверская улица, дом 13");
    }

    @Test
    void sanitizePassportAddressLogs() {
        String source = loadResourceAsString("system/log/sanitizer/passport-address-log.txt");
        String sanitized = logSanitizer.sanitize(source);

        assertStringDoesNotContainSubstrings(sanitized, "\"building\":\"2-10соор6\"",
                "\"country\":\"Россия\"", "\"city\":\"Москва\"", "\"locality\":\"Москва\"",
                "\"latitude\":55.7538268275671", "\"longitude\":37.61594015938264",
                "\"region\":\"Москва и Московская область\"", "\"street\":\"Манежная улица\"",
                "\"zip\":\"119019\"");
    }

    @Test
    void sanitizePassportPhoneLogs() {
        String source = loadResourceAsString("system/log/sanitizer/passport-phone-log.txt");
        String sanitized = logSanitizer.sanitize(source);

        assertStringDoesNotContainSubstrings(sanitized, "number=%2B79090082211",
                "\"e164\": \"+79090082211\"", "\"international\": \"+7 909 008-22-11\"",
                "\"original\": \"+79090082211\"");
    }

    private void assertStringDoesNotContainSubstrings(String text, String... substrings) {
        Set<String> existingSubstrings = new HashSet<>();
        for (String substring : substrings) {
            if (text.contains(substring)) {
                existingSubstrings.add(substring);
            }
        }

        if (!existingSubstrings.isEmpty()) {
            fail(String.format("Text contains substrings: %s", existingSubstrings));
        }
    }
}
