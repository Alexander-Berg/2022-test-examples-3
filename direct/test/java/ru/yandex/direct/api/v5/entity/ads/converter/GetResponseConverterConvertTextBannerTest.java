package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.ads.PriceCurrencyEnum;
import com.yandex.direct.api.v5.ads.PriceQualifierEnum;
import com.yandex.direct.api.v5.ads.TextAdGet;
import com.yandex.direct.api.v5.general.StatusEnum;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ads.StatusClarificationTranslations;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.i18n.Translatable;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static ru.yandex.direct.api.v5.common.ConverterUtils.convertToMicros;
import static ru.yandex.direct.core.testing.steps.BannerPriceSteps.defaultNewBannerPrice;

@Api5Test
@RunWith(SpringRunner.class)
public class GetResponseConverterConvertTextBannerTest {
    private static final ObjectFactory FACTORY = new ObjectFactory();
    private static final List<ModerationDiag> reasons =
            asList(new ModerationDiag().withDiagText("Раз"), new ModerationDiag().withDiagText("Два"));

    private static final StatusClarificationTranslations TRANSLATIONS = StatusClarificationTranslations.INSTANCE;

    @Autowired
    public TranslationService translationService;

    @Autowired
    public GetResponseConverter converter;

    @Test
    public void titleIsConverted() {
        String title = "title";
        var ad = buildTextAd().withTitle(title);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTitle()).isEqualTo(title);
    }

    @Test
    public void title2IsConverted_not_null() {
        String titleExtension = "titleExtension";
        var ad = buildTextAd().withTitleExtension(titleExtension);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTitle2().getValue()).isEqualTo(titleExtension);
    }

    @Test
    public void title2IsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getTitle2().isNil()).isTrue();
    }

    @Test
    public void textIsConverted() {
        String text = "text body";
        var ad = buildTextAd().withBody(text);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getText()).isEqualTo(text);
    }

    @Test
    public void hrefIsConverted_not_null() {
        String href = "href";
        var ad = buildTextAd().withHref(href);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getHref().getValue()).isEqualTo(href);
    }

    @Test
    public void hrefIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getHref().isNil()).isTrue();
    }

    @Test
    public void mobileIsConverted_YES() {
        var ad = buildTextAd().withIsMobile(true);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getMobile()).isEqualTo(YesNoEnum.YES);
    }

    @Test
    public void mobileIsConverted_NO() {
        var ad = buildTextAd().withIsMobile(false);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getMobile()).isEqualTo(YesNoEnum.NO);
    }

    @Test
    public void displayDomainIsConverted_not_null() {
        String domain = "domain";
        var ad = buildTextAd().withDomain(domain);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getDisplayDomain().getValue()).isEqualTo(domain);
    }

    @Test
    public void displayDomainIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getDisplayDomain().isNil()).isTrue();
    }

    @Test
    public void displayUrlPathIsConverted_not_null() {
        String displayHref = "displayHref";
        var ad =
                buildTextAd().withDisplayHref(displayHref).withDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.YES);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getDisplayUrlPath().getValue()).isEqualTo(displayHref);
    }

    @Test
    public void displayUrlPathIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getDisplayUrlPath().isNil()).isTrue();
    }

    @Test
    public void vCardIdIsConverted_not_null() {
        Long vcardId = 9L;
        var ad = buildTextAd().withVcardId(vcardId).withVcardStatusModerate(BannerVcardStatusModerate.YES);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getVCardId().getValue()).isEqualTo(vcardId);
    }

    @Test
    public void vCardIdIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getVCardId().isNil()).isTrue();
    }

    @Test
    public void businessIdIsConverted_not_null() {
        Long businessId = 123L;
        var ad = buildTextAd().withPermalinkId(businessId);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getBusinessId().getValue()).isEqualTo(businessId);
    }

    @Test
    public void businessIdIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getBusinessId().isNil()).isTrue();
    }

    @Test
    public void preferVCardOverBusinessIsConverted_not_null() {
        var ad = buildTextAd().withPreferVCardOverPermalink(true);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getPreferVCardOverBusiness().getValue()).isEqualTo(YesNoEnum.YES);
    }

    @Test
    public void preferVCardOverBusinessIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getPreferVCardOverBusiness().getValue()).isEqualTo(YesNoEnum.NO);
    }

    @Test
    public void adImageHashIsConverted_not_null() {
        String hash = "adImageHash";
        var ad = buildTextAd().withImageHash(hash).withImageStatusModerate(StatusBannerImageModerate.YES);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getAdImageHash().getValue()).isEqualTo(hash);
    }

    @Test
    public void adImageHashIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getAdImageHash().isNil()).isTrue();
    }

    @Test
    public void sitelinkSetIdIsConverted_not_null() {
        Long sitelinkSetId = 3L;
        var ad = buildTextAd().withSitelinksSetId(sitelinkSetId)
                .withStatusSitelinksModerate(BannerStatusSitelinksModerate.YES);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getSitelinkSetId().getValue()).isEqualTo(sitelinkSetId);
    }

    @Test
    public void sitelinkSetIdIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getSitelinkSetId().isNil()).isTrue();
    }

    @Test
    public void bannerTurboLandingStatusModerateIsConverted_null() {
        var ad = buildTextAd().withTurboLandingStatusModerate(null);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTurboPageModeration().isNil()).isTrue();
    }

    @Test
    public void bannerTurboLandingStatusModerateIsConverted_not_null() {
        var ad = buildTextAd().withTurboLandingStatusModerate(BannerTurboLandingStatusModerate.YES);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTurboPageModeration().getValue().getStatus()).isEqualTo(StatusEnum.ACCEPTED);
    }

    @Test
    public void turboPageIdIsConverted_not_null() {
        long turboLandingId = 123L;
        var ad = buildTextAd().withTurboLandingId(turboLandingId);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTurboPageId().getValue()).isEqualTo(turboLandingId);
    }

    @Test
    public void turboPageIdIsConverted_null() {
        var ad =
                buildTextAd().withTurboLandingId(null);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getTurboPageId().isNil()).isTrue();
    }

    @Test
    public void adExtensionsIsConverted_not_null() {
        var ad = buildTextAd().withCalloutIds(asList(1L, 2L, 3L));
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getAdExtensions()).extracting("AdExtensionId", "Type")
                .contains(
                        tuple(1L, AdExtensionTypeEnum.CALLOUT),
                        tuple(2L, AdExtensionTypeEnum.CALLOUT),
                        tuple(3L, AdExtensionTypeEnum.CALLOUT));
    }

    @Test
    public void adExtensionsIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getAdExtensions()).isEmpty();
    }

    @Test
    public void adImageModerationStatusIsConverted_not_null() {
        var ad = buildTextAd()
                .withImageHash("2")
                .withImageStatusModerate(StatusBannerImageModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withImageModerationReasons(reasons).build();

        TextAdGet result = converter.convertTextBanner(adsGetContainer);
        assertThat(result.getAdImageModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.imageRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void adImageModerationStatusIsConverted_null() {
        var ad = buildTextAd();
        TextAdGet result = converter.convertTextBanner(getContainer(ad));
        assertThat(result.getAdImageModeration().isNil()).isTrue();
    }

    @Test
    public void displayUrlPathModerationStatusIsConverted_not_null() {
        var ad = buildTextAd().withDisplayHref("displayHref")
                .withDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withDisplayHrefModerationReasons(reasons).build();

        TextAdGet result = converter.convertTextBanner(adsGetContainer);
        assertThat(result.getDisplayUrlPathModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.displayLinkRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void displayUrlPathModerationStatusIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getDisplayUrlPathModeration().isNil()).isTrue();
    }

    @Test
    public void sitelinksModerationStatusIsConverted_not_null() {
        var ad = buildTextAd().withSitelinksSetId(1L)
                .withStatusSitelinksModerate(BannerStatusSitelinksModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withSitelinksModerationReasons(reasons).build();

        TextAdGet result = converter.convertTextBanner(adsGetContainer);
        assertThat(result.getSitelinksModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.sitelinksRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void sitelinksModerationStatusIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getSitelinksModeration().isNil()).isTrue();
    }

    @Test
    public void vCardModerationStatusIsConverted_not_null() {
        var ad = buildTextAd().withVcardId(1L).withVcardStatusModerate(BannerVcardStatusModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withVCardModerationReasons(reasons).build();

        TextAdGet result = converter.convertTextBanner(adsGetContainer);
        assertThat(result.getVCardModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.contactInfoRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void vCardModerationStatusIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getVCardModeration().isNil()).isTrue();
    }

    @Test
    public void videoExtensionIsConverted_not_null() {
        Long creativeId = 1L;
        String thumbnailUrl = "thumbnail_url";
        String previewUrl = "preview_url";

        AdsGetContainer adsGetContainer = getContainerBuilder(buildTextAd()).withCreative(
                        new Creative().withId(creativeId).withPreviewUrl(thumbnailUrl).withLivePreviewUrl(previewUrl))
                .withBannerCreative(new TextBanner().withCreativeStatusModerate(BannerCreativeStatusModerate.YES))
                .build();

        TextAdGet result = converter.convertTextBanner(adsGetContainer);
        assertThat(result.getVideoExtension().getValue())
                .hasFieldOrPropertyWithValue("CreativeId", creativeId)
                .hasFieldOrPropertyWithValue("ThumbnailUrl", thumbnailUrl)
                .hasFieldOrPropertyWithValue("PreviewUrl", previewUrl)
                .hasFieldOrPropertyWithValue("Status", StatusEnum.ACCEPTED);
    }

    @Test
    public void videoExtensionIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getVideoExtension().isNil()).isTrue();
    }

    @Test
    public void priceExtensionIsConverted_not_null() {
        var price = defaultNewBannerPrice();
        var ad = buildTextAd().withBannerPrice(price);
        TextAdGet result = converter.convertTextBanner(getContainer(ad));

        assertThat(result.getPriceExtension().getValue())
                .hasFieldOrPropertyWithValue("Price", convertToMicros(price.getPrice()))
                .hasFieldOrPropertyWithValue("PriceCurrency",
                        PriceCurrencyEnum.fromValue(price.getCurrency().name()))
                .hasFieldOrPropertyWithValue("PriceQualifier",
                        PriceQualifierEnum.fromValue(price.getPrefix().name()));
        assertThat(result.getPriceExtension().getValue().getOldPrice().getValue())
                .isEqualTo(convertToMicros(price.getPriceOld()));
    }

    @Test
    public void priceExtensionIsConverted_null() {
        TextAdGet result = converter.convertTextBanner(getContainer());
        assertThat(result.getPriceExtension().isNil()).isTrue();
    }

    private String translate(Translatable translatable) {
        return translationService.translate(translatable);
    }

    //region utils
    private TextBanner buildTextAd() {
        return new TextBanner()
                .withId(0L)
                .withIsMobile(false)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true);
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
        return getContainer(buildTextAd());
    }
    //endregion
}
