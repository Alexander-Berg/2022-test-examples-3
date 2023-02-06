package ru.yandex.direct.libs.mirrortools.utils;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class HostingHandlerGetHostingTest {
    private static final List<String> KNOWN_HOSTINGS = List.of("known-hosting.by", "known-hosting.com");
    private static final List<String> PUBLIC_LEVEL_DOMAINS = List.of("ru", "com");
    private static final HostingsHandler HOSTINGS_HANDLER =
            new HostingsHandler(KNOWN_HOSTINGS, PUBLIC_LEVEL_DOMAINS);

    static Stream<Arguments> params() {
        return Stream.of(
                arguments("site.ru", "site.ru"),
                arguments("www.site.ru", "site.ru"),
                arguments("asdfadsf.site.ru", "site.ru"),
                arguments("asdf.asdfadsf.site.ru", "site.ru"),
                arguments("site.com.ru", "site.com.ru"),
                arguments("chtoto.known-hosting.by", "chtoto.known-hosting.by"),
                arguments("chtoto.known-hosting.com", "chtoto.known-hosting.com"),
                arguments("www.chtoto.known-hosting.by", "chtoto.known-hosting.by"),
                arguments("www.chtoto.tam.known-hosting.by", "tam.known-hosting.by"),
                arguments("xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai",
                        "xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"),
                arguments("3dlevel.xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai",
                        "xn-----6kcabb7cqcfqdlkailcltfcigj03a.xn--p1ai"),
                arguments("4thlevel.ucoz.com.ru", "ucoz.com.ru"),
                arguments("4thlevel.ucoz.ru.com", "ucoz.ru.com"),
                // для теста  net не является public second level domain
                arguments("4thlevel.ucoz.net.com", "net.com"),
                arguments("4thlevel.ucoz.domain.com", "domain.com"),

                arguments("ru", "ru"),
                arguments("com.ru", "com.ru"),
                arguments("", ""),

                arguments("site.ru:8080", "site.ru"),
                arguments("http://site.ru", "site.ru"),
                arguments("https://site.ru", "site.ru"),
                arguments("site.ru?param=1", "site.ru"),
                arguments("site.ru?param=1", "site.ru")
        );

    }

    @ParameterizedTest(name = "domain = {0}, stripped domain = {1}")
    @MethodSource("params")
    void test(String input, String expectedResult) {
        var res = HOSTINGS_HANDLER.getHosting(input);
        assertThat(res).isEqualTo(expectedResult);
    }
}
