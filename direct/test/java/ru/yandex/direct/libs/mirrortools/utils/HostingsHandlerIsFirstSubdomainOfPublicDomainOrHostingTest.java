package ru.yandex.direct.libs.mirrortools.utils;

import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.libs.mirrortools.MirrorToolsConfig;

import static org.junit.jupiter.params.provider.Arguments.arguments;

class HostingsHandlerIsFirstSubdomainOfPublicDomainOrHostingTest {
    private static final MirrorToolsConfig CONFIG = new MirrorToolsConfig();
    private static final HostingsHandler HOSTINGS_HANDLER =
            new HostingsHandler(CONFIG.getHostings(), CONFIG.getPublicSecondLevelDomains());

    public static Stream<Arguments> params() {
        return Stream.of(
                arguments("www.5ballov.ru", false),
                arguments("5ballov.co.ru", true),
                arguments("asdf.msk.ru", true),
                arguments("asdf.msk", false),
                arguments("www.asdf.msk.ru", true),
                arguments("www.msk.ru", true),
                arguments("https://www.twoooo.zyandex.ru", false),
                arguments("https://www.abcde.com.tr/home/params?a=b", true),
                arguments("asdf.globalmarket.com.ua", true),
                arguments("www.asdf.globalmarket.com.ua", true),
                arguments("53456.7910.org", true),
                arguments("53456.455645.54545.765656.7910.org", false),
                arguments("aaaa.boxmail.su", true),
                arguments("my.aaaa.boxmail.su", false));
    }

    @ParameterizedTest(name = "domain: {0}, expectedAnswer: {1}")
    @MethodSource("params")
    void testIsFirstSubdomainOfPublicDomainOrHosting(String domain, boolean expectedAnswer) {
        Assertions.assertThat(HOSTINGS_HANDLER.isFirstSubdomainOfPublicDomainOrHosting(domain)).isEqualTo(expectedAnswer);
    }
}
