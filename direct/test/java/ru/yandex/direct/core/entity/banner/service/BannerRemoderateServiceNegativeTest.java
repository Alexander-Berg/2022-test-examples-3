package ru.yandex.direct.core.entity.banner.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

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
import ru.yandex.direct.core.entity.banner.model.old.OldBanner;
import ru.yandex.direct.core.entity.banner.model.old.OldBannerStatusModerate;
import ru.yandex.direct.core.entity.banner.service.moderation.BannerModerateService;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonDetailed;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestModerationDiag;
import ru.yandex.direct.core.testing.info.AbstractBannerInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.campaign.model0.Campaign;
import ru.yandex.direct.core.testing.steps.campaign.model0.StatusModerate;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectInfo;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.db.PpcPropertyNames.MODERATION_REASONS_ALLOWABLE_TO_REMODERATE_BY_CLIENT;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.adNotFound;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.moderateArchivedBanner;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.remoderateBannerInDraftCampaign;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.remoderateBannerIsNotAllowed;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.remoderateBannerWithNoModReasons;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.remoderateBannerWithNotAllowedModReasons;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.remoderateDraftBanner;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.core.testing.data.TestBanners.activeTextBanner;
import static ru.yandex.direct.core.testing.data.TestCampaigns.newTextCampaign;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(Parameterized.class)
public class BannerRemoderateServiceNegativeTest {
    private static final ModerationReasonDetailed REMODERATABLE_REASON =
            new ModerationReasonDetailed()
                    .withId(TestModerationDiag.DIAG_ID2);
    private static final ModerationReasonDetailed UNREMODERATABLE_REASON =
            new ModerationReasonDetailed()
                    .withId(TestModerationDiag.DIAG_ID1);

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
    public BiFunction<Long, Long, OldBanner> bannerSupplier;
    @Parameterized.Parameter(4)
    public Function<Long, Matcher<DefectInfo<Defect>>> defectSupplier;
    @Parameterized.Parameter(5)
    public Function<Long, Map<ModerationReasonObjectType, List<Long>>> modReasonObjectsSupplier;
    @Parameterized.Parameter(6)
    public List<ModerationReasonDetailed> reasons;
    @Parameterized.Parameter(7)
    public boolean operatorHasAccess;
    @Parameterized.Parameter(8)
    public boolean featureEnabled;

    @Autowired
    private Steps steps;
    @Autowired
    private BannerModerateService service;
    @Autowired
    private PpcPropertiesSupport ppcPropertiesSupport;

    private ClientInfo clientInfo;
    private long bannerId;
    private Long campaignId;
    private Long operatorUid;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][]{
                new Object[]{
                        "Объявление черновик",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusShow(false)
                                .withStatusModerate(OldBannerStatusModerate.NEW)),
                        createValidationError((id) -> validationError(path(index(0)),
                                remoderateDraftBanner())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "Объявление архивное",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusArchived(true)
                                .withStatusModerate(OldBannerStatusModerate.NO)),
                        createValidationError((id) -> validationError(path(index(0)),
                                moderateArchivedBanner())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "Кампания архивная",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)
                                .withArchived(true)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.NO)),
                        createValidationError((id) -> validationError(path(index(0)),
                                archivedCampaignModification())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "Кампания черновик",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.NEW)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.NO)),
                        createValidationError((id) -> validationError(path(index(0)),
                                remoderateBannerInDraftCampaign(id))),
                        createModReasonObjects(bannerId -> Map.of(ModerationReasonObjectType.BANNER,
                                List.of(bannerId))), List.of(REMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "У оператора нет доступа",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.NO)),
                        createValidationError((id) -> validationError(path(index(0)),
                                adNotFound())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), false, true
                },
                new Object[]{
                        "Выключена фича",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.NO)),
                        createValidationError((id) -> validationError(path(),
                                remoderateBannerIsNotAllowed())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), true, false
                },
                new Object[]{
                        "Баннер принят на модерации",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.YES)),
                        createValidationError((id) -> validationError(path(index(0)),
                                remoderateBannerWithNoModReasons())),
                        createModReasonObjects(bannerId -> emptyMap()), List.of(REMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "Отклонен по причине, не предусматривающей перемодерацию",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.YES)),
                        createValidationError((id) -> validationError(path(index(0)),
                                remoderateBannerWithNotAllowedModReasons(Set.of(UNREMODERATABLE_REASON.getId())))),
                        createModReasonObjects(bannerId -> Map.of(ModerationReasonObjectType.BANNER,
                                List.of(bannerId))), List.of(UNREMODERATABLE_REASON), true, true
                },
                new Object[]{
                        "Отклонен по нескольким причинам, одна из которых не предусматривает перемодерацию",
                        createCampaign(clientInfo -> newTextCampaign(clientInfo.getClientId(), clientInfo.getUid())
                                .withStatusModerate(StatusModerate.YES)),
                        createAdGroup(campaignId -> defaultTextAdGroup(campaignId)
                                .withStatusPostModerate(StatusPostModerate.NO)
                                .withStatusModerate(ru.yandex.direct.core.entity.adgroup.model.StatusModerate.YES)),
                        createBanner((campaignId, adGroupId) -> activeTextBanner(campaignId, adGroupId)
                                .withStatusModerate(OldBannerStatusModerate.YES)),
                        createValidationError((id) -> validationError(path(index(0)),
                                remoderateBannerWithNotAllowedModReasons(Set.of(UNREMODERATABLE_REASON.getId())))),
                        createModReasonObjects(bannerId -> Map.of(ModerationReasonObjectType.BANNER,
                                List.of(bannerId))), List.of(REMODERATABLE_REASON, UNREMODERATABLE_REASON),
                        true, true
                }
        };
    }

    public static Function<ClientInfo, Campaign> createCampaign(Function<ClientInfo, Campaign> fun) {
        return fun;
    }

    public static Function<Long, AdGroup> createAdGroup(Function<Long, AdGroup> fun) {
        return fun;
    }

    public static BiFunction<Long, Long, OldBanner> createBanner(BiFunction<Long, Long, OldBanner> fun) {
        return fun;
    }

    public static Function<Long, Matcher<DefectInfo<Defect>>> createValidationError(Function<Long,
            Matcher<DefectInfo<Defect>>> fun) {
        return fun;
    }

    public static Function<Long, Map<ModerationReasonObjectType, List<Long>>> createModReasonObjects(
            Function<Long, Map<ModerationReasonObjectType, List<Long>>> fun) {
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
        OldBanner banner = bannerSupplier.apply(campaignInfo.getCampaignId(), adGroupInfo.getAdGroupId());
        AbstractBannerInfo bannerInfo = steps.bannerSteps().createBanner(banner, adGroupInfo);
        steps.keywordSteps().createKeyword(adGroupInfo);
        bannerId = bannerInfo.getBanner().getId();
        campaignId = campaignInfo.getCampaignId();
        operatorUid = operatorHasAccess ? clientInfo.getUid() : steps.clientSteps().createDefaultClient().getUid();
        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.CLIENT_ALLOWED_TO_REMODERATE,
                featureEnabled);
        steps.moderationReasonSteps().insertRejectReasons(clientInfo.getShard(),
                modReasonObjectsSupplier.apply(bannerId), reasons);
    }

    @Test
    public void moderateTextBanner_unsuccess() {
        MassResult<Long> result = service.remoderateBanners(clientInfo.getClientId(), operatorUid,
                singletonList(bannerId));
        assertThat(result.getValidationResult(), hasDefectDefinitionWith(defectSupplier.apply(campaignId)));
    }
}
