package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import one.util.streamex.EntryStream;
import org.assertj.core.api.SoftAssertions;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.StatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusSitelinksModerate;
import ru.yandex.direct.core.entity.banner.model.BannerVcardStatusModerate;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService;
import ru.yandex.direct.core.entity.moderation.repository.sending.remoderation.RemoderationType;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.NewTextBannerInfo;
import ru.yandex.direct.core.testing.info.SitelinkSetInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.core.testing.data.TestNewTextBanners.fullTextBanner;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;

@CoreTest
@RunWith(Parameterized.class)
public class BannerRemoderateServicePositiveTest {
    private static final ModerationReasonDetailed REMODERATABLE_REASON =
            new ModerationReasonDetailed()
                    .withId(TestModerationDiag.DIAG_ID2);

    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Parameterized.Parameter()
    public String description;
    @Parameterized.Parameter(1)
    public Function<ClientInfo, Campaign> campaignSupplier;
    @Parameterized.Parameter(2)
    public Function<Long, AdGroup> adGroupSupplier;
    @Parameterized.Parameter(3)
    public BiFunction<Long, Long, NewTextBannerInfo> bannerSupplier;
    @Parameterized.Parameter(4)
    public Function<Map<ModerationReasonObjectType, Long>, Map<ModerationReasonObjectType, List<Long>>> modReasonObjectsSupplier;
    @Parameterized.Parameter(5)
    public List<ModerationReasonObjectType> rejectedTypes;

    @Autowired
    private Steps steps;
    @Autowired
    private BannerModerateService service;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private ClientInfo clientInfo;
    private long bannerId;
    private Long operatorUid;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                new Object[]{
                        "Отклонен по причине, предусматривающей перемодерацию, только баннер",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> new NewTextBannerInfo()
                                .withBanner(fullTextBanner(campaignId, adGroupId)
                                        .withStatusModerate(BannerStatusModerate.NO)
                                        .withStatusSitelinksModerate(BannerStatusSitelinksModerate.YES)
                                        .withVcardStatusModerate(BannerVcardStatusModerate.YES)
                                )),
                        createModReasonObjects(map -> EntryStream.of(map)
                                .mapValues(List::of)
                                .toMap()),
                        List.of(ModerationReasonObjectType.BANNER)
                },
                new Object[]{
                        "Отклонен по причине, предусматривающей перемодерацию, несколько полей",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> new NewTextBannerInfo()
                                .withBanner(fullTextBanner(campaignId, adGroupId)
                                        .withStatusModerate(BannerStatusModerate.NO)
                                        .withStatusSitelinksModerate(BannerStatusSitelinksModerate.NO)
                                        .withVcardStatusModerate(BannerVcardStatusModerate.NO)
                                )),
                        createModReasonObjects(map -> EntryStream.of(map)
                                .mapValues(List::of)
                                .toMap()),
                        List.of(ModerationReasonObjectType.BANNER, ModerationReasonObjectType.SITELINKS_SET,
                                ModerationReasonObjectType.CONTACTINFO)
                },
        };
    }

    public static Function<ClientInfo, Campaign> createCampaign(Function<ClientInfo, Campaign> fun) {
        return fun;
    }

    public static Function<Long, AdGroup> createAdGroup(Function<Long, AdGroup> fun) {
        return fun;
    }

    public static BiFunction<Long, Long, NewTextBannerInfo> createBanner(
            BiFunction<Long, Long, NewTextBannerInfo> fun) {
        return fun;
    }

    public static Function<Long, Matcher<DefectInfo<Defect>>> createValidationError(Function<Long,
            Matcher<DefectInfo<Defect>>> fun) {
        return fun;
    }

    public static Function<Map<ModerationReasonObjectType, Long>, Map<ModerationReasonObjectType, List<Long>>> createModReasonObjects(
            Function<Map<ModerationReasonObjectType, Long>, Map<ModerationReasonObjectType, List<Long>>> fun) {
        return fun;
    }

    @Before
    public void setUp() throws Exception {
        steps.moderationDiagSteps().insertStandartDiags();
        ppcPropertiesSupport.set(MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT,
                String.valueOf(REMODERATABLE_REASON.getId()));

        clientInfo = steps.clientSteps().createDefaultClient();
        Campaign campaign = campaignSupplier.apply(clientInfo);
        CampaignInfo campaignInfo = steps.campaignSteps().createCampaign(campaign, clientInfo);
        AdGroup adGroup = adGroupSupplier.apply(campaignInfo.getCampaignId());
        AdGroupInfo adGroupInfo = steps.adGroupSteps().createAdGroup(adGroup, campaignInfo);
        NewTextBannerInfo banner = bannerSupplier.apply(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        var sitelinkSet = steps.sitelinkSetSteps().createSitelinkSet(new SitelinkSetInfo().withClientInfo(clientInfo));
        var vcard = steps.vcardSteps().createVcard(campaignInfo);
        TextBanner textBanner = banner.getBanner();
        var bannerInfo = steps.textBannerSteps()
                .createBanner(banner
                        .withSitelinkSetInfo(sitelinkSet)
                        .withVcardInfo(vcard)
                        .withBanner(textBanner)
                        .withClientInfo(clientInfo)
                        .withAdGroupInfo(adGroupInfo)
                        .withCampaignInfo(campaignInfo));
        steps.keywordSteps().createKeyword(adGroupInfo);
        bannerId = bannerInfo.getBanner().getId();
        var campaignId = campaignInfo.getCampaignId();
        operatorUid = clientInfo.getUid();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CLIENT_ALLOWED_TO_REMODERATE, true);

        var mapOfRejectedItems = listToMap(rejectedTypes, Function.identity(), objectType -> {
            if (objectType == ModerationReasonObjectType.BANNER) {
                return bannerId;
            }
            if (objectType == ModerationReasonObjectType.SITELINKS_SET) {
                return sitelinkSet.getSitelinkSetId();
            }
            if (objectType == ModerationReasonObjectType.CONTACTINFO) {
                return vcard.getVcardId();
            } else {
                throw new IllegalArgumentException("Unsupported ModerationReasonObjectType");
            }
        });
        steps.moderationReasonSteps().insertRejectReasons(clientInfo.getShard(),
                modReasonObjectsSupplier.apply(mapOfRejectedItems), List.of(REMODERATABLE_REASON));
    }

    @Test
    public void moderateTextBanner_success() {
        MassResult<Long> result = service.remoderateBanners(clientInfo.getClientId(), operatorUid,
                singletonList(bannerId));
        SoftAssertions softAssertions = new SoftAssertions();
        softAssertions.assertThat(result.getUnsuccessfulObjectsCount()).isEqualTo(0);
        softAssertions.assertThat(result.getSuccessfulObjectsCount()).isEqualTo(1);
        softAssertions.assertThat(result.getErrorCount()).isEqualTo(0);

        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.BANNER))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.BANNER));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.BANNER_BUTTON))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.BANNER_BUTTON));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.TURBOLANDING))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.TURBOLANDING));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.DISPLAY_HREFS))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.DISPLAY_HREF));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.CONTACTS))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.CONTACTINFO));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.IMAGE))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.IMAGE));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.SITELINKS_SET))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.SITELINKS_SET));
        softAssertions.assertThat(steps.bannerSteps().isBannerReModerationFlagPresentForType(clientInfo.getShard(),
                bannerId, RemoderationType.BANNER_LOGO))
                .isEqualTo(rejectedTypes.contains(ModerationReasonObjectType.BANNER_LOGO));

        softAssertions.assertAll();
    }

}
