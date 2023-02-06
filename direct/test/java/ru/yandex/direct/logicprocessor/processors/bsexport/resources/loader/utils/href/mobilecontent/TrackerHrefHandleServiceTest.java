package ru.yandex.direct.logicprocessor.processors.bsexport.resources.loader.utils.href.mobilecontent;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import one.util.streamex.StreamEx;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.logicprocessor.processors.configuration.EssLogicProcessorTestConfiguration;
import ru.yandex.direct.utils.model.UrlParts;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@ContextConfiguration(classes = EssLogicProcessorTestConfiguration.class)
@ExtendWith(SpringExtension.class)
class TrackerHrefHandleServiceTest {

    @Autowired
    private TrackerHrefHandleService trackerHrefHandleService;

    static Stream<Arguments> paramsForExistingMacroTest() {
        return Stream.of(
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={trackid}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={TRACKID}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={logid}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&click_id={LOGID}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&param&click_id={logid}",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&param&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&param=logid",
                        "https://redirect.appmetrica.yandex.com?q={Google_aid}&google_aid={google_aid}&param=logid&click_id={trackid}")
        );
    }

    static Stream<Arguments> paramsForHrefWithParamsTest() {
        return Stream.of(
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?param=value1&param2=value2",
                        "https://redirect.appmetrica.yandex.com?param=value1&param2=value2&" +
                                "google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com?param=value1&param2=value2&" +
                                "google_aid=whatever",
                        "https://redirect.appmetrica.yandex.com?param=value1&param2=value2&" +
                                "google_aid={google_aid}&click_id={trackid}")
        );
    }

    static Stream<Arguments> params() {
        return Stream.of(
                // app_metrika
                arguments(OsType.IOS,
                        "https://redirect.appmetrica.yandex.com",
                        "https://redirect.appmetrica.yandex.com?ios_ifa={ios_ifa}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.com",
                        "https://redirect.appmetrica.yandex.com?google_aid={google_aid}&click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "https://redirect.appmetrica.yandex.ru",
                        "https://redirect.appmetrica.yandex.ru?google_aid={google_aid}&click_id={trackid}"),

                // mat_tune
                arguments(OsType.ANDROID,
                        "http://hastrk3.com/pub_c?adgroup_id=3601",
                        "http://hastrk3.com/pub_c?adgroup_id=3601&google_aid={google_aid}&publisher_ref_id={trackid}"),
                arguments(OsType.IOS,
                        "http://hastrk3.com/pub_c?adgroup_id=3601",
                        "http://hastrk3.com/pub_c?adgroup_id=3601&ios_ifa={ios_ifa}&publisher_ref_id={trackid}"),


                // appsflyer
                arguments(OsType.ANDROID,
                        "https://app.appsflyer.com/id794999619",
                        "https://app.appsflyer.com/id794999619?advertising_id={google_aid}&clickid={trackid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.IOS,
                        "https://app.appsflyer.com/id794999619",
                        "https://app.appsflyer.com/id794999619?idfa={ios_ifa}&clickid={trackid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),

                arguments(OsType.ANDROID,
                        "https://impression.appsflyer.com/id794999619",
                        "https://impression.appsflyer.com/id794999619?advertising_id={google_aid}&clickid={trackid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.IOS,
                        "https://impression.appsflyer.com/id794999619",
                        "https://impression.appsflyer.com/id794999619?idfa={ios_ifa}&clickid={trackid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),

                // kochava
                arguments(OsType.ANDROID,
                        "https://control.kochava.com/v1/cpi/click",
                        "https://control.kochava.com/v1/cpi/click?adid={google_aid}&android_id={android_id}"),
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}"),

                arguments(OsType.ANDROID,
                        "https://imp.control.kochava.com/track/impression?event=view",
                        "https://imp.control.kochava.com/track/impression?event=view&adid={google_aid}&android_id={android_id}"),
                arguments(OsType.IOS,
                        "https://imp.control.kochava.com/track/impression?event=view",
                        "https://imp.control.kochava.com/track/impression?event=view&ios_idfa={ios_ifa}"),

                // adjust
                arguments(OsType.ANDROID, "http://app.adj.st/z5zbnp",
                        "http://app.adj.st/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}"),
                arguments(OsType.IOS,
                        "http://app.adj.st/z5zbnp",
                        "http://app.adj.st/z5zbnp?idfa={ios_ifa}&ya_click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "http://app.adj.st/z5zbnp?ya_click_id={logid}",
                        "http://app.adj.st/z5zbnp?ya_click_id={trackid}&gps_adid={google_aid}&oaid={oaid}"),

                arguments(OsType.ANDROID, "http://app.adjust.com/z5zbnp",
                        "http://app.adjust.com/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}"),
                arguments(OsType.IOS,
                        "http://app.adjust.com/z5zbnp",
                        "http://app.adjust.com/z5zbnp?idfa={ios_ifa}&ya_click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "http://app.adjust.com/z5zbnp?ya_click_id={logid}",
                        "http://app.adjust.com/z5zbnp?ya_click_id={trackid}&gps_adid={google_aid}&oaid={oaid}"),

                arguments(OsType.ANDROID, "http://view.adjust.com/impression/z5zbnp",
                        "http://view.adjust.com/impression/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}"),
                arguments(OsType.IOS,
                        "http://view.adjust.com/impression/z5zbnp",
                        "http://view.adjust.com/impression/z5zbnp?idfa={ios_ifa}&ya_click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "http://view.adjust.com/impression/z5zbnp?ya_click_id={logid}",
                        "http://view.adjust.com/impression/z5zbnp?ya_click_id={trackid}&gps_adid={google_aid}&oaid={oaid}"),
                // flurry
                arguments(OsType.ANDROID,
                        "https://ad.apps" +
                                ".fm/JdOTGS5CQf8zy8_OsVQKUbPep_dGN3pRvXtKzxd9KeRCY4r76MIFhLxmD2dvfeZWoYVoIExjBvOYDJ0R2cJahA",
                        "https://ad.apps" +
                                ".fm/JdOTGS5CQf8zy8_OsVQKUbPep_dGN3pRvXtKzxd9KeRCY4r76MIFhLxmD2dvfeZWoYVoIExjBvOYDJ0R2cJahA?adid={google_aid}&click_id={trackid}"),
                arguments(OsType.IOS,
                        "https://ad.apps" +
                                ".fm/JdOTGS5CQf8zy8_OsVQKUbPep_dGN3pRvXtKzxd9KeRCY4r76MIFhLxmD2dvfeZWoYVoIExjBvOYDJ0R2cJahA",
                        "https://ad.apps" +
                                ".fm/JdOTGS5CQf8zy8_OsVQKUbPep_dGN3pRvXtKzxd9KeRCY4r76MIFhLxmD2dvfeZWoYVoIExjBvOYDJ0R2cJahA?ios_idfa={ios_ifa}&click_id={trackid}"),

                // mytracker
                arguments(OsType.ANDROID,
                        "https://trk.mail.ru/c/qwerty1234",
                        "https://trk.mail.ru/c/qwerty1234?mt_gaid={google_aid}&clickId={trackid}&regid={trackid}"),
                arguments(OsType.IOS,
                        "https://trk.mail.ru/c/qwerty1234",
                        "https://trk.mail.ru/c/qwerty1234?mt_idfa={ios_ifa}&clickId={trackid}&regid={trackid}"),

                // branch
                arguments(OsType.ANDROID,
                        "https://some.app.link/qwerty1234",
                        "https://some.app.link/qwerty1234?%24aaid={google_aid}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.IOS,
                        "https://some.app.link/qwerty1234",
                        "https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct"),

                // singular
                arguments(OsType.ANDROID,
                        "https://some.sng.link/qwe/rty",
                        "https://some.sng.link/qwe/rty?aifa={google_aid}&andi={android_id}&oaid={oaid}&cl={trackid}"),
                arguments(OsType.IOS,
                        "https://some.sng.link/qwe/rty",
                        "https://some.sng.link/qwe/rty?idfa={ios_ifa}&cl={trackid}"),

                // unknown type
                arguments(OsType.ANDROID,
                        "http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354",
                        "http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354"),
                arguments(OsType.ANDROID,
                        "http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354&clickid={logid}",
                        "http://62218kapi-02.com/serve?action=click&publisher_id=62218&site_id=50802&offer_id=309354&clickid={trackid}")
        );
    }

    static Stream<Arguments> paramsForAdjustHrefWithCallbacks() {
        return Stream.of(
                // kochava
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click?install_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}&install_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback"),
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click?install_callback=http%3A%2F%2Fexample.com%2Fpostback",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}&install_callback=http%3A%2F%2Fexample.com%2Fpostback"),
                arguments(OsType.ANDROID,
                        "https://control.kochava.com/v1/cpi/click?conversion_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback",
                        "https://control.kochava.com/v1/cpi/click?adid={google_aid}&android_id={android_id}&conversion_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback"),
                arguments(OsType.ANDROID,
                        "https://control.kochava.com/v1/cpi/click?conversion_callback=http%3A%2F%2Fexample.com%2Fpostback",
                        "https://control.kochava.com/v1/cpi/click?adid={google_aid}&android_id={android_id}&conversion_callback=http%3A%2F%2Fexample.com%2Fpostback"),

                // adjust
                arguments(OsType.IOS,
                        "http://app.adj.st/z5zbnp?install_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback",
                        "http://app.adj.st/z5zbnp?idfa={ios_ifa}&ya_click_id={trackid}"),
                arguments(OsType.IOS,
                        "http://app.adj.st/z5zbnp?install_callback=http%3A%2F%2Fexample.com%2Fpostback",
                        "http://app.adj.st/z5zbnp?install_callback=http%3A%2F%2Fexample.com%2Fpostback&idfa={ios_ifa}&ya_click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "http://app.adj.st/z5zbnp?conversion_callback=http%3A%2F%2Fpostback.yandexadexchange.net%2Fpostback",
                        "http://app.adj.st/z5zbnp?gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}"),
                arguments(OsType.ANDROID,
                        "http://app.adj.st/z5zbnp?conversion_callback=http%3A%2F%2Fexample.com%2Fpostback",
                        "http://app.adj.st/z5zbnp?conversion_callback=http%3A%2F%2Fexample.com%2Fpostback&gps_adid={google_aid}&oaid={oaid}&ya_click_id={trackid}")
        );
    }

    static Stream<Arguments> paramsForAppsflyerHrefWithCampaignName() {
        return Stream.of(
                // kochava
                arguments(OsType.ANDROID,
                        "https://control.kochava.com/v1/cpi/click",
                        "https://control.kochava.com/v1/cpi/click?adid={google_aid}&android_id={android_id}"),
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click?c=",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}&c="),
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click?c=123",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}&c=123"),

                // appsflyer
                arguments(OsType.ANDROID,
                        "https://app.appsflyer.com/id794999619?c=",
                        "https://app.appsflyer.com/id794999619?advertising_id={google_aid}&clickid={trackid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.IOS,
                        "https://app.appsflyer.com/id794999619?c=whatever",
                        "https://app.appsflyer.com/id794999619?idfa={ios_ifa}&clickid={trackid}&pid=yandexdirect_int&af_c_id={campaign_id}&c=whatever")

        );
    }

    static Stream<Arguments> paramsForHrefWithConstantParameters() {
        return Stream.of(
                // kochava - none
                arguments(OsType.ANDROID,
                        "https://control.kochava.com/v1/cpi/click",
                        "https://control.kochava.com/v1/cpi/click?adid={google_aid}&android_id={android_id}"),
                arguments(OsType.IOS,
                        "https://control.kochava.com/v1/cpi/click?pid=123",
                        "https://control.kochava.com/v1/cpi/click?ios_idfa={ios_ifa}&pid=123"),

                // appsflyer - pid=yandexdirect_int
                arguments(OsType.IOS,
                        "https://app.appsflyer.com/ru.auto.ara",
                        "https://app.appsflyer.com/ru.auto.ara?idfa={ios_ifa}&clickid={trackid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.IOS,
                        "https://app.appsflyer.com/ru.auto.ara?pid=something",
                        "https://app.appsflyer.com/ru.auto.ara?idfa={ios_ifa}&clickid={trackid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.IOS,
                        "https://impression.appsflyer.com/ru.auto.ara?clickid={logid}&idfa={ios_ifa}&pid=yandexdirect_int",
                        "https://impression.appsflyer.com/ru.auto.ara?clickid={trackid}&idfa={ios_ifa}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.ANDROID,
                        "https://app.appsflyer.com/ru.auto.ara",
                        "https://app.appsflyer.com/ru.auto.ara?advertising_id={google_aid}&clickid={trackid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.ANDROID,
                        "https://app.appsflyer.com/ru.auto.ara?pid=something",
                        "https://app.appsflyer.com/ru.auto.ara?advertising_id={google_aid}&clickid={trackid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),
                arguments(OsType.ANDROID,
                        "https://impression.appsflyer.com/ru.auto.ara?clickid={logid}&advertising_id={google_aid}&pid=yandexdirect_int",
                        "https://impression.appsflyer.com/ru.auto.ara?clickid={trackid}&advertising_id={google_aid}&oaid={oaid}&pid=yandexdirect_int&af_c_id={campaign_id}&c={campaign_name}"),

                // branch - %243p=a_yandex_direct
                arguments(OsType.IOS,
                        "https://some.app.link/qwerty1234",
                        "https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.IOS,
                        "https://some.app.link/qwerty1234?%243p=something",
                        "https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.IOS,
                        "https://some.app.link/qwerty1234?~click_id={logid}&%243p=a_yandex_direct",
                        "https://some.app.link/qwerty1234?%24idfa={ios_ifa}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.ANDROID,
                        "https://some.app.link/qwerty1234",
                        "https://some.app.link/qwerty1234?%24aaid={google_aid}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.ANDROID,
                        "https://some.app.link/qwerty1234?%243p=something",
                        "https://some.app.link/qwerty1234?%24aaid={google_aid}&~click_id={trackid}&%243p=a_yandex_direct"),
                arguments(OsType.ANDROID,
                        "https://some.app.link/qwerty1234?~click_id={logid}&%243p=a_yandex_direct",
                        "https://some.app.link/qwerty1234?%24aaid={google_aid}&~click_id={trackid}&%243p=a_yandex_direct")
        );
    }

    void checkCorrectness(OsType osType, String initialHref, String expectedHref) {
        var gotHref = trackerHrefHandleService.handleHref(initialHref, osType);
        var expectedHrefParts = UrlParts.fromUrl(expectedHref);
        var gotParts = UrlParts.fromUrl(gotHref);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(gotParts.getProtocol()).isEqualTo(expectedHrefParts.getProtocol());
            softly.assertThat(gotParts.getDomain()).isEqualTo(expectedHrefParts.getDomain());
            softly.assertThat(gotParts.getPath()).isEqualTo(expectedHrefParts.getPath());
            softly.assertThat(gotParts.getAnchor()).isEqualTo(expectedHrefParts.getAnchor());
            var gotParams = getParamsMap(gotParts);
            var expectedParams = getParamsMap(expectedHrefParts);
            softly.assertThat(gotParams).isEqualTo(expectedParams);
        });
    }

    /**
     * Тест проверяет, что если в ссылке уже присутствуют макросы, которые должны добавиться, но в других параметрах,
     * то будут добавлены и новые параметры
     */
    @ParameterizedTest
    @MethodSource("paramsForExistingMacroTest")
    void existingMarcoInHrefTest(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    @ParameterizedTest
    @MethodSource("params")
    void trackerParamsTest(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    /**
     * Тест проверяет, что если в ссылке уже есть какие-то параметры, то новые перезатрут только конфликтующие,
     * остальные старые параметры останутся на месте, а остальные новые будут добавлены дополнительно
     */
    @ParameterizedTest
    @MethodSource("paramsForHrefWithParamsTest")
    void hrefWithParamsTest(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    /**
     * Тест проверяет, что если в Adjust-ссылке есть параметры install_callback или conversion_callback,
     * то они будут удалены, но только если содержат ссылку на домен postback.yandexadexchange.net
     */
    @ParameterizedTest
    @MethodSource("paramsForAdjustHrefWithCallbacks")
    void adjustHrefWithCallbacksTest(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    /**
     * Тест проверяет, что если в AppsFlyer-ссылке нет параметра c, то его значение выставится в {campaign_name}
     */
    @ParameterizedTest
    @MethodSource("paramsForAppsflyerHrefWithCampaignName")
    void appsflyerHrefWithCampaignNameTest(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    /**
     * Тест проверяет, что в ссылках всегда присутствуют константные параметры
     */
    @ParameterizedTest
    @MethodSource("paramsForHrefWithConstantParameters")
    void hrefWithConstantParameters(OsType osType, String initialHref, String expectedHref) {
        checkCorrectness(osType, initialHref, expectedHref);
    }

    private Map<String, Optional<String>> getParamsMap(UrlParts parts) {
        var params = parts.getParameters();
        if (Objects.isNull(params)) {
            return null;
        }
        return StreamEx.of(params)
                .mapToEntry(Pair::getLeft, p -> Optional.ofNullable(p.getRight()))
                .toMap();
    }
}
