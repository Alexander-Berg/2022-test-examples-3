package ru.yandex.direct.api.v5.entity.ads.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.yandex.direct.api.v5.ads.MobileAppAdGet;
import com.yandex.direct.api.v5.ads.MobileAppFeatureEnum;
import com.yandex.direct.api.v5.general.MobileAppAdActionEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import com.yandex.direct.api.v5.general.YesNoUnknownEnum;
import one.util.streamex.StreamEx;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ads.StatusClarificationTranslations;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContent;
import ru.yandex.direct.core.entity.mobilecontent.model.MobileContentExternalWorldMoney;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreActionForPrices;
import ru.yandex.direct.core.entity.mobilecontent.model.StoreCountry;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.i18n.Translatable;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.mobilecontent.util.MobileContentUtil.getExternalWorldMoney;

@Api5Test
@RunWith(SpringRunner.class)
public class GetResponseConverterConvertMobileAppBannerTest {

    private static final StatusClarificationTranslations TRANSLATIONS = StatusClarificationTranslations.INSTANCE;

    @Autowired
    public TranslationService translationService;

    @Autowired
    public GetResponseConverter converter;

    @Test
    public void titleIsConverted() {
        String title = "title";
        var ad = buildMobileAppAd().withTitle(title);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getTitle()).isEqualTo(title);
    }

    @Test
    public void textIsConverted() {
        String text = "text body";
        var ad = buildMobileAppAd().withBody(text);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getText()).isEqualTo(text);
    }

    @Test
    public void trackingUrlIsConverted_not_null() {
        String trackingUrl = "trackingUrl";
        var ad = buildMobileAppAd().withHref(trackingUrl);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getTrackingUrl().getValue()).isEqualTo(trackingUrl);
    }

    @Test
    public void trackingUrlIsConverted_null() {
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer());
        assertThat(result.getTrackingUrl().isNil()).isTrue();
    }

    @Test
    public void impressionUrlIsConverted_not_null() {
        String impressionUrl = "impressionUrl";
        var ad = buildMobileAppAd().withImpressionUrl(impressionUrl);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getImpressionUrl().getValue()).isEqualTo(impressionUrl);
    }

    @Test
    public void impressionUrlIsConverted_null() {
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer());
        assertThat(result.getImpressionUrl().isNil()).isTrue();
    }

    @Test
    public void actionIsConverted() {
        var ad = buildMobileAppAd().withPrimaryAction(NewMobileContentPrimaryAction.INSTALL);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getAction()).isEqualByComparingTo(MobileAppAdActionEnum.INSTALL);
    }

    @Test
    public void adImageHashIsConverted_not_null() {
        String hash = "adImageHash";
        var ad = buildMobileAppAd().withImageHash(hash)
                .withImageStatusModerate(StatusBannerImageModerate.YES);
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getAdImageHash().getValue()).isEqualTo(hash);
    }

    @Test
    public void adImageHashIsConverted_null() {
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer());
        assertThat(result.getAdImageHash().isNil()).isTrue();
    }

    @Test
    public void adImageModerationStatusIsConverted_not_null() {
        var ad =
                buildMobileAppAd().withImageHash("adImageHash")
                        .withImageStatusModerate(StatusBannerImageModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad)
                .withImageModerationReasons(
                        asList(new ModerationDiag().withDiagText("Раз"), new ModerationDiag().withDiagText("Два")))
                .build();

        MobileAppAdGet result = converter.convertMobileAppBanner(adsGetContainer);

        assertThat(result.getAdImageModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.imageRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void adImageModerationStatusIsConverted_null() {
        var ad = buildMobileAppAd();
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer(ad));
        assertThat(result.getAdImageModeration().isNil()).isTrue();
    }

    @Test
    public void featuresIsConverted() {
        Map<NewReflectedAttribute, Boolean> reflectedAttributes =
                StreamEx.of(NewReflectedAttribute.values()).toMap(v -> true);

        var ad = buildMobileAppAd().withReflectedAttributes(reflectedAttributes);

        MobileContent mobileContent = new MobileContent()
                .withModifyTime(LocalDateTime.now())
                .withIconHash("iconHash")
                .withRating(BigDecimal.TEN)
                .withPrices(ImmutableMap.<String, Map<StoreActionForPrices, MobileContentExternalWorldMoney>>builder()
                        .put(StoreCountry.RU.toString(),
                                ImmutableMap.<StoreActionForPrices, MobileContentExternalWorldMoney>builder()
                                        .put(StoreActionForPrices.update,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.open,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.buy,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.more,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.download,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.install,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.play,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .put(StoreActionForPrices.get,
                                                getExternalWorldMoney("1.23", CurrencyCode.RUB))
                                        .build())
                        .build())
                .withRatingVotes(5L);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withMobileContent(mobileContent).build();

        MobileAppAdGet result = converter.convertMobileAppBanner(adsGetContainer);
        assertThat(result.getFeatures())
                .extracting("feature", "enabled", "isAvailable")
                .contains(
                        Tuple.tuple(MobileAppFeatureEnum.PRICE, YesNoEnum.YES, YesNoUnknownEnum.YES),
                        Tuple.tuple(MobileAppFeatureEnum.ICON, YesNoEnum.YES, YesNoUnknownEnum.YES),
                        Tuple.tuple(MobileAppFeatureEnum.CUSTOMER_RATING, YesNoEnum.YES, YesNoUnknownEnum.YES),
                        Tuple.tuple(MobileAppFeatureEnum.RATINGS, YesNoEnum.YES, YesNoUnknownEnum.YES));
    }

    @Test
    public void videoExtensionIsConverted_not_null() {
        Long creativeId = 1L;
        String thumbnailUrl = "thumbnail_url";
        String previewUrl = "preview_url";

        AdsGetContainer adsGetContainer = getContainerBuilder(buildMobileAppAd()).withCreative(
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl))
                .withBannerCreative(new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.YES))
                .build();

        MobileAppAdGet result = converter.convertMobileAppBanner(adsGetContainer);
        assertThat(result.getVideoExtension().getValue())
                .hasFieldOrPropertyWithValue("CreativeId", creativeId)
                .hasFieldOrPropertyWithValue("ThumbnailUrl", thumbnailUrl)
                .hasFieldOrPropertyWithValue("PreviewUrl", previewUrl)
                .hasFieldOrPropertyWithValue("Status", StatusEnum.ACCEPTED);
    }

    @Test
    public void videoExtensionIsConverted_null() {
        MobileAppAdGet result = converter.convertMobileAppBanner(getContainer());
        assertThat(result.getVideoExtension().isNil()).isTrue();
    }

    private String translate(Translatable translatable) {
        return translationService.translate(translatable);
    }

    //region utils
    private MobileAppBanner buildMobileAppAd() {
        Map<NewReflectedAttribute, Boolean> reflectedAttributes =
                StreamEx.of(NewReflectedAttribute.values()).toMap(v -> true);

        return new MobileAppBanner()
                .withId(0L)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true)
                .withPrimaryAction(NewMobileContentPrimaryAction.BUY)
                .withReflectedAttributes(reflectedAttributes);
    }

    private AdsGetContainer.Builder getContainerBuilder(BannerWithSystemFields ad) {
        return new AdsGetContainer.Builder()
                .withAd(ad)
                .withCampaign(new Campaign()
                        .withStatusActive(true)
                        .withStatusArchived(false)
                        .withStatusShow(true));
    }

    private AdsGetContainer getContainer(BannerWithSystemFields ad) {
        return getContainerBuilder(ad).build();
    }

    private AdsGetContainer getContainer() {
        return getContainer(buildMobileAppAd());
    }
    //endregion'
}
