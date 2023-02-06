package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.Map;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.AdCategoryEnum;
import com.yandex.direct.api.v5.ads.AdGetItem;
import com.yandex.direct.api.v5.ads.AdSubtypeEnum;
import com.yandex.direct.api.v5.ads.AdTypeEnum;
import com.yandex.direct.api.v5.ads.AgeLabelEnum;
import com.yandex.direct.api.v5.ads.ArrayOfAdCategoryEnum;
import com.yandex.direct.api.v5.ads.ObjectFactory;
import com.yandex.direct.api.v5.general.StateEnum;
import com.yandex.direct.api.v5.general.StatusEnum;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.ads.StatusClarificationTranslations;
import ru.yandex.direct.api.v5.entity.ads.container.AdsGetContainer;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.common.TranslationService;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.NewMobileContentPrimaryAction;
import ru.yandex.direct.core.entity.banner.model.NewReflectedAttribute;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.campaign.model.Campaign;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.i18n.Translatable;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.direct.core.entity.banner.model.Age.AGE_18;

@Api5Test
@RunWith(SpringRunner.class)
public class GetResponseConverterConvertResponseItemTest {

    private static final StatusClarificationTranslations TRANSLATIONS = StatusClarificationTranslations.INSTANCE;

    @Autowired
    public TranslationService translationService;

    @Autowired
    public GetResponseConverter converter;

    //region base ad
    @Test
    public void idIsConverted() {
        Long id = 1L;
        var ad = buildTextAd().withId(id);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    public void adgroupIdIsConverted() {
        Long id = 1L;
        var ad = buildTextAd().withAdGroupId(id);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getAdGroupId()).isEqualTo(id);
    }

    @Test
    public void campaignIdIsConverted() {
        Long id = 1L;
        var ad = buildTextAd().withCampaignId(id);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getCampaignId()).isEqualTo(id);
    }

    @Test
    public void statusIsCalculated() {
        var ad =
                buildTextAd().withStatusModerate(BannerStatusModerate.YES)
                        .withStatusPostModerate(BannerStatusPostModerate.YES);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getStatus()).isEqualTo(StatusEnum.ACCEPTED);
    }

    @Test
    public void stateIsCalculated() {
        var ad = buildTextAd()
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true);
        Campaign campaign = buildCampaign()
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true);
        AdGetItem result = converter.convertResponseItem(getContainer(ad, campaign));
        assertThat(result.getState()).isEqualTo(StateEnum.ON);
    }

    @Test
    public void statusClarificatinIsCalculated() {
        var ad = buildTextAd()
                .withStatusActive(false)
                .withStatusArchived(true)
                .withStatusShow(false);
        Campaign campaign = buildCampaign().withStatusArchived(false);

        AdGetItem result = converter.convertResponseItem(getContainer(ad, campaign));
        assertThat(result.getStatusClarification())
                .isEqualTo(translate(TRANSLATIONS.adArchived()));
    }

    @Test
    public void adCategoriesIsConverted_not_null() {
        BannerFlags flags = new BannerFlags().with(BannerFlags.ALCOHOL, true).with(BannerFlags.TOBACCO, true);
        var ad = buildTextAd().withFlags(flags);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));

        JAXBElement<ArrayOfAdCategoryEnum> adCategories =
                new ObjectFactory().createAdGetItemAdCategories(
                        new ArrayOfAdCategoryEnum().withItems(asList(AdCategoryEnum.ALCOHOL, AdCategoryEnum.TOBACCO)));
        assertThat(result.getAdCategories()).isEqualToComparingFieldByFieldRecursively(adCategories);
    }

    @Test
    public void adCategoriesIsConverted_null() {
        AdGetItem result = converter.convertResponseItem(getContainer());
        assertThat(result.getAdCategories().isNil()).isTrue();
    }

    @Test
    public void ageLabelIsConverted_not_null() {
        BannerFlags flags = new BannerFlags().with(BannerFlags.AGE, AGE_18);
        var ad = buildTextAd().withFlags(flags);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));

        JAXBElement<AgeLabelEnum> ageLabel = new ObjectFactory().createAdGetItemAgeLabel(AgeLabelEnum.AGE_18);
        assertThat(result.getAgeLabel()).isEqualToComparingFieldByFieldRecursively(ageLabel);
    }

    @Test
    public void ageLabelIsConverted_null() {
        AdGetItem result = converter.convertResponseItem(getContainer());
        assertThat(result.getAgeLabel().isNil()).isTrue();
    }

    @Test
    public void typeIsConverted() {
        var ad = buildTextAd();
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getType()).isEqualByComparingTo(AdTypeEnum.TEXT_AD);
    }

    @Test
    public void subtypeIsCalculated() {
        AdGetItem result = converter.convertResponseItem(getContainer());
        assertThat(result.getSubtype()).isEqualByComparingTo(AdSubtypeEnum.NONE);
    }
    //endregion

    //region text ad
    @Test
    public void textAdIsConverted() {
        String title = "title";
        var ad = buildTextAd().withTitle(title);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getTextAd().getTitle()).isEqualTo(title);
    }
    //endregion

    //region mobile app
    @Test
    public void mobileAppAdIsConverted() {
        String title = "title";
        var ad = buildMobileAppAd().withTitle(title);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getMobileAppAd().getTitle()).isEqualTo(title);
    }
    //endregion

    //region dynamic
    @Test
    public void dynamicAdIsConverted() {
        String text = "text body";
        var ad = buildDynamicTextAd().withBody(text);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getDynamicTextAd().getText()).isEqualTo(text);
    }
    //endregion

    //region text image
    @Test
    public void textImageAdIsConverted() {
        String href = "href";
        var ad = buildTextImageAd().withHref(href);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getTextImageAd().getHref().getValue()).isEqualTo(href);
    }
    //endregion

    //region mobile app image
    @Test
    public void mobileAppImageAdIsConverted() {
        String hash = "adImageHash";
        var ad = buildMobileAppImageAd().withImageHash(hash);
        AdGetItem result = converter.convertResponseItem(getContainer(ad));
        assertThat(result.getMobileAppImageAd().getAdImageHash()).isEqualTo(hash);
    }
    //endregion

    //region text ad builder
    @Test
    public void textAdBuilderAdIsConverted() {
        Long creativeId = 1L;

        AdsGetContainer adsGetContainer = getContainerBuilder(buildTextAdBuilderAd())
                .withCreative(new Creative().withId(creativeId))
                .build();

        AdGetItem result = converter.convertResponseItem(adsGetContainer);
        assertThat(result.getTextAdBuilderAd().getCreative().getCreativeId()).isEqualTo(creativeId);
    }
    //endregion

    //region mobile app ad builder
    @Test
    public void creativeIsConverted_only_id() {
        Long creativeId = 1L;

        AdsGetContainer adsGetContainer = getContainerBuilder(buildMobileAppAdBuilderAd())
                .withCreative(new Creative().withId(creativeId))
                .build();

        AdGetItem result = converter.convertResponseItem(adsGetContainer);
        assertThat(result.getMobileAppAdBuilderAd().getCreative().getCreativeId()).isEqualTo(creativeId);
    }
    //endregion

    //region exceptions
    @Test
    public void anyAdBuilderAdWithNullFlag_exceptionThrown() {
        var ad = buildImageCreativeBanner().withIsMobileImage(null);
        assertThatThrownBy(() -> converter.convertResponseItem(getContainer(ad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can\'t calc subtype for ad");
    }

    @Test
    public void anyImageAdWithNullFlag_exceptionThrown() {
        var ad = buildImageHashBanner().withIsMobileImage(null);
        assertThatThrownBy(() -> converter.convertResponseItem(getContainer(ad)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("can\'t calc subtype for ad");
    }

    private String translate(Translatable translatable) {
        return translationService.translate(translatable);
    }

    //endregion

    //region utils
    private Campaign buildCampaign() {
        return new Campaign()
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true);
    }

    private void enrichWithBaseFields(BannerWithSystemFields ad) {
        ad.withId(0L)
                .withStatusModerate(BannerStatusModerate.NEW)
                .withStatusPostModerate(BannerStatusPostModerate.NEW)
                .withStatusActive(true)
                .withStatusArchived(false)
                .withStatusShow(true);
    }

    private TextBanner buildTextAd() {
        var textBanner = new TextBanner()
                .withIsMobile(false);

        enrichWithBaseFields(textBanner);

        return textBanner;
    }

    private MobileAppBanner buildMobileAppAd() {
        Map<NewReflectedAttribute, Boolean> reflectedAttributes =
                StreamEx.of(NewReflectedAttribute.values()).toMap(v -> true);

        var mobileAppBanner = new MobileAppBanner()
                .withPrimaryAction(NewMobileContentPrimaryAction.BUY)
                .withReflectedAttributes(reflectedAttributes)
                .withImpressionUrl("https://impression.url");

        enrichWithBaseFields(mobileAppBanner);

        return mobileAppBanner;
    }

    private DynamicBanner buildDynamicTextAd() {
        var dynamicBanner = new DynamicBanner();

        enrichWithBaseFields(dynamicBanner);

        return dynamicBanner;
    }

    private ImageBanner buildTextImageAd() {
        return buildImageHashBanner().withIsMobileImage(false);
    }

    private ImageBanner buildMobileAppImageAd() {
        return buildImageHashBanner().withIsMobileImage(true);
    }

    private ImageBanner buildTextAdBuilderAd() {
        return buildImageCreativeBanner().withIsMobileImage(false);
    }

    private ImageBanner buildMobileAppAdBuilderAd() {
        return buildImageCreativeBanner().withIsMobileImage(true);
    }

    private ImageBanner buildImageHashBanner() {
        var imageHashBanner = new ImageBanner()
                .withImageHash("2")
                .withImageStatusModerate(NewStatusImageModerate.NEW);

        enrichWithBaseFields(imageHashBanner);

        return imageHashBanner;
    }

    private ImageBanner buildImageCreativeBanner() {
        var imageCreativeBanner = new ImageBanner()
                .withCreativeId(1L);

        enrichWithBaseFields(imageCreativeBanner);

        return imageCreativeBanner;
    }

    private AdsGetContainer.Builder getContainerBuilder(BannerWithSystemFields ad, Campaign campaign) {
        return new AdsGetContainer.Builder().withAd(ad).withCampaign(campaign).withAdGroupType(AdGroupType.CPM_BANNER);
    }

    private AdsGetContainer.Builder getContainerBuilder(BannerWithSystemFields ad) {
        return getContainerBuilder(ad, buildCampaign());
    }

    private AdsGetContainer getContainer(BannerWithSystemFields ad, Campaign campaign) {
        return getContainerBuilder(ad, campaign).build();
    }

    private AdsGetContainer getContainer(BannerWithSystemFields ad) {
        return getContainer(ad, buildCampaign());
    }

    private AdsGetContainer getContainer() {
        return getContainer(buildTextAd());
    }

    //endregion
}
