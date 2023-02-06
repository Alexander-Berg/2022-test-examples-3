package ru.yandex.direct.api.v5.entity.ads;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.ads.AdBuilderAdAddItem;
import com.yandex.direct.api.v5.ads.ButtonExtensionAction;
import com.yandex.direct.api.v5.ads.ButtonExtensionAddItem;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.DynamicTextAdAdd;
import com.yandex.direct.api.v5.ads.MobAppAgeLabelEnum;
import com.yandex.direct.api.v5.ads.MobileAppAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppAdFeatureItem;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.MobileAppFeatureEnum;
import com.yandex.direct.api.v5.ads.MobileAppImageAdAdd;
import com.yandex.direct.api.v5.ads.PriceCurrencyEnum;
import com.yandex.direct.api.v5.ads.PriceExtensionAddItem;
import com.yandex.direct.api.v5.ads.PriceQualifierEnum;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.TextAdAdd;
import com.yandex.direct.api.v5.ads.TextAdBuilderAdAdd;
import com.yandex.direct.api.v5.ads.TextImageAdAdd;
import com.yandex.direct.api.v5.ads.VideoExtensionAddItem;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.MobileAppAdActionEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;

import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;

@ParametersAreNonnullByDefault
public class AdsAddTestData {

    public static TextAdAdd filledTextAd() {
        return new TextAdAdd()
                .withVCardId(12345L)
                .withAdImageHash("1234987adsfgkh")
                .withSitelinkSetId(87654L)
                .withAdExtensionIds(87543L, 23434L)
                .withText("text ad text")
                .withTitle("text ad title")
                .withTitle2("text ad title extension")
                .withHref("http://href.url")
                .withMobile(YesNoEnum.NO)
                .withDisplayUrlPath("href.url")
                .withVideoExtension(new VideoExtensionAddItem().withCreativeId(8484L))
                .withPriceExtension(new PriceExtensionAddItem()
                        .withPrice(convertToMicros(BigDecimal.ONE))
                        .withOldPrice(convertToMicros(BigDecimal.TEN))
                        .withPriceCurrency(PriceCurrencyEnum.RUB)
                        .withPriceQualifier(PriceQualifierEnum.FROM))
                .withBusinessId(123L)
                .withPreferVCardOverBusiness(YesNoEnum.YES);
    }

    public static DynamicTextAdAdd filledDynamicAd() {
        return new DynamicTextAdAdd()
                .withVCardId(23456L)
                .withAdImageHash("asdkfjhsdfjh")
                .withSitelinkSetId(7575L)
                .withAdExtensionIds(888L, 999L)
                .withText("dynamic ad text");
    }

    public static TextImageAdAdd filledTextImageAd() {
        return new TextImageAdAdd()
                .withButtonExtension(new ButtonExtensionAddItem().withAction(ButtonExtensionAction.BUY).withHref(
                        "https://ya" +
                        ".ru"))
                .withAdImageHash("xcv,mnxcv,mn")
                .withHref("https://the.href.ru");
    }

    public static TextAdBuilderAdAdd filledTextAdBuilderAd() {
        return new TextAdBuilderAdAdd()
                .withHref("http://ad.builder.href")
                .withCreative(new AdBuilderAdAddItem().withCreativeId(87348734L));
    }

    public static MobileAppAdAdd filledMobileAppAd() {
        return new MobileAppAdAdd()
                .withAdImageHash("weryuwery")
                .withText("mobile ad text")
                .withTitle("mobile ad title")
                .withTrackingUrl("http://app.store.tracking.url")
                .withImpressionUrl("http://app.store.impression.url")
                .withAction(MobileAppAdActionEnum.INSTALL)
                .withFeatures(new MobileAppAdFeatureItem()
                        .withEnabled(YesNoEnum.YES)
                        .withFeature(MobileAppFeatureEnum.ICON))
                .withAgeLabel(MobAppAgeLabelEnum.AGE_16);
    }

    public static MobileAppImageAdAdd filledMobileAppImageAd() {
        return new MobileAppImageAdAdd()
                .withAdImageHash("hashashash")
                .withTrackingUrl("https://i.m.tracking.you");
    }

    public static MobileAppAdBuilderAdAdd filledMobileAppAdBuilderAd() {
        return new MobileAppAdBuilderAdAdd()
                .withCreative(new AdBuilderAdAddItem().withCreativeId(77777L))
                .withTrackingUrl("https://mobile.creative.ad.track");
    }

    public static CpmBannerAdBuilderAdAdd filledCpmBannerAdBuilderAd() {
        return new CpmBannerAdBuilderAdAdd()
                .withHref("http://ad.builder.href")
                .withCreative(new AdBuilderAdAddItem().withCreativeId(1234444L))
                .withTrackingPixels(new ArrayOfString().withItems("somePixel"));
    }

    public static CpcVideoAdBuilderAdAdd filledCpcVideoAdBuilderAd() {
        return new CpcVideoAdBuilderAdAdd()
                .withCreative(new AdBuilderAdAddItem().withCreativeId(890123L))
                .withHref("https://mail.yandex.ru");
    }

    public static MobileAppCpcVideoAdBuilderAdAdd filledMobileAppCpcVideoAdBuilderAd() {
        return new MobileAppCpcVideoAdBuilderAdAdd()
                .withCreative(new AdBuilderAdAddItem().withCreativeId(5551L))
                .withTrackingUrl("https://i.m.tracking.you");
    }

    public static SmartAdBuilderAdAdd filledSmartAdBuilderAdAdd() {
        return new SmartAdBuilderAdAdd()
                .withCreative(new AdBuilderAdAddItem().withCreativeId(8922093L));
    }
}
