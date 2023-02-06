package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.mobilecontent.model.OsType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.MobileAppStoreHrefParser.DEFAULT_LANG;
import static ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.MobileAppStoreHrefParser.DEFAULT_REGION;
import static ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.MobileAppStoreHrefParser.parseLang;
import static ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.MobileAppStoreHrefParser.parseRegion;

class MobileAppStoreHrefParserTests {

    private static List<Arguments> createLangTestData() {
        return List.of(
                Arguments.of(OsType.ANDROID, null, DEFAULT_LANG),
                Arguments.of(OsType.IOS, "", DEFAULT_LANG),
                Arguments.of(OsType.IOS, "https://apps.apple.com/ru/app/id1561640111", DEFAULT_LANG),
                Arguments.of(OsType.IOS, "https://apps.apple.com/ru/app/id1561640111?l=dasdasd", "dasdasd"),
                Arguments.of(OsType.IOS, "https://apps.apple.com/en/app/id1561640111?l1=xx&l2=yy&l=zz", "zz"),
                Arguments.of(OsType.IOS, "https://apps.apple.com/en/l=zz/id1561640111", DEFAULT_LANG),
                Arguments.of(OsType.IOS, "!@#$%^&*()", DEFAULT_LANG),
                Arguments.of(OsType.IOS, "https://apps.apple.com/app/id1", DEFAULT_LANG),
                Arguments.of(OsType.IOS, "https://itunes.apple.com/app/a", DEFAULT_LANG),
                Arguments.of(OsType.ANDROID, "https://play.google.com/store/apps?hl=xxx&gl=US", "xxx"),
                Arguments.of(OsType.ANDROID, "https://play.google.com/hl=xxx/store/apps?gl=US", DEFAULT_LANG),
                Arguments.of(OsType.ANDROID, "!@#$%^&*()", DEFAULT_LANG),
                Arguments.of(OsType.ANDROID, "https://play.google.com/store/apps/details?id=com.allgoritm.youla&referrer=utm_campaign%3D6540730%26utm_medium%3Dexternal%26utm_content%3D21926738%26utm_source%3D1link.mail.ru%26_1lpb_data%3DURLENCODE(2030_URLENCODE(clickId={$clickId}))", DEFAULT_LANG)
        );
    }

    private static List<Arguments> createRegionTestData() {
        return List.of(
                Arguments.of(OsType.ANDROID, null, DEFAULT_REGION),
                Arguments.of(OsType.IOS, "", DEFAULT_REGION),
                Arguments.of(OsType.IOS, "https://apps.apple.com/XXX/app/id1561640111", "xxx"),
                Arguments.of(OsType.IOS, "https://apps.apple.com//app/id1561640111?l=dasdasd", DEFAULT_REGION),
                Arguments.of(OsType.IOS, "https://apps.apple.com////yyy///app////id1346027678", "yyy"),
                Arguments.of(OsType.IOS, "!@#$%^&*()", DEFAULT_REGION),
                Arguments.of(OsType.IOS, "https://apps.apple.com/app/id1", DEFAULT_REGION),
                Arguments.of(OsType.IOS, "https://itunes.apple.com/app/a", DEFAULT_REGION),
                Arguments.of(OsType.ANDROID, "https://play.google.com/store/apps?hl=xxx&gl=yyy", "yyy"),
                Arguments.of(OsType.ANDROID, "https://play.google.com/gl=xxx/store/apps?hl=US", DEFAULT_REGION),
                Arguments.of(OsType.ANDROID, "!@#$%^&*()", DEFAULT_REGION),
                Arguments.of(OsType.ANDROID, "https://play.google.com/store/apps/details?id=com.allgoritm.youla&referrer=utm_campaign%3D6540730%26utm_medium%3Dexternal%26utm_content%3D21926738%26utm_source%3D1link.mail.ru%26_1lpb_data%3DURLENCODE(2030_URLENCODE(clickId={$clickId}))", DEFAULT_REGION)
        );
    }

    @ParameterizedTest
    @MethodSource("createLangTestData")
    void parseLangTest(OsType osType, String url, String expectedLang) {
        assertThat(parseLang(osType, url)).isEqualTo(expectedLang);
    }

    @ParameterizedTest
    @MethodSource("createRegionTestData")
    void parseRegionTest(OsType osType, String url, String expectedRegion) {
        assertThat(parseRegion(osType, url)).isEqualTo(expectedRegion);
    }
}
