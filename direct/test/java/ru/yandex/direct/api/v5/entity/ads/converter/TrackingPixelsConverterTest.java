package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.TrackingPixelGetArray;
import com.yandex.direct.api.v5.ads.TrackingPixelGetItem;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.type.pixels.PixelProvider;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.entity.ads.converter.TrackingPixelsConverter.UNKNOWN_PROVIDER_NAME;
import static ru.yandex.direct.api.v5.entity.ads.converter.TrackingPixelsConverter.convertTrackingPixels;

public class TrackingPixelsConverterTest {

    private static final String YNDX_AUDIENCE = "https://mc.yandex.ru/pixel/4967878486431588376?rnd=%aw_random%";
    private static final String ADFOX =
            "https://ads.adfox.ru/******/getCode?p1=****&p2=****&ptrc=*&pfc=****&pfb=*****&pr=%random%";
    private static final String ADMETRICA =
            "https://amc.yandex.ru/******/getCode?p1=****&p2=****&ptrc=*&pfc=****&pfb=*****&pr=%random%";
    private static final String MC_ADMETRICA =
            "https://mc.admetrica.ru/******/getCode?p1=****&p2=****&ptrc=*&pfc=****&pfb=*****&pr=%random%";
    private static final String TNS =
            "https://www.tns-counter.ru/V13a****weborama_ad/ru/UTF-8/tmsec=wadwatch3_217461-1996-1/%25aw_RANDOM%25";
    private static final String UNKNOWN_PROVIDER_URL =
            "https://yandex.ru";
    private static final String ADMETRICA_INVALID_URL = "https://amc.yandex.ru/******/getCode?p1";
    private static final String ADFOX_ADMETRIKA_INVALID_URL =
            "https://amc.adfox.ru/******/getCode?p1=****&p2=****&ptrc=*&pfc=****&pfb=*****&pr=%random%";

    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Test
    public void convert_NotEmptyList() {
        TrackingPixelGetArray trackingPixelGetArray =
                convertTrackingPixels(asList(YNDX_AUDIENCE, ADFOX, ADMETRICA, MC_ADMETRICA, TNS,
                        UNKNOWN_PROVIDER_URL, ADMETRICA_INVALID_URL, ADFOX_ADMETRIKA_INVALID_URL));
        assertThat(trackingPixelGetArray, notNullValue());

        List<TrackingPixelGetItem> items = trackingPixelGetArray.getItems();
        assertThat(items, beanDiffer(asList(
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(YNDX_AUDIENCE)
                        .withProvider(PixelProvider.fromUrl(YNDX_AUDIENCE).getProviderName()),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(ADFOX)
                        .withProvider(PixelProvider.fromUrl(ADFOX).getProviderName()),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(ADMETRICA)
                        .withProvider(PixelProvider.fromUrl(ADMETRICA).getProviderName()),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(MC_ADMETRICA)
                        .withProvider(PixelProvider.fromUrl(MC_ADMETRICA).getProviderName()),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(TNS)
                        .withProvider(PixelProvider.fromUrl(TNS).getProviderName()),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(UNKNOWN_PROVIDER_URL)
                        .withProvider(UNKNOWN_PROVIDER_NAME),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(ADMETRICA_INVALID_URL)
                        .withProvider(UNKNOWN_PROVIDER_NAME),
                FACTORY.createTrackingPixelGetItem()
                        .withTrackingPixel(ADFOX_ADMETRIKA_INVALID_URL)
                        .withProvider(UNKNOWN_PROVIDER_NAME))
        ));
    }

    @Test
    public void convertTrackingPixels_EmptyList() {
        assertThat(convertTrackingPixels(null), nullValue());
    }

    @Test
    public void convertTrackingPixels_Null() {
        assertThat(convertTrackingPixels(null), nullValue());
    }
}
