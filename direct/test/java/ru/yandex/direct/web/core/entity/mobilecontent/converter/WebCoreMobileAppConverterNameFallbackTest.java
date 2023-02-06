package ru.yandex.direct.web.core.entity.mobilecontent.converter;

import java.util.Collection;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import ru.yandex.direct.core.entity.mobileapp.model.MobileApp;
import ru.yandex.direct.core.entity.mobileapp.model.MobileAppStoreType;
import ru.yandex.direct.core.entity.mobileapp.service.SkAdNetworkSlotsConfig;
import ru.yandex.direct.core.entity.mobileapp.service.SkAdNetworkSlotsConfigProvider;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.web.core.entity.mobilecontent.model.WebMobileApp;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ParametersAreNonnullByDefault
@RunWith(Parameterized.class)
public class WebCoreMobileAppConverterNameFallbackTest {
    private static final String ENGLISH_NAME = "English name";
    private static final String RUSSIAN_NAME = "Русскоязычное название";
    private static final String CHINESE_NAME = "\u5FAE\u4FE1"; // WeChat
    // BMP = basic multilingual plane
    private static final String CHINESE_NAME_OUT_OF_BMP = "\uD862\uDF4E"; // сиборгий (Sg), химический элемент
    private static final String MOBILE_CONTENT_ID = "com.yandex.testapp";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private TrackerConverter trackerConverter;

    @Mock
    private MobileContentConverter mobileContentConverter;

    @Mock
    private SkAdNetworkSlotsConfigProvider skAdNetworkSlotsConfigProvider;

    private WebCoreMobileAppConverter converter;

    private final String name;
    private final String mobileContentName;
    private final String mobileContentId;
    private final String expectedName;

    @Parameterized.Parameters(name = "\"{0}\" \"{1}\" \"{2}\" => \"{3}\"")
    public static Collection<Object[]> parameters() {
        return List.of(
                // корректные данные
                new Object[]{ENGLISH_NAME, ENGLISH_NAME, MOBILE_CONTENT_ID, ENGLISH_NAME},
                new Object[]{RUSSIAN_NAME, RUSSIAN_NAME, MOBILE_CONTENT_ID, RUSSIAN_NAME},
                new Object[]{CHINESE_NAME, CHINESE_NAME, MOBILE_CONTENT_ID, CHINESE_NAME},

                // если первое имя корректно, фоллбек не нужен
                new Object[]{ENGLISH_NAME, null, null, ENGLISH_NAME},

                // некорректное первое из имён
                new Object[]{null, ENGLISH_NAME, MOBILE_CONTENT_ID, ENGLISH_NAME},
                new Object[]{"", ENGLISH_NAME, MOBILE_CONTENT_ID, ENGLISH_NAME},
                new Object[]{" ", ENGLISH_NAME, MOBILE_CONTENT_ID, ENGLISH_NAME},

                // имена вне BMP
                new Object[]{CHINESE_NAME_OUT_OF_BMP, CHINESE_NAME_OUT_OF_BMP, MOBILE_CONTENT_ID, MOBILE_CONTENT_ID},
                new Object[]{CHINESE_NAME_OUT_OF_BMP + " ", CHINESE_NAME_OUT_OF_BMP + " ", MOBILE_CONTENT_ID,
                        MOBILE_CONTENT_ID},

                // если всё пропало, функция не падает
                new Object[]{null, null, null, null}
        );
    }

    public WebCoreMobileAppConverterNameFallbackTest(
            String name, String mobileContentName, String mobileContentId,
            String expectedName) {
        this.name = name;
        this.mobileContentName = mobileContentName;
        this.mobileContentId = mobileContentId;
        this.expectedName = expectedName;
    }

    @Before
    public void setUp() throws Exception {
        when(skAdNetworkSlotsConfigProvider.getConfig())
                .thenReturn(new SkAdNetworkSlotsConfig(10, 3));

        converter = new WebCoreMobileAppConverter(trackerConverter, mobileContentConverter,
                skAdNetworkSlotsConfigProvider);
    }

    @Test
    public void convertMobileAppToWeb() {
        MobileApp mobileApp = new MobileApp()
                .withStoreType(MobileAppStoreType.GOOGLEPLAYSTORE)
                .withName(name)
                .withMobileContent(new MobileContent()
                        .withName(mobileContentName)
                        .withStoreContentId(mobileContentId));

        WebMobileApp webMobileApp = converter.convertMobileAppToWeb(mobileApp, null, emptyList(), emptySet());

        assertThat(webMobileApp.getName()).isEqualTo(expectedName);
    }
}
