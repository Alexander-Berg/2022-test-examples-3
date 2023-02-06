package ru.yandex.direct.core.aggregatedstatuses.logic;

import java.util.Collection;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.aggregatedstatuses.ad.AdStatesEnum;
import ru.yandex.direct.core.entity.banner.aggrstatus.StatusAggregationBanner;
import ru.yandex.direct.core.entity.banner.model.BannerButtonStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerCreativeStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerDisplayHrefStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerLogoStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerMulticardSetStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerTurboLandingStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.NewStatusImageModerate;
import ru.yandex.direct.core.entity.banner.model.StatusBannerImageModerate;
import ru.yandex.direct.dbschema.ppc.enums.BannersBannerType;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsType;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class StatesAdTest {
    private static AdStates adStatesCalculator;

    @Parameterized.Parameter
    public StatusAggregationBanner ad;

    @Parameterized.Parameter(1)
    public Collection<AdStatesEnum> expectedStates;

    @Parameterized.Parameters(name = "{index}: => States: {1}")
    public static Object[][] params() {
        return new Object[][]{
                {new StatusAggregationBanner(),
                        List.of()},

                {new StatusAggregationBanner()
                        .withStatusArchived(true),
                        List.of(AdStatesEnum.ARCHIVED)},

                {new StatusAggregationBanner()
                        .withStatusShow(false),
                        List.of(AdStatesEnum.SUSPENDED)},

                {new StatusAggregationBanner()
                        .withStatusShow(false)
                        .withBannerType(BannersBannerType.internal)
                        .withInternalBannerStoppedByUrlMonitoring(false),
                        List.of(AdStatesEnum.SUSPENDED)},

                {new StatusAggregationBanner()
                        .withStatusShow(false)
                        .withBannerType(BannersBannerType.internal)
                        .withInternalBannerStoppedByUrlMonitoring(true),
                        List.of(AdStatesEnum.SUSPENDED_BY_MONITORING)},

                {new StatusAggregationBanner()
                        .withStatusActive(true),
                        List.of(AdStatesEnum.ACTIVE_IN_BS)},

                {new StatusAggregationBanner()
                        .withStatusModerate(BannerStatusModerate.NEW),
                        List.of(AdStatesEnum.DRAFT)},

                {new StatusAggregationBanner()
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.NEW),
                        List.of(AdStatesEnum.DRAFT)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.REJECTED),
                        List.of(AdStatesEnum.REJECTED)},

                {new StatusAggregationBanner()
                        .withStatusModerate(BannerStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED)},

                {new StatusAggregationBanner()
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED)},

                {new StatusAggregationBanner()
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.NO),
                        List.of(AdStatesEnum.REJECTED)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.performance)
                        .withCreativeStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED)},

                {new StatusAggregationBanner()
                        .withBannerHasHref(false)
                        .withPhoneFlag(BannerVcardStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED, AdStatesEnum.REJECTED_VCARD)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withVideoAdditionsStatusModerate(BannerCreativeStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_VIDEO_ADDITION)},

                //region Moderation states
                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.image_ad)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.READY),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.image_ad)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.SENT),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.image_ad)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.SENDING),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.mcbanner)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.SENT)
                        .withStatusModerate(BannerStatusModerate.YES),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withCreativeStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.READY),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withCreativeStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.SENT),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withCreativeStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.SENDING),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.READY),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.SENT),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.SENDING),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withStatusModerate(BannerStatusModerate.READY),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withStatusModerate(BannerStatusModerate.SENT),
                        List.of(AdStatesEnum.MODERATION)},

                {new StatusAggregationBanner()
                        .withStatusModerate(BannerStatusModerate.SENDING),
                        List.of(AdStatesEnum.MODERATION)},
                //endregion

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED)},

                {new StatusAggregationBanner()
                        .withCreativeStatusModerate(ru.yandex.direct.core.entity.creative.model.StatusModerate.NO),
                        List.of(AdStatesEnum.CREATIVE_REJECTED)},

                {new StatusAggregationBanner()
                        .withAdditionsCalloutsDeclined(true),
                        List.of(AdStatesEnum.HAS_REJECTED_CALLOUTS)},

                {new StatusAggregationBanner()
                        .withPhoneFlag(BannerVcardStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_VCARD)},

                {new StatusAggregationBanner()
                        .withBannerSitelinksStatusModerate(BannerStatusSitelinksModerate.NO),
                        List.of(AdStatesEnum.HAS_REJECTED_SITELINKS)},

                {new StatusAggregationBanner()
                        .withBannerLogoStatusModerate(BannerLogoStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_LOGO)},

                {new StatusAggregationBanner()
                        .withBannerMulticardSetStatusModerate(BannerMulticardSetStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_MULTICARD_SET)},

                {new StatusAggregationBanner()
                        .withBannerButtonStatusModerate(BannerButtonStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_BUTTON)},

                {new StatusAggregationBanner()
                        .withBannerImageStatusModerate(StatusBannerImageModerate.NO),
                        List.of(AdStatesEnum.REJECTED_IMAGE)},

                {new StatusAggregationBanner()
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_DISPLAY_HREF)},

                {new StatusAggregationBanner()
                        .withBannerTurbolandingStatusModerate(BannerTurboLandingStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED_TURBOLANDING)},


                // Additions on Moderation
                {new StatusAggregationBanner()
                        .withAdditionsCalloutsOnModeration(true),
                        List.of(AdStatesEnum.CALLOUTS_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withPhoneFlag(BannerVcardStatusModerate.READY),
                        List.of(AdStatesEnum.VCARD_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withPhoneFlag(BannerVcardStatusModerate.SENDING),
                        List.of(AdStatesEnum.VCARD_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withPhoneFlag(BannerVcardStatusModerate.SENT),
                        List.of(AdStatesEnum.VCARD_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerSitelinksStatusModerate(BannerStatusSitelinksModerate.READY),
                        List.of(AdStatesEnum.SITELINKS_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerSitelinksStatusModerate(BannerStatusSitelinksModerate.SENDING),
                        List.of(AdStatesEnum.SITELINKS_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerSitelinksStatusModerate(BannerStatusSitelinksModerate.SENT),
                        List.of(AdStatesEnum.SITELINKS_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerLogoStatusModerate(BannerLogoStatusModerate.READY),
                        List.of(AdStatesEnum.LOGO_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerLogoStatusModerate(BannerLogoStatusModerate.SENDING),
                        List.of(AdStatesEnum.LOGO_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerLogoStatusModerate(BannerLogoStatusModerate.SENT),
                        List.of(AdStatesEnum.LOGO_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerButtonStatusModerate(BannerButtonStatusModerate.READY),
                        List.of(AdStatesEnum.BUTTON_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerButtonStatusModerate(BannerButtonStatusModerate.SENDING),
                        List.of(AdStatesEnum.BUTTON_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerButtonStatusModerate(BannerButtonStatusModerate.SENT),
                        List.of(AdStatesEnum.BUTTON_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerMulticardSetStatusModerate(BannerMulticardSetStatusModerate.READY),
                        List.of(AdStatesEnum.MULTICARD_SET_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerMulticardSetStatusModerate(BannerMulticardSetStatusModerate.SENDING),
                        List.of(AdStatesEnum.MULTICARD_SET_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerMulticardSetStatusModerate(BannerMulticardSetStatusModerate.SENT),
                        List.of(AdStatesEnum.MULTICARD_SET_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerImageStatusModerate(StatusBannerImageModerate.READY),
                        List.of(AdStatesEnum.IMAGE_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerImageStatusModerate(StatusBannerImageModerate.SENDING),
                        List.of(AdStatesEnum.IMAGE_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerImageStatusModerate(StatusBannerImageModerate.SENT),
                        List.of(AdStatesEnum.IMAGE_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.READY),
                        List.of(AdStatesEnum.DISPLAY_HREF_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.SENDING),
                        List.of(AdStatesEnum.DISPLAY_HREF_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.SENT),
                        List.of(AdStatesEnum.DISPLAY_HREF_ON_MODERATION)},

                {new StatusAggregationBanner()
                        .withBannerTurbolandingStatusModerate(BannerTurboLandingStatusModerate.READY),
                        List.of(AdStatesEnum.TURBOLANDING_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerTurbolandingStatusModerate(BannerTurboLandingStatusModerate.SENDING),
                        List.of(AdStatesEnum.TURBOLANDING_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerTurbolandingStatusModerate(BannerTurboLandingStatusModerate.SENT),
                        List.of(AdStatesEnum.TURBOLANDING_ON_MODERATION)},


                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withVideoAdditionsStatusModerate(BannerCreativeStatusModerate.READY),
                        List.of(AdStatesEnum.VIDEO_ADDITION_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withVideoAdditionsStatusModerate(BannerCreativeStatusModerate.SENDING),
                        List.of(AdStatesEnum.VIDEO_ADDITION_ON_MODERATION)},
                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withVideoAdditionsStatusModerate(BannerCreativeStatusModerate.SENT),
                        List.of(AdStatesEnum.VIDEO_ADDITION_ON_MODERATION)},
                // End Additions on moderation

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpm_outdoor),
                        List.of(AdStatesEnum.PLACEMENTS_REQUIRED)},

                // withPagesModeration is covered in AdStatesPagesModerationTest

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerHasHref(true)
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.YES)
                        .withBannerSitelinksStatusModerate(BannerStatusSitelinksModerate.NEW),
                        List.of(AdStatesEnum.PREACCEPTED)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withCampaignType(CampaignsType.text)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerHasHref(true)
                        .withBannerDisplayHrefStatusModerate(BannerDisplayHrefStatusModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.text)
                        .withCampaignType(CampaignsType.text)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withHasVCard(true)
                        .withPhoneFlag(BannerVcardStatusModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.NO),
                        List.of(AdStatesEnum.REJECTED, AdStatesEnum.PREACCEPTED)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerBasedOnCreativeStatusModerate(BannerCreativeStatusModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.NO),
                        List.of(AdStatesEnum.REJECTED, AdStatesEnum.PREACCEPTED)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerImageAdStatusModerate(NewStatusImageModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.dynamic)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.performance)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.internal)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpm_indoor)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.image_ad)
                        .withCampaignType(CampaignsType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpc_video)
                        .withCampaignType(CampaignsType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerType(BannersBannerType.text)
                        .withCampaignType(CampaignsType.text)
                        .withHasPublishedOrganization(true)
                        .withHasVCard(true)
                        .withPhoneFlag(BannerVcardStatusModerate.NO),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.REJECTED_VCARD, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpc_video)
                        .withCampaignType(CampaignsType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerType(BannersBannerType.internal)
                        .withTemplateId(861L),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)},

                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpc_video)
                        .withCampaignType(CampaignsType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        .withBannerType(BannersBannerType.internal)
                        // Тут просто любой id темплейта, который не является модерируемым
                        .withTemplateId(123L),
                        List.of(AdStatesEnum.PREACCEPTED)},

                /* На всякий случай проверяем, что проверка модерируемости темплейта распространяется
                только на внутренние баннеры */
                {new StatusAggregationBanner()
                        .withBannerType(BannersBannerType.cpc_video)
                        .withCampaignType(CampaignsType.mobile_content)
                        .withStatusPostModerate(BannerStatusPostModerate.YES)
                        // Тут просто любой id темплейта, который не является модерируемым
                        .withTemplateId(123L),
                        List.of(AdStatesEnum.PREACCEPTED, AdStatesEnum.READY_TO_BS)}
        };
    }

    @BeforeClass
    public static void prepare() {
        adStatesCalculator = new AdStates();
    }

    @Test
    public void test() {
        Collection<AdStatesEnum> states = adStatesCalculator.calc(ad);

        assertEquals("got right states", states, expectedStates);
    }
}
