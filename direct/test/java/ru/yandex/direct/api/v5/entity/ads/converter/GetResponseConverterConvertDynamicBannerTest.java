package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.List;

import com.yandex.direct.api.v5.adextensiontypes.AdExtensionTypeEnum;
import com.yandex.direct.api.v5.ads.DynamicTextAdGet;
import com.yandex.direct.api.v5.general.StatusEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ads.StatusClarificationTranslations;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.moderationdiag.model.ModerationDiag;
import ru.yandex.direct.i18n.Translatable;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@Api5Test
@RunWith(SpringRunner.class)
public class GetResponseConverterConvertDynamicBannerTest {

    private static final List<ModerationDiag> reasons =
            asList(new ModerationDiag().withDiagText("Раз"), new ModerationDiag().withDiagText("Два"));

    private static final StatusClarificationTranslations TRANSLATIONS = StatusClarificationTranslations.INSTANCE;

    @Autowired
    public GetResponseConverter converter;

    @Autowired
    public TranslationService translationService;

    @Test
    public void textIsConverted() {
        String text = "text body";
        var ad = buildDynamicTextAd().withBody(text);
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getText()).isEqualTo(text);
    }

    @Test
    public void vCardIdIsConverted_not_null() {
        Long vcardId = 9L;
        var ad = buildDynamicTextAd().withVcardId(vcardId)
                .withVcardStatusModerate(BannerVcardStatusModerate.YES);
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getVCardId().getValue()).isEqualTo(vcardId);
    }

    @Test
    public void vCardIdIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getVCardId().isNil()).isTrue();
    }

    @Test
    public void adImageHashIsConverted_not_null() {
        String hash = "adImageHash";
        var ad = buildDynamicTextAd()
                .withImageHash(hash).withImageStatusModerate(StatusBannerImageModerate.YES);
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getAdImageHash().getValue()).isEqualTo(hash);
    }

    @Test
    public void adImageHashIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getAdImageHash().isNil()).isTrue();
    }

    @Test
    public void sitelinkSetIdIsConverted_not_null() {
        Long sitelinkSetId = 3L;
        var ad = buildDynamicTextAd().withSitelinksSetId(sitelinkSetId)
                .withStatusSitelinksModerate(BannerStatusSitelinksModerate.YES);
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getSitelinkSetId().getValue()).isEqualTo(sitelinkSetId);
    }

    @Test
    public void sitelinkSetIdIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getSitelinkSetId().isNil()).isTrue();
    }

    @Test
    public void adExtensionsIsConverted_not_null() {
        var ad = buildDynamicTextAd().withCalloutIds(asList(1L, 2L, 3L));
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getAdExtensions()).extracting("AdExtensionId", "Type")
                .contains(
                        tuple(1L, AdExtensionTypeEnum.CALLOUT),
                        tuple(2L, AdExtensionTypeEnum.CALLOUT),
                        tuple(3L, AdExtensionTypeEnum.CALLOUT));
    }

    @Test
    public void adExtensionsIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getAdExtensions()).isEmpty();
    }

    @Test
    public void adImageModerationStatusIsConverted_not_null() {
        var ad = buildDynamicTextAd()
                .withImageHash("2")
                .withImageStatusModerate(StatusBannerImageModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withImageModerationReasons(reasons).build();

        DynamicTextAdGet result = converter.convertDynamicBanner(adsGetContainer);
        assertThat(result.getAdImageModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.imageRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void adImageModerationStatusIsConverted_null() {
        var ad = buildDynamicTextAd();
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer(ad));
        assertThat(result.getAdImageModeration().isNil()).isTrue();
    }

    @Test
    public void sitelinksModerationStatusIsConverted_not_null() {
        var ad = buildDynamicTextAd().withSitelinksSetId(1L)
                .withStatusSitelinksModerate(BannerStatusSitelinksModerate.NO);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).withSitelinksModerationReasons(reasons).build();

        DynamicTextAdGet result = converter.convertDynamicBanner(adsGetContainer);
        assertThat(result.getSitelinksModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.REJECTED)
                .hasFieldOrPropertyWithValue("StatusClarification",
                        translate(TRANSLATIONS.sitelinksRejectedAtModeration()) + " Раз\nДва");
    }

    @Test
    public void sitelinksModerationStatusIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getSitelinksModeration().isNil()).isTrue();
    }

    @Test
    public void vCardModerationStatusIsConverted_not_null() {
        var ad = buildDynamicTextAd().withVcardId(1L)
                .withVcardStatusModerate(BannerVcardStatusModerate.NEW);

        AdsGetContainer adsGetContainer = getContainerBuilder(ad).build();

        DynamicTextAdGet result = converter.convertDynamicBanner(adsGetContainer);
        assertThat(result.getVCardModeration().getValue())
                .hasFieldOrPropertyWithValue("Status", StatusEnum.DRAFT)
                .hasFieldOrPropertyWithValue("StatusClarification", translate(TRANSLATIONS.adDraft()));
    }

    @Test
    public void vCardModerationStatusIsConverted_null() {
        DynamicTextAdGet result = converter.convertDynamicBanner(getContainer());
        assertThat(result.getVCardModeration().isNil()).isTrue();
    }

    private String translate(Translatable translatable) {
        return translationService.translate(translatable);
    }

    //region utils
    private DynamicBanner buildDynamicTextAd() {
        return new DynamicBanner()
                .withId(0L)
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
        return getContainerBuilder(buildDynamicTextAd()).build();
    }
    //endregion
}
