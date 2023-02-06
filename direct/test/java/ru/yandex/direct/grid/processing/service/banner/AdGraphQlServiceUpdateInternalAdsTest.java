package ru.yandex.direct.grid.processing.service.banner;

import java.util.List;

import graphql.GraphQLError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.banner.model.Banner;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.core.entity.banner.model.BannerStatusPostModerate;
import ru.yandex.direct.core.entity.banner.model.InternalBanner;
import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.core.entity.banner.model.TemplateVariable;
import ru.yandex.direct.core.entity.banner.repository.BannerTypedRepository;
import ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.InternalBannerInfo;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdInternalModerationInfo;
import ru.yandex.direct.grid.processing.model.banner.GdTemplateVariable;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateInternalAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdUpdateInternalAdsItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.defect.CommonDefects;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.internalads.service.TemplateInfoService.DEFAULT_RESOURCE_RESTRICTIONS_ERROR_MESSAGE;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGraphQlServiceUpdateInternalAdsTest {
    private static final String NEW_BANNER_DESCRIPTION = "aaa";
    private static final long BANNER_VAR_TEMPLATE_RESOURCE_ID =
            TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED;
    private static final long BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID =
            TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE;
    private static final String NEW_BANNER_VAR_VALUE = "912837";

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    Steps steps;

    @Autowired
    private BannerTypedRepository bannerRepository;

    @Autowired
    UserRepository userRepository;

    private static final String UPDATE_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    updatedAds {\n"
            + "         id\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdUpdateInternalAds, GdUpdateAdsPayload> UPDATE_MUTATION =
            new TemplateMutation<>("updateInternalAds", UPDATE_MUTATION_TEMPLATE,
                    GdUpdateInternalAds.class, GdUpdateAdsPayload.class);


    private Integer shard;
    private User operator;
    private Long adGroupId;
    private Long existentBannerId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        AdGroupInfo adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        InternalBannerInfo bannerInfo = steps.bannerSteps().createActiveInternalBanner(adGroupInfo);
        existentBannerId = bannerInfo.getBannerId();
    }

    @Test
    public void updateInternalAds_DraftFalse_Success() {
        GdUpdateInternalAds input = createRequest(createCorrectUpdateItem(existentBannerId), false);

        GdUpdateAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        checkUpdatedBanner(payload, BANNER_VAR_TEMPLATE_RESOURCE_ID);
    }

    @Test
    public void updateInternalAds_DraftTrue_Success() {
        GdUpdateInternalAds input = createRequest(createCorrectUpdateItem(existentBannerId), true);

        GdUpdateAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        checkUpdatedBanner(payload, BANNER_VAR_TEMPLATE_RESOURCE_ID);
    }

    @Test
    public void updateInternalAds_NullAtRequiredVarValue_ReturnsValidationError() {
        GdUpdateInternalAdsItem updateItem = createCorrectUpdateItem(existentBannerId);
        updateItem.getTemplateVars().get(0).setValue(null);

        GdUpdateInternalAds input = createRequest(updateItem, true);

        GdUpdateAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                getResourceRestrictionsNotFollowedDefect(),
                toGdDefect(path(field(GdUpdateInternalAds.AD_UPDATE_ITEMS.name()),
                        index(0),
                        field(GdUpdateInternalAdsItem.TEMPLATE_VARS.name()),
                        index(0)),
                        CommonDefects.requiredButEmpty()))
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateInternalAds_EmptyStringAtRequiredVarValue_ReturnsRequestError() {
        GdUpdateInternalAdsItem updateItem = createCorrectUpdateItem(existentBannerId);
        updateItem.getTemplateVars().get(0).setValue("");

        GdUpdateInternalAds input = createRequest(updateItem, true);

        List<GraphQLError> graphQLErrors = graphQlTestExecutor.doMutation(UPDATE_MUTATION, input, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void updateInternalAds_WithModerationInfo_Success() {
        initAdGroupIdForModeratedPlace();

        GdInternalModerationInfo moderationInfo = new GdInternalModerationInfo()
                .withIsSecretAd(false)
                .withTicketUrl("https://st.yandex-team.ru/DIRECT-135207")
                .withCustomComment("comment")
                .withStatusShow(false)
                .withSendToModeration(false)
                .withStatusShowAfterModeration(false);

        GdUpdateInternalAdsItem updateItem =
                createCorrectUpdateItemWithModerationInfo(existentBannerId, moderationInfo);

        GdUpdateInternalAds input = createRequest(updateItem, true);

        GdUpdateAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        validateResponseSuccessful(payload);

        checkUpdatedBanner(payload, BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID, new InternalModerationInfo()
                        .withIsSecretAd(moderationInfo.getIsSecretAd())
                        .withTicketUrl(moderationInfo.getTicketUrl())
                        .withCustomComment(moderationInfo.getCustomComment())
                        .withSendToModeration(false)
                        .withStatusShowAfterModeration(moderationInfo.getStatusShowAfterModeration()),
                moderationInfo.getStatusShow(), BannerStatusModerate.NEW);
    }

    @Test
    public void updateInternalAds_WithIncorrectTicketUrl_ValidationError() {
        initAdGroupIdForModeratedPlace();

        GdInternalModerationInfo moderationInfo = new GdInternalModerationInfo()
                .withIsSecretAd(false)
                .withTicketUrl("incorrect ticket url")
                .withCustomComment("comment")
                .withStatusShow(true)
                .withSendToModeration(false)
                .withStatusShowAfterModeration(true);

        GdUpdateInternalAdsItem updateItem =
                createCorrectUpdateItemWithModerationInfo(existentBannerId, moderationInfo);

        GdUpdateInternalAds input = createRequest(updateItem, true);

        GdUpdateAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAds.AD_UPDATE_ITEMS.name()),
                        index(0),
                        field(GdUpdateInternalAdsItem.MODERATION_INFO.name()),
                        field(GdInternalModerationInfo.TICKET_URL.name())),
                CommonDefects.invalidValue())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private void checkUpdatedBanner(GdUpdateAdsPayload payload, Long resourceId) {
        checkUpdatedBanner(payload, resourceId, null, true, BannerStatusModerate.YES);
    }

    private void checkUpdatedBanner(GdUpdateAdsPayload payload, Long resourceId,
                                    InternalModerationInfo moderationInfo, boolean statusShow,
                                    BannerStatusModerate statusModerate) {
        Long adId = payload.getUpdatedAds().get(0).getId();
        Banner actualBanner = bannerRepository.getBanners(shard, singletonList(adId), null).get(0);

        Banner expectedBanner = new InternalBanner()
                .withId(adId)
                .withAdGroupId(adGroupId)
                .withDescription(NEW_BANNER_DESCRIPTION)
                .withTemplateVariables(singletonList(
                        new TemplateVariable()
                                .withTemplateResourceId(resourceId)
                                .withInternalValue(NEW_BANNER_VAR_VALUE)))
                .withModerationInfo(moderationInfo)
                .withIsStoppedByUrlMonitoring(false)
                .withStatusShow(statusShow)
                .withStatusModerate(statusModerate)
                .withStatusPostModerate(statusModerate == BannerStatusModerate.YES
                        ? BannerStatusPostModerate.YES : BannerStatusPostModerate.NO);

        assertThat(actualBanner).is(
                matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private GdUpdateInternalAds createRequest(GdUpdateInternalAdsItem inputItem, boolean saveDraft) {
        return new GdUpdateInternalAds()
                .withSaveDraft(saveDraft)
                .withAdUpdateItems(singletonList(inputItem));
    }

    private GdUpdateInternalAdsItem createCorrectUpdateItem(Long bannerId) {
        return createCorrectUpdateItemEx(bannerId, BANNER_VAR_TEMPLATE_RESOURCE_ID, null);
    }

    private GdUpdateInternalAdsItem createCorrectUpdateItemWithModerationInfo(Long bannerId,
                                                                              GdInternalModerationInfo moderationInfo) {
        return createCorrectUpdateItemEx(bannerId, BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID, moderationInfo);
    }

    private GdUpdateInternalAdsItem createCorrectUpdateItemEx(Long bannerId, Long resourceId,
                                                              GdInternalModerationInfo moderationInfo) {
        GdTemplateVariable templateVariable = new GdTemplateVariable()
                .withTemplateResourceId(resourceId)
                .withValue(NEW_BANNER_VAR_VALUE);

        return new GdUpdateInternalAdsItem()
                .withId(bannerId)
                .withDescription(NEW_BANNER_DESCRIPTION)
                .withTemplateVars(singletonList(templateVariable))
                .withModerationInfo(moderationInfo);
    }

    private static GdDefect getResourceRestrictionsNotFollowedDefect() {
        return toGdDefect(path(field(GdUpdateInternalAds.AD_UPDATE_ITEMS.name()),
                index(0),
                field(GdUpdateInternalAdsItem.TEMPLATE_VARS.name())),
                InternalAdDefects.resourceRestrictionsNotFollowed(DEFAULT_RESOURCE_RESTRICTIONS_ERROR_MESSAGE));
    }

    private void initAdGroupIdForModeratedPlace() {
        CampaignInfo campaignInfo =
                steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupId = adGroup.getAdGroupId();

        var bannerInfo = steps.internalBannerSteps()
                .createModeratedInternalBanner(adGroup, BannerStatusModerate.YES);
        existentBannerId = bannerInfo.getBannerId();
    }

}
