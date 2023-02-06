package ru.yandex.direct.libs.mirrortools.utils;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.params.provider.Arguments.arguments;
import static ru.yandex.direct.libs.mirrortools.utils.HostingsHandler.getDomainLevel;

class HostingsHandlergetDomainLevelTest {
    public static Stream<Arguments> params() {
        return Stream.of(
                arguments("www.5ballov.ru", 3),
                arguments("5ballov.ru", 2),
                arguments("asdf.ru", 2),
                arguments("asdf.ru.", 2),
                arguments("https://www.twoooo.zyandex.ru", 4),
                arguments("https://www.abcde.com/home/params?a=b", 3),
                arguments("53456.455645.54545.765656.003.ru", 6),
                arguments("ФФФ.ффф", 2),
                arguments("Рус.протечкинет.рф", 3)
        );
    }

    @ParameterizedTest(name = "domain: {0}, level: {1}")
    @MethodSource("params")
    void testIsNthLevelDomain(String domain, int level) {
        Assertions.assertThat(getDomainLevel(domain)).isEqualTo(level);
    }
}
