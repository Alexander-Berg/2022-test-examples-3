package ru.yandex.direct.api.v5.entity.ads.converter;

import java.math.BigDecimal;
import java.util.Arrays;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.AdBuilderAdUpdateItem;
import com.yandex.direct.api.v5.ads.AdUpdateItem;
import com.yandex.direct.api.v5.ads.AgeLabelEnum;
import com.yandex.direct.api.v5.ads.CpcVideoAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.CpmBannerAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.MobileAppCpcVideoAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.PriceCurrencyEnum;
import com.yandex.direct.api.v5.ads.PriceExtensionUpdateItem;
import com.yandex.direct.api.v5.ads.SmartAdBuilderAdUpdate;
import com.yandex.direct.api.v5.ads.TextAdUpdate;
import com.yandex.direct.api.v5.ads.UpdateRequest;
import com.yandex.direct.api.v5.general.ArrayOfString;
import com.yandex.direct.api.v5.general.OperationEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.api.v5.entity.ads.AdsUpdateRequestItem;
import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BabyFood;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerPrice;
import ru.yandex.direct.core.entity.banner.model.BannerPricesCurrency;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.FlagProperty;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBannerMain;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.BannerService;
import ru.yandex.direct.model.ModelChanges;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.SMART_TEST_AD_ID;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.listOfUpdateItems;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.textAdUpdateWithCalloutsUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validCpcVideoAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validImageAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validImageCreativeAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validMobileAppCpcVideoAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validMobileCreativeAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validMobileImageAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.AdsUpdateTestData.validTextAdUpdate;
import static ru.yandex.direct.api.v5.entity.ads.validation.AdsApiValidationSignals.bannerWithBannerTypeNotSpecified;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.PRICE_COMPARE_STRATEGY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class AdsUpdateRequestConverterTest {

    private static final ObjectFactory FACTORY = new ObjectFactory();

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private AdsUpdateRequestConverter converter;

    @Test
    public void convert_convertsAllTypesSomehow() {
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(emptyList());

        when(bannerService.getBannersByIds(anyCollection()))
                .thenReturn(singletonList(new PerformanceBanner()
                        .withId(SMART_TEST_AD_ID)));

        int count = 20;
        UpdateRequest request = new UpdateRequest().withAds(listOfUpdateItems(count));

        assertThat(converter.convert(request, false)).hasSize(count);
    }

    @Test
    public void convert_convertBannerWithBannerPrice() {
        var expectedPrice = new BannerPrice().withPrice(BigDecimal.ONE).withCurrency(BannerPricesCurrency.RUB);

        TextAdUpdate update = validTextAdUpdate().withPriceExtension(
                new ObjectFactory().createTextAdUpdatePriceExtension(
                        new PriceExtensionUpdateItem()
                                .withPrice(convertToMicros(expectedPrice.getPrice()))
                                .withPriceCurrency(PriceCurrencyEnum.fromValue(expectedPrice.getCurrency().name()))));

        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner().withBannerPrice(expectedPrice)));

        @SuppressWarnings("unchecked")
        AdsUpdateRequestItem<TextBanner> result =
                (AdsUpdateRequestItem) converter.convert(new UpdateRequest().withAds(
                        new AdUpdateItem().withTextAd(update)), false).get(0);

        assertThat(result.getInternalItem().isPropChanged(TextBanner.BANNER_PRICE)).isTrue();
        assertThat(result.getInternalItem().getChangedProp(TextBanner.BANNER_PRICE))
                .is(matchedBy(beanDiffer(expectedPrice).useCompareStrategy(PRICE_COMPARE_STRATEGY)));
    }

    @Test
    public void convert_convertsBannerWithCallouts() {
        TextAdUpdate update = textAdUpdateWithCalloutsUpdate(OperationEnum.ADD, OperationEnum.ADD);
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner()
                        .withCalloutIds(Arrays.asList(1L, 2L))));

        @SuppressWarnings("unchecked")
        AdsUpdateRequestItem<TextBanner> result =
                (AdsUpdateRequestItem) converter.convert(new UpdateRequest().withAds(
                        new AdUpdateItem().withTextAd(update)), false).get(0);

        assertThat(result.getInternalItem().isPropChanged(TextBanner.CALLOUT_IDS)).isTrue();
        assertThat(result.getInternalItem().getChangedProp(TextBanner.CALLOUT_IDS)).contains(1L, 2L);
    }

    @Test
    public void convert_convertsBannerWithFlags() {
        final long bannerId = 404340L;
        TextAdUpdate update = validTextAdUpdate().withAgeLabel(AgeLabelEnum.AGE_12);
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner()
                        .withId(bannerId)
                        .withFlags(new BannerFlags().with(BannerFlags.AGE, Age.AGE_6))));

        var result = converter.convert(new UpdateRequest().withAds(
                new AdUpdateItem().withId(bannerId).withTextAd(update)), false).get(0);

        assertThat(result.getInternalItem().isPropChanged(TextBanner.FLAGS)).isTrue();
        assertThat(result.getInternalItem().getChangedProp(TextBanner.FLAGS)
                .get(BannerFlags.AGE)).isSameAs(Age.AGE_12);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromNullToAge() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.AGE, null, AgeLabelEnum.AGE_12, BannerFlags.AGE, null);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromAgeToAge() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.AGE, Age.AGE_6, AgeLabelEnum.AGE_12, BannerFlags.AGE,
                Age.AGE_12);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromBabyToAge() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_6, AgeLabelEnum.AGE_12,
                BannerFlags.AGE, Age.AGE_12);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromNullToBaby() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.AGE, null, AgeLabelEnum.MONTHS_6, BannerFlags.BABY_FOOD, null);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromAgeToBaby() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.AGE, Age.AGE_12, AgeLabelEnum.MONTHS_6, BannerFlags.BABY_FOOD,
                BabyFood.BABY_FOOD_6);
    }

    @Test
    public void convert_convertsBannerWithFlagsFromBabyToBaby() {
        testChangeAgeFlag(new TextBanner(), BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_3, AgeLabelEnum.MONTHS_6,
                BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_6);
    }

    @Test
    public void convert_convertsMobileBannerWithFlagsFromAgeToAge() {
        testChangeAgeFlag(new MobileAppBanner(), BannerFlags.AGE, Age.AGE_6, AgeLabelEnum.AGE_12, BannerFlags.AGE,
                Age.AGE_12);
    }

    @Test
    public void convert_convertsMobileBannerWithFlagsFromNullToAge() {
        testChangeAgeFlag(new MobileAppBanner(), BannerFlags.AGE, null, AgeLabelEnum.AGE_12, BannerFlags.AGE, null);
    }

    @Test
    public void convert_convertsBannerWithBusinessAndPreferVCard() {
        final long bannerId = 404340L;
        TextAdUpdate update = validTextAdUpdate()
                .withBusinessId(FACTORY.createTextAdUpdateBusinessId(123L))
                .withPreferVCardOverBusiness(FACTORY.createTextAdUpdatePreferVCardOverBusiness(YesNoEnum.YES));
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner()
                        .withId(bannerId)
                        .withPermalinkId(321L)
                        .withPreferVCardOverPermalink(false)
        ));

        var result = converter.convert(
                new UpdateRequest().withAds(new AdUpdateItem().withId(bannerId).withTextAd(update)), false)
                .get(0)
                .getInternalItem()
                .castModelUp(TextBanner.class);

        assertThat(result.isPropChanged(TextBanner.PERMALINK_ID)).isTrue();
        assertThat(result.getChangedProp(TextBanner.PERMALINK_ID)).isEqualTo(123L);
        assertThat(result.isPropChanged(TextBanner.PREFER_V_CARD_OVER_PERMALINK)).isTrue();
        assertThat(result.getChangedProp(TextBanner.PREFER_V_CARD_OVER_PERMALINK)).isEqualTo(true);
    }

    @Test
    public void convert_convertsBannerWithBusinessWithoutPreferVCard() {
        final long bannerId = 404340L;
        TextAdUpdate update = validTextAdUpdate().withBusinessId(FACTORY.createTextAdUpdateBusinessId(123L));
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner()
                        .withId(bannerId)
                        .withPermalinkId(123L)));

        var result = converter.convert(
                new UpdateRequest().withAds(new AdUpdateItem().withId(bannerId).withTextAd(update)), false)
                .get(0)
                .getInternalItem()
                .castModelUp(TextBanner.class);

        assertThat(result.isPropChanged(TextBanner.PERMALINK_ID)).isTrue();
        assertThat(result.getChangedProp(TextBanner.PERMALINK_ID)).isEqualTo(123L);
        assertThat(result.isPropChanged(TextBanner.PREFER_V_CARD_OVER_PERMALINK)).isFalse();
    }

    @Test
    public void convert_convertsBannerWithoutBusiness() {
        final long bannerId = 404340L;
        TextAdUpdate update = validTextAdUpdate().withBusinessId(FACTORY.createTextAdUpdateBusinessId(null));
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new TextBanner()
                        .withId(bannerId)
                        .withPermalinkId(123L)));

        var result = converter.convert(
                new UpdateRequest().withAds(new AdUpdateItem().withId(bannerId).withTextAd(update)), false)
                .get(0)
                .getInternalItem()
                .castModelUp(TextBanner.class);

        assertThat(result.isPropChanged(TextBanner.PERMALINK_ID)).isTrue();
        assertThat(result.getChangedProp(TextBanner.PERMALINK_ID)).isNull();
        assertThat(result.isPropChanged(TextBanner.PHONE_ID)).isTrue();
        assertThat(result.getChangedProp(TextBanner.PHONE_ID)).isNull();
        assertThat(result.isPropChanged(TextBanner.PREFER_V_CARD_OVER_PERMALINK)).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void convert_ImageAds_AllSubTypesAreSet() {
        converter.convert(new UpdateRequest().withAds(
                new AdUpdateItem().withTextImageAd(validImageAdUpdate()),
                new AdUpdateItem().withTextAdBuilderAd(validImageCreativeAdUpdate()),
                new AdUpdateItem().withMobileAppImageAd(validMobileImageAdUpdate()),
                new AdUpdateItem().withMobileAppAdBuilderAd(validMobileCreativeAdUpdate())), false)
                .stream()
                .map(AdsUpdateRequestItem::getInternalItem)
                .map(mc -> (ModelChanges<ImageBanner>) (ModelChanges<?>) mc)
                .forEach(mc -> assertThat(mc.getChangedProp(ImageBanner.IS_MOBILE_IMAGE)).isNotNull());
    }

    @Test
    public void convert_mobileAppCpcVideoBanners() {
        final long bannerId = 407740L;
        final long creativeId = 5552L;
        final String href = "http://yandex.ru";
        MobileAppCpcVideoAdBuilderAdUpdate update = validMobileAppCpcVideoAdUpdate()
                .withTrackingUrl(FACTORY.createMobileAppCpcVideoAdBuilderAdUpdateTrackingUrl(href))
                .withCreative(new AdBuilderAdUpdateItem().withCreativeId(creativeId));

        when(bannerService.getBannersByIds(anyCollection()))
                .thenReturn(singletonList(new CpcVideoBanner()
                        .withId(bannerId)
                        .withHref("http://changed.yandex.ru")
                        .withCreativeId(creativeId - 10L)));

        AdUpdateItem adUpdateItem = new AdUpdateItem()
                .withId(bannerId)
                .withMobileAppCpcVideoAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpcVideoBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpcVideoBanner.HREF)).isTrue();
        assertThat(result.isPropChanged(CpcVideoBanner.CREATIVE_ID)).isTrue();

        assertThat(result.getChangedProp(CpcVideoBanner.HREF)).isEqualTo(href);
        assertThat(result.getChangedProp(CpcVideoBanner.CREATIVE_ID)).isEqualTo(creativeId);

    }

    @Test
    public void convert_textAdWithoutAnyType_convertedToInvalidBanner() {
        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(1L);
        UpdateRequest updateRequest = new UpdateRequest().withAds(adUpdateItem);
        var result = converter.convert(updateRequest, false).get(0).getInternalItem();
        assertThat(result.toModel()).hasSameClassAs(bannerWithBannerTypeNotSpecified());
    }

    @Test
    public void convert_cpcVideoBanners() {
        final long bannerId = 407740L;
        final long creativeId = 98776688L;
        final String href = "http://music.yandex.ru";
        CpcVideoAdBuilderAdUpdate update = validCpcVideoAdUpdate()
                .withHref(FACTORY.createCpcVideoAdBuilderAdUpdateHref(href))
                .withCreative(new AdBuilderAdUpdateItem().withCreativeId(creativeId));

        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                new CpcVideoBanner()
                        .withId(bannerId)
                        .withHref("http://metrika.yandex.ru")
                        .withCreativeId(creativeId - 10L)));

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpcVideoAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpcVideoBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpcVideoBanner.HREF)).isTrue();
        assertThat(result.isPropChanged(CpcVideoBanner.CREATIVE_ID)).isTrue();

        assertThat(result.getChangedProp(CpcVideoBanner.HREF)).isEqualTo(href);
        assertThat(result.getChangedProp(CpcVideoBanner.CREATIVE_ID)).isEqualTo(creativeId);

    }

    @Test
    public void convertCpmAd_NoPixelsSet_PixelsUnchanged() {
        final long bannerId = 407740L;
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate();

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpmBanner.PIXELS)).isFalse();
    }

    @Test
    public void convertCpmAd_NilPixelsSet_PixelsDeleted() {
        final long bannerId = 407740L;
        JAXBElement<ArrayOfString> pixels = FACTORY.createCpmBannerAdBuilderAdUpdateTrackingPixels(new ArrayOfString());
        pixels.setNil(true);
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate().withTrackingPixels(pixels);

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);
        assertThat(result.isPropChanged(CpmBanner.PIXELS)).isTrue();
        assertThat(result.getChangedProp(CpmBanner.PIXELS)).isEqualTo(emptyList());
    }

    @Test
    public void convertCpmAd_PixelsSet_PixelsChanged() {
        final long bannerId = 407740L;
        JAXBElement<ArrayOfString> pixels = FACTORY.createCpmBannerAdBuilderAdUpdateTrackingPixels(
                new ArrayOfString().withItems("pixels"));
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate().withTrackingPixels(pixels);

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);
        assertThat(result.isPropChanged(CpmBanner.PIXELS)).isTrue();
        assertThat(result.getChangedProp(CpmBanner.PIXELS)).isEqualTo(singletonList("pixels"));
    }

    @Test
    public void convertCpmAd_TnsIdSet_TnsIdChanged() {
        long bannerId = 407740L;
        String tnsId = "SomeTnsId";
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate()
                .withTnsId(FACTORY.createCpmBannerAdBuilderAdUpdateTnsId(tnsId));

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpmBanner.TNS_ID)).isTrue();
        assertThat(result.getChangedProp(CpmBanner.TNS_ID)).isEqualTo(tnsId);
    }

    @Test
    public void convertCpmAd_NilTnsIdSet_TnsIdDelete() {
        final long bannerId = 407740L;
        var tnsId = FACTORY.createCpmBannerAdBuilderAdUpdateTnsId("");
        tnsId.setNil(true);
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate().withTnsId(tnsId);

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpmBanner.TNS_ID)).isTrue();
        assertThat(result.getChangedProp(CpmBanner.TNS_ID)).isNull();
    }

    @Test
    public void convertCpmAd_NoTnsSet_TnsIdUnchanged() {
        final long bannerId = 407740L;
        CpmBannerAdBuilderAdUpdate update = new CpmBannerAdBuilderAdUpdate();

        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(bannerId).withCpmBannerAdBuilderAd(update);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<CpmBanner>) convert(adUpdateItem);

        assertThat(result.isPropChanged(CpmBanner.TNS_ID)).isFalse();
    }

    @Test
    public void convertSmartAd_creativeId() {
        long creativeId = 2L;

        long adId = 1L;
        when(bannerService.getBannersByIds(anyCollection()))
                .thenReturn(singletonList(new PerformanceBanner()
                        .withId(adId)
                        .withCreativeId(creativeId)));

        AdBuilderAdUpdateItem smartAdItem = new AdBuilderAdUpdateItem().withCreativeId(creativeId);
        SmartAdBuilderAdUpdate smartAd = new SmartAdBuilderAdUpdate().withCreative(smartAdItem);
        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(adId).withSmartAdBuilderAd(smartAd);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<PerformanceBanner>) convert(adUpdateItem);
        assertThat(result.isPropChanged(PerformanceBanner.CREATIVE_ID)).isTrue();
        assertThat(result.getPropIfChanged(PerformanceBanner.CREATIVE_ID)).isEqualTo(creativeId);
    }

    @Test
    public void convertSmartAd_NoCreative() {
        long adId = 1L;
        when(bannerService.getBannersByIds(anyCollection()))
                .thenReturn(singletonList(new PerformanceBannerMain()
                        .withId(adId)));

        SmartAdBuilderAdUpdate smartAd = new SmartAdBuilderAdUpdate().withCreative(null);
        AdUpdateItem adUpdateItem = new AdUpdateItem().withId(adId).withSmartAdBuilderAd(smartAd);
        @SuppressWarnings("unchecked")
        var result = (ModelChanges<PerformanceBannerMain>) convert(adUpdateItem);
        assertThat(result.getModelType()).isEqualTo(PerformanceBannerMain.class);
    }

    private ModelChanges<? extends BannerWithSystemFields> convert(AdUpdateItem adUpdateItem) {
        return converter.convert(new UpdateRequest().withAds(adUpdateItem), false).get(0).getInternalItem();
    }

    private <T, V> void testChangeAgeFlag(BannerWithSystemFields banner, FlagProperty<T> fromField, T from,
                                          AgeLabelEnum to, FlagProperty<V> expectedField, V expected) {
        final long bannerId = 404340L;
        TextAdUpdate update = validTextAdUpdate().withAgeLabel(to);
        when(bannerService.getBannersByIds(anyCollection())).thenReturn(singletonList(
                banner
                        .withId(bannerId)
                        .withFlags(new BannerFlags().with(fromField, from))));

        var result = converter.convert(new UpdateRequest().withAds(
                new AdUpdateItem().withId(bannerId).withTextAd(update)), false).get(0);

        assertThat(result.getInternalItem().isPropChanged(TextBanner.FLAGS)).isTrue();
        assertThat(result.getInternalItem().getChangedProp(TextBanner.FLAGS)
                .get(expectedField)).isEqualTo(expected);
    }
}
