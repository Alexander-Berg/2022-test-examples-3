package ru.yandex.direct.libs.mirrortools.utils;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.libs.mirrortools.MirrorToolsConfig;

import static org.junit.jupiter.params.provider.Arguments.arguments;

class HostingsHandlerStripWwwTest {
    private static final MirrorToolsConfig CONFIG = new MirrorToolsConfig();
    private static final HostingsHandler HOSTINGS_HANDLER =
            new HostingsHandler(CONFIG.getHostings(), CONFIG.getPublicSecondLevelDomains());


    public static List<Arguments> params() {
        List<Arguments> objects = new ArrayList<>();
        for (String hosting : CONFIG.getHostings()) {
            objects.add(arguments("http://www." + hosting, "www." + hosting));
            objects.add(arguments("www." + hosting, "www." + hosting));
            objects.add(arguments("www.qwe." + hosting, "qwe." + hosting));
            objects.add(arguments(hosting, hosting));
        }
        for (String publicSecondLevelDomain : CONFIG.getPublicSecondLevelDomains()) {
            objects.add(arguments("www.qwe." + publicSecondLevelDomain + ".eu",
                    "qwe." + publicSecondLevelDomain + ".eu"));
            objects.add(
                    arguments("www." + publicSecondLevelDomain + ".ru", "www." + publicSecondLevelDomain + ".ru"));
            objects.add(arguments("https://www." + publicSecondLevelDomain + ".org",
                    "www." + publicSecondLevelDomain + ".org"));
        }
        objects.addAll(List.of(
                arguments("www.5ballov.ru", "5ballov.ru"),
                arguments("5ballov.ru", "5ballov.ru"),
                arguments("www.ru", "www.ru"),
                arguments("www.yandex", "www.yandex"),
                arguments("www.zyandex.ru", "zyandex.ru"),
                arguments("https://www.zyandex.ru", "zyandex.ru"),
                arguments("www.leningrad.spb.ru", "leningrad.spb.ru"),
                arguments("https://www.abcde.com/home/params?a=b", "abcde.com"),
                arguments("www.003.ru", "003.ru"),
                arguments("www.ффф", "ффф"),
                arguments("www...", ".."),
                arguments("www.xxxa", "www.xxxa"),
                arguments("www.aaa.bbbb", "aaa.bbbb"),
                arguments("bbbb.cc", "bbbb.cc"),
                arguments("www.а-вагонка.рф", "а-вагонка.рф"),
                arguments("www.biz.io", "www.biz.io"),
                arguments("www.biz.дев", "biz.дев"),
                // stripWWW делает lowercase - фикисируем это поведение
                arguments("CAST.ru", "cast.ru"),
                arguments("www.CAST.ru", "cast.ru"))

        );
        return objects;
    }


    @ParameterizedTest(name = "domain: {0}")
    @MethodSource("params")
    void testStripWww(String domain, String strippedDomain) {
        Assertions.assertThat(HOSTINGS_HANDLER.stripWww(domain)).isEqualTo(strippedDomain);
    }
}
