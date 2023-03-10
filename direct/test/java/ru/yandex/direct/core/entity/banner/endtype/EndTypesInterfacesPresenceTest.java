package ru.yandex.direct.core.entity.banner.endtype;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.ClassUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.model.ContentPromotionBanner;
import ru.yandex.direct.core.entity.banner.model.CpcVideoBanner;
import ru.yandex.direct.core.entity.banner.model.CpmAudioBanner;
import ru.yandex.direct.core.entity.banner.model.CpmBanner;
import ru.yandex.direct.core.entity.banner.model.CpmGeoPinBanner;
import ru.yandex.direct.core.entity.banner.model.CpmIndoorBanner;
import ru.yandex.direct.core.entity.banner.model.CpmOutdoorBanner;
import ru.yandex.direct.core.entity.banner.model.DynamicBanner;
import ru.yandex.direct.core.entity.banner.model.ImageBanner;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.McBanner;
import ru.yandex.direct.core.entity.banner.model.MobileAppBanner;
import ru.yandex.direct.core.entity.banner.model.PerformanceBanner;
import ru.yandex.direct.core.entity.banner.model.TextBanner;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.ClassUtils.getAllInterfaces;
import static org.apache.commons.lang3.ClassUtils.getPackageName;
import static org.junit.Assert.assertTrue;

/**
 * Из-за того что интерфейсы наследуются друг от друга, при изменении интерфейсов конечного типа баннера можно не
 * заметить, что отключились необходимые интерфейсы или подключились лишние.
 * Тест проверяет, что у баннеров список интерфейсов совпадает с ожидаемым.
 * <p>
 * При осознанном добавлении/удалении интерфейсов у баннера, нужно обновить ожидаемый список интерфейсов здесь.
 */
@RunWith(Parameterized.class)
public class EndTypesInterfacesPresenceTest {
    private static final String BANNER_MODEL_PACKAGE = "ru.yandex.direct.core.entity.banner.model";
    @Parameterized.Parameter
    public Class<BannerWithSystemFields> bannerEndType;
    @Parameterized.Parameter(1)
    public Set<String> expectedInterfaces;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return asList(new Object[][]{
                {
                        ContentPromotionBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithHref",
                                "BannerWithTitle",
                                "BannerWithContentPromotion",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithContentPromotionHref",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithFlags",
                                "BannerWithAggregatorDomain",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithTitleAndBodyForBsExport")
                },
                {
                        CpcVideoBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithHref",
                                "BannerWithTitle",
                                "BannerWithTitleComputedFromCreative",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithTurboLandingModeration",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithHrefAndTurboLanding",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "BannerWithTurboLanding",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithTurboLandingParams",
                                "BannerWithFlags",
                                "BannerWithAggregatorDomain",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BannerWithBodyComputedFromCreative",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithVideo",
                                "BannerWithTurboLandingForBsExport",
                                "BannerWithTitleAndBodyForBsExport",
                                "BannerWithMobileContentAdGroupForBsExport",
                                "BannerWithHrefForBsExport")
                },
                {
                        CpmAudioBanner.class,
                        Set.of("BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithPixelsAndMeasurers",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithMeasurers",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithFlags",
                                "BannerWithAggregatorDomain",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BannerWithPixels",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithPricePackage")
                },
                {
                        CpmBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithLogoModeration",
                                "BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithLogoForBsExport",
                                "BannerWithPricePackage",
                                "BannerWithTurboLandingModeration",
                                "BannerWithButton",
                                "BannerWithMulticardSet",
                                "BannerWithMulticardSetMulticards",
                                "BannerWithMulticardSetStatusModerate",
                                "BannerWithMulticardSetModeration",
                                "BannerWithMulticardSetForBsExport",
                                "BannerWithStatusArchived",
                                "BannerWithMeasurers",
                                "BannerWithHrefAndTurboLanding",
                                "BannerWithSystemFields",
                                "BannerWithButtonModeration",
                                "BannerWithAggregatorDomain",
                                "BannerWithCreativeModeration",
                                "BannerWithTitle",
                                "BannerWithCampaignId",
                                "BannerWithOrganization",
                                "BannerWithPixelsAndMeasurers",
                                "BannerWithTns",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithTitleExtension",
                                "BannerWithLanguage",
                                "BannerWithTurboLanding",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithTurboLandingParams",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithButtonForBsExport",
                                "BannerWithLogo",
                                "BannerWithPixels",
                                "BannerWithAdditionalHrefs",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithTurboLandingForBsExport",
                                "BannerWithTitleAndBodyForBsExport",
                                "BannerWithTitleExtensionForBsExport",
                                "BannerWithBigKingImage")
                },
                {
                        CpmGeoPinBanner.class,
                        Set.of("BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithOrganization",
                                "BannerWithPixelsAndMeasurers",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithMeasurers",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BannerWithPixels",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BaseBannerWithResourcesForBsExport")
                },
                {
                        CpmIndoorBanner.class,
                        Set.of("BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport")
                },
                {
                        CpmOutdoorBanner.class,
                        Set.of("BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "ImmutableBanner",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport")
                },
                {
                        DynamicBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithHref",
                                "BannerWithFixedTitle",
                                "BannerWithAdGroupId",
                                "BannerWithBannerImageModeration",
                                "BannerWithSitelinksModeration",
                                "BannerWithStatusArchived",
                                "BannerWithSystemFields",
                                "BannerWithBannerImage",
                                "BannerWithOrganization",
                                "BannerWithOrganizationAndVcard",
                                "BannerWithOrganizationAndPhone",
                                "BannerWithPhone",
                                "BannerWithVcardModeration",
                                "BannerWithAggregatorDomain",
                                "BannerWithDisplayHref",
                                "BannerWithTitle",
                                "BannerWithVcard",
                                "BannerWithCampaignId",
                                "BannerWithStatusShow",
                                "BannerWithHrefAndDisplayHref",
                                "BannerWithModerationStatuses",
                                "BannerWithSitelinks",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithDisplayHrefModeration",
                                "BannerWithCallouts",
                                "BannerWithTitleAndBodyForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithVcardForBsExport",
                                "BannerToSendPromoExtensionForBsExport")
                },
                {
                        ImageBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithLogoModeration",
                                "BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithLogoForBsExport",
                                "BannerWithTurboLandingModeration",
                                "BannerWithButton",
                                "BannerWithStatusArchived",
                                "BannerWithHrefAndTurboLanding",
                                "BannerWithSystemFields",
                                "BannerWithButtonModeration",
                                "BannerWithAggregatorDomain",
                                "BannerWithImageModeration",
                                "BannerWithCreativeModeration",
                                "BannerWithImage",
                                "BannerWithTitle",
                                "BannerWithImageAndCreative",
                                "BannerWithCampaignId",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithTitleExtension",
                                "BannerWithLanguage",
                                "BannerWithTurboLanding",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithTurboLandingParams",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithButtonForBsExport",
                                "BannerWithLogo",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithTurboLandingForBsExport",
                                "BannerWithTitleAndBodyForBsExport",
                                "BannerWithMobileContentAdGroupForBsExport",
                                "BannerWithTitleExtensionForBsExport",
                                "BannerWithHrefForBsExport")
                },
                {
                        InternalBanner.class,
                        Set.of("BannerWithInternalInfo",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithFlags",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BaseBannerWithResourcesForBsExport")
                },
                {
                        McBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithHref",
                                "BannerWithTitle",
                                "BannerWithFixedTitle",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithFixedBody",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithFlags",
                                "BannerWithAggregatorDomain",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithImageModeration",
                                "BannerWithImage",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithHrefForBsExport",
                                "BannerWithTitleAndBodyForBsExport")
                },
                {
                        MobileAppBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithHref",
                                "BannerWithTitle",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithBannerImageModeration",
                                "BannerWithMobileContent",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithBannerImage",
                                "BannerWithFlags",
                                "BannerWithCreativeModeration",
                                "BannerWithCreative",
                                "BannerWithAggregatorDomain",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithTitleAndBodyForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithCallouts",
                                "BannerWithMobileContentAdGroupForBsExport",
                                "BannerWithMobileContentDataForBsExport",
                                "BannerWithHrefForBsExport",
                                "BannerToSendShowTitleAndBodyFlagForBsExport")
                },
                {
                        PerformanceBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithTitle",
                                "BannerWithFixedTitle",
                                "BannerWithAdGroupId",
                                "BannerWithCampaignId",
                                "BannerWithStatusShow",
                                "BannerWithModerationStatuses",
                                "BannerWithStatusArchived",
                                "BannerWithFixedBody",
                                "BannerWithSystemFields",
                                "BannerWithLanguage",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithCreativeModeration",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithTitleAndBodyForBsExport",
                                "BannerWithVcardForBsExport",
                                "BannerWithVcard",
                                "BannerWithVcardModeration",
                                "BannerWithCallouts")
                },
                {
                        TextBanner.class,
                        Set.of("BannerWithBody",
                                "BannerWithLogoModeration",
                                "BannerWithHref",
                                "BannerWithAdGroupId",
                                "BannerWithLogoForBsExport",
                                "BannerWithTurboLandingModeration",
                                "BannerWithBannerImageModeration",
                                "BannerWithButton",
                                "BannerWithMulticardSet",
                                "BannerWithMulticardSetMulticards",
                                "BannerWithMulticardSetStatusModerate",
                                "BannerWithMulticardSetModeration",
                                "BannerWithMulticardSetForBsExport",
                                "BannerWithMulticardSetAndBannerImage",
                                "BannerWithSitelinksModeration",
                                "BannerWithStatusArchived",
                                "BannerWithOrganizationAndVcard",
                                "BannerWithOrganizationAndPhone",
                                "BannerWithPhone",
                                "BannerWithSystemFields",
                                "BannerWithButtonModeration",
                                "BannerWithBannerImage",
                                "BannerWithHrefAndPriceAndTurboApp",
                                "BannerWithVcardModeration",
                                "BannerWithAggregatorDomain",
                                "BannerWithCreativeModeration",
                                "BannerWithDisplayHref",
                                "BannerWithDisplayHrefTexts",
                                "BannerWithGreenUrlTextsForBsExport",
                                "BannerWithHrefAndTurboLandingAndSitelinks",
                                "BannerWithHrefAndTurboLandingAndVcardAndOrganization",
                                "BannerWithTitle",
                                "BannerWithIsMobile",
                                "BannerWithVcard",
                                "BannerWithCampaignId",
                                "BannerWithOrganization",
                                "BannerWithPrice",
                                "BannerWithStatusShow",
                                "BannerWithHrefAndDisplayHref",
                                "BannerWithModerationStatuses",
                                "BannerWithSitelinks",
                                "BannerWithTitleExtension",
                                "BannerWithTurboGallery",
                                "BannerWithLanguage",
                                "BannerWithTurboLanding",
                                "Banner",
                                "BannerWithCreative",
                                "BannerWithTurboLandingParams",
                                "BannerWithFlags",
                                "BannerWithOnlyGeoFlag",
                                "BannerWithButtonForBsExport",
                                "BannerWithLogo",
                                "BannerWithDisplayHrefModeration",
                                "BannerWithTurboApp",
                                "BannerWithCallouts",
                                "BannerWithTitleAndBodyForBsExport",
                                "BannerWithStatusActive",
                                "BannerWithBsBannerId",
                                "BannerWithStatusBsSynced",
                                "BannerWithLastChange",
                                "BannerWithName",
                                "BannerWithNameModeration",
                                "BannerWithNameForBsExport",
                                "BannerWithHrefForBsExport",
                                "BaseBannerWithResourcesForBsExport",
                                "BannerWithTurboLandingForBsExport",
                                "BannerWithVcardForBsExport",
                                "BannerWithTitleExtensionForBsExport",
                                "BannerToSendPromoExtensionForBsExport",
                                "BannerToSendShowTitleAndBodyFlagForBsExport",
                                "BannerWithLeadformAttributes",
                                "BannerWithLeadButtonForBsExport",
                                "BannerWithZenPublisherId",
                                "BannerWithZenPublisherIdForBsExport")
                }
        });
    }

    @Test
    public void checkInterfaces() {
        Set<String> actualInterfacesNames = getAllInterfaces(bannerEndType).stream()
                .filter(x -> getPackageName(x).equals(BANNER_MODEL_PACKAGE))
                .map(ClassUtils::getShortClassName)
                .collect(toSet());
        boolean success = actualInterfacesNames.equals(expectedInterfaces);
        assertTrue(getErrorMessage(success, actualInterfacesNames), success);
    }

    private String getErrorMessage(boolean success, Set<String> actualInterfacesNames) {
        if (success) {
            return null;
        }
        Sets.SetView<String> difference1 = Sets.difference(expectedInterfaces, actualInterfacesNames);
        Sets.SetView<String> difference2 = Sets.difference(actualInterfacesNames, expectedInterfaces);
        return String.format("Actual set not contains: %s, expected set not contains: %s",
                difference1, difference2);
    }
}
