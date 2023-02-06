package ru.yandex.direct.core.entity.mobilecontent.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.mobilecontent.container.MobileAppStoreUrl;
import ru.yandex.direct.core.entity.mobilecontent.model.ContentType;
import ru.yandex.direct.core.entity.mobilecontent.model.OsType;
import ru.yandex.direct.core.entity.uac.model.Store;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;


@RunWith(value = Parameterized.class)
public class MobileAppStoreUrlParserTest {
    private String storeUrl;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private Optional<MobileAppStoreUrl> expectedMobileAppStoreUrl;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public MobileAppStoreUrlParserTest(String storeUrl, Optional<MobileAppStoreUrl> expectedMobileAppStoreUrl) {
        this.storeUrl = storeUrl;
        this.expectedMobileAppStoreUrl = expectedMobileAppStoreUrl;
    }

    @Parameterized.Parameters(name = "url{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                // Google Play
                // Apps
                // с кавычками
                {"\"https://play.google.com/store/apps/details?id=com.avtofriend.com.avtofriend\"",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".avtofriend.com" +
                                ".avtofriend", true
                        ))},
                {"https://play.google.com/store/apps/details?id=com.avtofriend.com.avtofriend",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".avtofriend.com" +
                                ".avtofriend", true
                        ))},
                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&gl=ua",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "UA", "RU", "com" +
                                ".netmarble.mherosgb",
                                false
                        ))},
                // страна большими буквами
                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&gl=UA",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "UA", "RU", "com" +
                                ".netmarble.mherosgb",
                                false
                        ))},

                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&hk=uA",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".netmarble.mherosgb", true
                        ))},
                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&hk=uk",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".netmarble.mherosgb", true
                        ))},
                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&hk=uk_RU",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".netmarble.mherosgb", true
                        ))},
                {"http://play.google.com/store/apps/details?id=com.netmarble.mherosgb&hk=tr_RU",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".netmarble.mherosgb", true
                        ))},
                {"play.google.com/store/apps/details?id=com.halfbrick.fruitninjafree&foo=baz&hl=tr&param=blah",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "TR", "com" +
                                ".halfbrick" +
                                ".fruitninjafree", true
                        ))},
                {"play.google.com/store/apps/details?id=com.halfbrick.fruitninjafree&foo=baz&hl=tr&gl=by&param=blah",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "BY", "TR", "com" +
                                ".halfbrick" +
                                ".fruitninjafree", false
                        ))},
                {"play.google.com/store/apps/details?id=com.halfbrick.fruitninjafree&foo=baz&hl=kk&param=blah",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "KK", "com" +
                                ".halfbrick" +
                                ".fruitninjafree", true
                        ))},
                // некорректный параметр gl
                {"play.google.com/store/apps/details?id=com.halfbrick.fruitninjafree&gl=x",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".halfbrick" +
                                ".fruitninjafree", true
                        ))},
                {"play.google.com/store/apps/details?id=com.halfbrick.fruitninjafree&gl=turkey",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.ANDROID, Store.GOOGLE_PLAY, ContentType.APP, "RU", "RU", "com" +
                                ".halfbrick" +
                                ".fruitninjafree", true
                        ))},

                {"https://play.baidu.com/store/apps/details?id=air.isporch&hl=tr", Optional.empty()},
                {"https://play.google.com/store/apps/details?id=&hl=tr", Optional.empty()},
                {"https://play.google.com/store/details?id=air.isporch&hl=tr", Optional.empty()},
                {"https://play.google.com/store/details?id=air.isporchhl=tr", Optional.empty()},
                {"https://play.google.com/store/details?id=ru.sutochno_redesignttps://itunes.apple.com/app/id973070068",
                        Optional.empty()},
                {"https://play.google.com/store/details?id=com.surpax.ledflashlight" +
                        ".panel=http:2percentTO2percentTOroem.ru",
                        Optional.empty()},
                // Пока поддерживаем только приложения, остальные типы контента недоступны
                {"https://play.google.com/store/movies/details/Маша_и_медведь_Крик_победы?id=uHSGpaaMREw",
                        Optional.empty()},
                {"https://play.google.com/store/books/details/Э_Джеймс_Пятьдесят_оттенков_серого?id=93RRKy9sEkIC&hl=ru",
                        Optional.empty()},
                {"https://play.google.com/store/music/album/Various_Artists_Золушка_Оригинальный_саундтрек?id" +
                        "=B3tgwz5c2gsskfikedprxspatrq",
                        Optional.empty()},
                {"https://play.google.com/store/newsstand/details/Коммерческий_директор?id=CAow98zSAQ",
                        Optional.empty()},

                // Apple App Store
                // Apps
                {"\"https://itunes.apple.com/us/app/garageband/id408709785\"",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "US", "RU", "id408709785", false
                        ))},
                {"https://itunes.apple.com/us/app/garageband/id408709785?mt=8",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "US", "RU", "id408709785", false
                        ))},
                {"https://itunes.apple.com/app/garageband/id408709785",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id408709785", true
                        ))},
                {"https://itunes.apple.com/app/id408709785",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id408709785", true
                        ))},
                {"http://itunes.apple.com/ru/app/garageband/id408709785",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id408709785", false
                        ))},
                {"itunes.apple.com/tr/app/garageband/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "TR", "RU", "id408709785", false
                        ))},
                {"itunes.apple.com/tr/app/garageband/id408709785?l=en",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "TR", "EN", "id408709785", false
                        ))},
                {"https://itunes.apple.com/app/id408709785?l=tr",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "TR", "id408709785", true
                        ))},
                {"https://itunes.apple.com/en/app/world-of-tanks-blitz/id859204347",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "US", "RU", "id859204347", false
                        ))},
                // Новый URL стора приложений
                {"https://apps.apple.com/ru/app/angry-birds-classic/id343200656",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id343200656", false
                        ))},
                // ссылку с двумя или более слэшами считаем валидной
                {"itunes.apple.com//tr/app/garageband/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "TR", "RU", "id408709785", false
                        ))},
                {"itunes.apple.com//app/garageband/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id408709785", true
                        ))},
                {"itunes.apple.com////////////rs/app/ga/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RS", "RU", "id408709785", false
                        ))},
                // ссылку со слэшом на конце тоже считаем валидной
                {"itunes.apple.com//ru/app/garageband/id408709785/",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id408709785", false
                        ))},
                // страна большими буквами
                {"itunes.apple.com/TR/app/garageband/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "TR", "RU", "id408709785", false
                        ))},
                // страна буквами разного регистра
                {"itunes.apple.com/Tr/app/garageband/id408709785?l=ru",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "TR", "RU", "id408709785", false
                        ))},
                {"https://itunes.apple.com/pk/app/panda-parachute-rescue-fall/id689245528?mt=11&foo=baz&hl=tr&param" +
                        "=blah",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "PK", "RU", "id689245528", false
                        ))},
                {"https://itunes.apple.com/ru/app/ex/id1052945328?mt=8",
                        Optional.of(new MobileAppStoreUrl(
                                OsType.IOS, Store.ITUNES, ContentType.APP, "RU", "RU", "id1052945328", false
                        ))},

                {"https://itunes.xiaomi.com/tr/app/garageband/id408709785?l=ru", Optional.empty()},
                {"https://itunes.apple.com/tr/app/garageband", Optional.empty()},
                // Пока поддерживаем только приложения, остальные типы контента недоступны
                {"https://itunes.apple.com/ru/movie/otel-grand-budapest/id832985900?l=ru", Optional.empty()},
                {"https://itunes.apple.com/us/album/bullet-single/id986872941", Optional.empty()},
                {"https://itunes.apple.com/tr/book/idle-thoughts-idle-fellow/id498917318?mt=11", Optional.empty()},
                {"https://itunes.apple.com/us/podcast/military-hd/id289484726?mt=2", Optional.empty()},

                // Странные случаи
                {null, Optional.empty()},
                {"", Optional.empty()},
                {"foo bar", Optional.empty()},
                {"?mt=11&foo=baz&hl=tr&param=blah", Optional.empty()},
                {"https%3A%2F%2Fplay.google.com%2Fstore%2Fapps%2Fdetails%3Fid%3Dcom.halfbrick" +
                        ".fruitninjafree%26foo%3Dbaz%26hl%3Dtr%26param%3Dblah", Optional.empty()},
                {"https://play.google.com", Optional.empty()},
        });
    }

    @SuppressWarnings("OptionalIsPresent")
    @Test
    public void parse() {
        Optional<MobileAppStoreUrl> mobileAppStoreUrl = MobileAppStoreUrlParser.parse(storeUrl);
        assertThat(mobileAppStoreUrl.isPresent(), equalTo(expectedMobileAppStoreUrl.isPresent()));
        if (mobileAppStoreUrl.isPresent()) {
            assertThat(mobileAppStoreUrl.get(), beanDiffer(expectedMobileAppStoreUrl.get()));
        }
    }
}
