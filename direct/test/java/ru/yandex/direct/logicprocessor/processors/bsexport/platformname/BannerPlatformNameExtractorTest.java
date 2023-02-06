package ru.yandex.direct.logicprocessor.processors.bsexport.platformname;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.banner.model.Language;
import ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformName;
import ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformNameForm;
import ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformNameInput;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.model.Language.EN;
import static ru.yandex.direct.core.entity.banner.model.Language.RU_;
import static ru.yandex.direct.core.entity.banner.model.Language.TR;
import static ru.yandex.direct.core.entity.banner.model.Language.UNKNOWN;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.APPGALLERY_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.APPSTORE_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.CLASSIFIED_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.GAMES_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.GOOGLEPLAY_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.MAPS_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.MARKET_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.SPRAV_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.USLUGI_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.BannerPlatformNameConstants.ZEN_NAME;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformNameForm.POSTFIX_YA_FULL;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformNameForm.SIMPLE;
import static ru.yandex.direct.logicprocessor.processors.bsexport.platformname.model.BannerPlatformNameForm.SIMPLE_YA_FULL;

class BannerPlatformNameExtractorTest {


    private static Stream<Arguments> positive_arguments() {
        return Stream.of(
                Arguments.of("https://zen.yandex.ru/abc",
                        RU_, ZEN_NAME, POSTFIX_YA_FULL, "в Яндекс.Дзене"),
                Arguments.of("https://zen.yandex.ru/abc",
                        null, ZEN_NAME, POSTFIX_YA_FULL, "в Яндекс.Дзене"),
                Arguments.of("https://zen.yandex.ru/abc",
                        UNKNOWN, ZEN_NAME, POSTFIX_YA_FULL, "в Яндекс.Дзене"),
                Arguments.of("https://maps.yandex.ru/abc",
                        RU_, MAPS_NAME, POSTFIX_YA_FULL, "на Яндекс.Картах"),
                Arguments.of("https://yandex.ru/sprav/",
                        EN, SPRAV_NAME, POSTFIX_YA_FULL, "on Yandex.Directory"),
                Arguments.of("https://yandex.ru/uslugi//",
                        EN, USLUGI_NAME, POSTFIX_YA_FULL, "on Yandex.Services"),
                Arguments.of("https://yandex.ru/o/",
                        EN, CLASSIFIED_NAME, POSTFIX_YA_FULL, "on Yandex.Classified"),
                Arguments.of("https://yandex.ru/market/",
                        RU_, MARKET_NAME, SIMPLE_YA_FULL, "Яндекс.Маркет"),
                Arguments.of("https://pokupki.market.yandex.ru/",
                        RU_, MARKET_NAME, SIMPLE_YA_FULL, "Яндекс.Маркет"),
                Arguments.of("http://itunes.apple.com/ru/",
                        EN, APPSTORE_NAME, SIMPLE, "App Store"),
                Arguments.of("https://apps.apple.com/ru/app/apple-store/id375380948",
                        EN, APPSTORE_NAME, SIMPLE, "App Store"),
                Arguments.of("https://play.google.com/store/apps/details",
                        RU_, GOOGLEPLAY_NAME, SIMPLE, "Google Play"),
                Arguments.of("https://appgallery.huawei.com/#/app/C101417879",
                        RU_, APPGALLERY_NAME, SIMPLE, "HUAWEI AppGallery"),
                Arguments.of("https://games.yandex.ru/abc",
                        RU_, GAMES_NAME, POSTFIX_YA_FULL, "на Яндекс.Играх")
        );
    }

    @ParameterizedTest
    @MethodSource("positive_arguments")
    void getBannerPlatformName_positive(String href,
                                        Language language,
                                        String name,
                                        BannerPlatformNameForm nameForm,
                                        String nameFormValue) {

        BannerPlatformName platformName = getPlatformName(href, language);
        assertThat(platformName.getName()).isEqualTo(name);
        assertThat(platformName.getForms()).containsEntry(nameForm, nameFormValue);
    }

    private static Stream<Arguments> negative_arguments() {
        return Stream.of(
                Arguments.of(null, RU_),
                Arguments.of("https://yandex.zen.ru/", RU_),
                Arguments.of("https://zen.ru/yandex", RU_),
                Arguments.of("https://zen.yandex.it/", RU_),
                Arguments.of("https://zen.yandex.ru.test.ru/", RU_),
                Arguments.of("https://test.ru/zen.yandex.ru/", RU_),
                Arguments.of("https://zenyandex.ru/", RU_),
                Arguments.of("https://zen.yandex.ru/abc", TR)
        );
    }

    @ParameterizedTest
    @MethodSource("negative_arguments")
    void getBannerPlatformName_negative(String href, Language language) {

        BannerPlatformName platformName = getPlatformName(href, language);
        assertThat(platformName).isNull();
    }

    private BannerPlatformName getPlatformName(String href, Language language) {
        return BannerPlatformNameExtractor.getBannerPlatformName(new BannerPlatformNameInput(href, language))
                .orElse(null);
    }

}
