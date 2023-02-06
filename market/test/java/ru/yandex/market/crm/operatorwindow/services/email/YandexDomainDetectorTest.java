package ru.yandex.market.crm.operatorwindow.services.email;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class YandexDomainDetectorTest {

    private static final YandexDomainDetector detector = new YandexDomainDetector();

    public static Iterable<Arguments> dataForTest() {
        return List.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("yandex.ru", true),
                Arguments.of("YANDEX.RU", true),
                Arguments.of("yandex.ua", true),
                Arguments.of("ya.ru", true),
                Arguments.of("yandex.by", true),
                Arguments.of("yandex.com", true),
                Arguments.of("yandex.kz", true),
                Arguments.of("  yandex.kz  ", true),
                Arguments.of("  yandex.something  ", false)
        );
    }

    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("dataForTest")
    public void test(String domain,
                     boolean isYandexDomainExpected) {
        boolean isYandexDomainActual = detector.isYandexDomain(domain);
        Assertions.assertEquals(isYandexDomainExpected, isYandexDomainActual);
    }
}
