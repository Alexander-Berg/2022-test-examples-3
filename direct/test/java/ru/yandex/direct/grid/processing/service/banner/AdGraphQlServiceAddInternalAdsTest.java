package ru.yandex.direct.grid.processing.service.banner;

import java.util.ArrayList;
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
import ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects;
import ru.yandex.direct.core.entity.internalads.restriction.InternalAdRestrictionDefects;
import ru.yandex.direct.core.entity.internalads.service.validation.defects.InternalAdDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.mock.TemplatePlaceRepositoryMockUtils;
import ru.yandex.direct.core.testing.mock.TemplateResourceRepositoryMockUtils;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.banner.GdInternalModerationInfo;
import ru.yandex.direct.grid.processing.model.banner.GdTemplateVariable;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddAdsPayload;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddInternalAds;
import ru.yandex.direct.grid.processing.model.banner.mutation.GdAddInternalAdsItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_INTERNAL_ADGROUP;
import static ru.yandex.direct.core.entity.banner.service.validation.BannerConstants.MAX_BANNERS_IN_INTERNAL_CAMPAIGN;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInAdGroup;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.maxBannersInInternalCampaign;
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
public class AdGraphQlServiceAddInternalAdsTest {
    private static final String BANNER_DESCRIPTION = "aaa";
    private static final long BANNER_TEMPLATE_ID = TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_1;
    private static final long BANNER_MODERATED_PLACE_TEMPLATE_ID = TemplatePlaceRepositoryMockUtils.PLACE_3_TEMPLATE_1;
    private static final long BANNER_VAR_TEMPLATE_RESOURCE_ID =
            TemplateResourceRepositoryMockUtils.TEMPLATE_1_RESOURCE_1_REQUIRED;
    private static final String BANNER_VAR_VALUE = "vvv";
    private static final long BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID =
            TemplateResourceRepositoryMockUtils.TEMPLATE_7_RESOURCE;
    private static final GdInternalModerationInfo MODERATION_INFO = new GdInternalModerationInfo()
            .withIsSecretAd(false)
            .withTicketUrl("https://st.yandex-team.ru/DIRECT-135207")
            .withCustomComment("comment")
            .withStatusShow(false)
            .withSendToModeration(false)
            .withStatusShowAfterModeration(true);

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private Steps steps;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BannerTypedRepository bannerTypedRepository;

    private static final String ADD_MUTATION_TEMPLATE = ""
            + "mutation {\n"
            + "  %s (input: %s) {\n"
            + "  \tvalidationResult {\n"
            + "      errors {\n"
            + "        code\n"
            + "        path\n"
            + "        params\n"
            + "      }\n"
            + "    }\n"
            + "    addedAds {\n"
            + "         id\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdAddInternalAds, GdAddAdsPayload> ADD_MUTATION =
            new TemplateMutation<>("addInternalAds", ADD_MUTATION_TEMPLATE,
                    GdAddInternalAds.class, GdAddAdsPayload.class);

    private ClientInfo clientInfo;
    private Integer shard;
    private User operator;
    private Long adGroupId;
    private AdGroupInfo adGroupInfo;
    private CampaignInfo campaignInfo;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, List.of(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        campaignInfo = steps.campaignSteps().createActiveInternalFreeCampaign(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void addInternalAds_DraftFalse_Success() {
        GdAddInternalAds input = createRequest(createCorrectAddItem(adGroupId), false);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        checkAddedBanner(payload);
    }

    @Test
    public void addInternalAds_DraftTrue_Success() {
        GdAddInternalAds input = createRequest(createCorrectAddItem(adGroupId), true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        checkAddedBanner(payload);
    }

    @Test
    public void addInternalAds_IncorrectTemplateId_ReturnsValidationError() {
        GdAddInternalAds input = createRequest(
                createCorrectAddItem(adGroupId).withTemplateId(Long.MAX_VALUE), true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.TEMPLATE_ID.name())),
                BannerDefects.internalTemplateNotAllowed())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_NullAtRequiredVarValue_ReturnsValidationError() {
        GdAddInternalAdsItem addItem = createCorrectAddItem(adGroupId);
        addItem.getTemplateVars().get(0).setValue(null);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                getResourceRestrictionsNotFollowedDefect(),
                toGdDefect(path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.TEMPLATE_VARS.name()),
                        index(0)),
                        CommonDefects.requiredButEmpty()))
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_EmptyStringAtRequiredVarValue_ReturnsRequestError() {
        GdAddInternalAdsItem addItem = createCorrectAddItem(adGroupId);
        addItem.getTemplateVars().get(0).setValue("");

        GdAddInternalAds input = createRequest(addItem, true);

        List<GraphQLError> graphQLErrors = graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void validate_CountBannersInGroup_MaxValue() {
        GdAddInternalAdsItem addItem = createCorrectAddItem(adGroupId);
        List<GdAddInternalAdsItem> addItems = new ArrayList<>();
        for (int i = 0; i < MAX_BANNERS_IN_INTERNAL_ADGROUP; i++) {
            addItems.add(addItem);
        }
        GdAddInternalAds input = createRequest(addItems, true);
        List<GraphQLError> graphQLErrors = graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator).getErrors();
        assertThat(graphQLErrors).isEmpty();
    }

    @Test
    public void validate_CountBannersInGroup_MoreThanMaxValue() {
        GdAddInternalAdsItem addItem = createCorrectAddItem(adGroupId);
        List<GdAddInternalAdsItem> addItems = new ArrayList<>();
        for (int i = 0; i < MAX_BANNERS_IN_INTERNAL_ADGROUP; i++) {
            addItems.add(addItem);
        }
        GdAddInternalAds input = createRequest(addItems, true);
        graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator);

        addItem = createCorrectAddItem(adGroupId);
        input = createRequest(addItem, true);
        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()), index(0)),
                maxBannersInAdGroup(MAX_BANNERS_IN_INTERNAL_ADGROUP), true)
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void validate_CountBannersInCampaign_MaxValue() {
        GdAddInternalAdsItem addItem;
        GdAddInternalAds input;
        List<GraphQLError> graphQLErrors;
        List<GdAddInternalAdsItem> addItems = new ArrayList<>();
        // из-за ограничения на количество объектов в запросе,
        // запрос падает с ошибкой, поэтому задействован цикл, чтобы облегчить запрос
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < MAX_BANNERS_IN_INTERNAL_CAMPAIGN / MAX_BANNERS_IN_INTERNAL_ADGROUP / 5; k++) {
                adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
                for (int i = 0; i < MAX_BANNERS_IN_INTERNAL_ADGROUP; i++) {
                    addItem = createCorrectAddItem(adGroupInfo.getAdGroupId());
                    addItems.add(addItem);
                }
            }
            input = createRequest(addItems, true);
            addItems = new ArrayList<>();
            graphQLErrors = graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator).getErrors();
            assertThat(graphQLErrors).isEmpty();
        }
    }

    @Test
    public void validate_CountBannersInCampaign_MoreThanMaxValue() {
        GdAddInternalAdsItem addItem;
        GdAddInternalAds input;
        List<GdAddInternalAdsItem> addItems = new ArrayList<>();
        // из-за ограничения на количество объектов в запросе,
        // запрос падает с ошибкой, поэтому задействован цикл, чтобы облегчить запрос
        for (int j = 0; j < 5; j++) {
            for (int k = 0; k < (MAX_BANNERS_IN_INTERNAL_CAMPAIGN / MAX_BANNERS_IN_INTERNAL_ADGROUP) / 5; k++) {
                adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
                for (int i = 0; i < MAX_BANNERS_IN_INTERNAL_ADGROUP; i++) {
                    addItem = createCorrectAddItem(adGroupInfo.getAdGroupId());
                    addItems.add(addItem);
                }
            }
            input = createRequest(addItems, true);
            addItems = new ArrayList<>();
            graphQlTestExecutor.doMutation(ADD_MUTATION, input, operator);
        }
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        addItem = createCorrectAddItem(adGroupInfo.getAdGroupId());
        input = createRequest(addItem, true);
        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()), index(0)),
                maxBannersInInternalCampaign(MAX_BANNERS_IN_INTERNAL_CAMPAIGN), true)
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_NullAtRequiredImageVarValue_ReturnsValidationError() {
        GdAddInternalAdsItem addItem = createAddItem(
                adGroupId, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE, null);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                getResourceRestrictionsNotFollowedDefect(),
                toGdDefect(path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.TEMPLATE_VARS.name()),
                        index(0)),
                        CommonDefects.requiredButEmpty()))
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_2AdsWithSameImage_Success() {
        BannerImageFormat bannerImageFormat = steps.bannerSteps().createBannerImageFormat(clientInfo);

        GdAddInternalAdsItem addItem1 = createAddItem(
                adGroupId, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE,
                bannerImageFormat.getImageHash());
        GdAddInternalAdsItem addItem2 = createAddItem(
                adGroupId, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE,
                bannerImageFormat.getImageHash());

        GdAddInternalAds input = createRequest(List.of(addItem1, addItem2), true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        validateResponseSuccessful(payload);

        checkAddedBannerEx(payload, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE,
                bannerImageFormat.getImageHash());
    }

    @Test
    public void addInternalAds_NullAtOptionalImageAndWrongUrl_ReturnsValidationError() {
        GdAddInternalAdsItem addItem = createAddItemEx(
                adGroupId, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_4_URL_IMG,
                List.of(
                        new GdTemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_4_RESOURCE_1_IMAGE)
                                .withValue(null),
                        new GdTemplateVariable()
                                .withTemplateResourceId(TemplateResourceRepositoryMockUtils.TEMPLATE_4_RESOURCE_2_REQUIRED_URL)
                                .withValue("yandex.ru")
                ), null);
        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.TEMPLATE_VARS.name()),
                        index(1)),
                new Defect<>(InternalAdRestrictionDefects.Url.URL_NOT_VALID))
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_InvalidHashAtRequiredImageVarValue_ReturnsValidationError() {
        GdAddInternalAdsItem addItem = createAddItem(
                adGroupId, TemplatePlaceRepositoryMockUtils.PLACE_1_TEMPLATE_3_WITH_IMAGE,
                TemplateResourceRepositoryMockUtils.TEMPLATE_3_RESOURCE_1_REQUIRED_IMAGE, "123123123123");

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.TEMPLATE_VARS.name()),
                        index(0)),
                CommonDefects.objectNotFound())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addInternalAds_WithModerationInfoForModeratedPlace_Success() {
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        GdAddInternalAdsItem addItem = createAddItemForModeratedPlace(adGroupId, MODERATION_INFO);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        validateResponseSuccessful(payload);

        checkAddedBannerWithModerationInfo(payload);
    }

    @Test
    public void addInternalAds_WithModerationInfoForNotModeratedPlace_ValidationError() {
        GdAddInternalAdsItem addItem = createCorrectAddItem(adGroupId).withModerationInfo(MODERATION_INFO);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.MODERATION_INFO.name())),
                CommonDefects.isNull())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAds_WithoutModerationInfoForModeratedPlace_ValidationError() {
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        GdAddInternalAdsItem addItem = createAddItemForModeratedPlace(adGroupId, null);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.MODERATION_INFO.name())),
                CommonDefects.notNull())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void addInternalAds_WithIncorrectTicketUrl_ValidationError() {
        campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaignWithModeratedPlace(clientInfo);
        adGroupInfo = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo);
        adGroupId = adGroupInfo.getAdGroupId();

        GdInternalModerationInfo moderationInfo = new GdInternalModerationInfo()
                .withIsSecretAd(false)
                .withTicketUrl("incorrect ticket url")
                .withCustomComment("comment")
                .withStatusShowAfterModeration(true)
                .withSendToModeration(false)
                .withStatusShow(true);

        GdAddInternalAdsItem addItem = createAddItemForModeratedPlace(adGroupId, moderationInfo);

        GdAddInternalAds input = createRequest(addItem, true);

        GdAddAdsPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                        index(0),
                        field(GdAddInternalAdsItem.MODERATION_INFO.name()),
                        field(GdInternalModerationInfo.TICKET_URL.name())),
                CommonDefects.invalidValue())
                .withWarnings(null);
        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private void checkAddedBanner(GdAddAdsPayload payload) {
        checkAddedBannerEx(payload, AdGraphQlServiceAddInternalAdsTest.BANNER_TEMPLATE_ID,
                AdGraphQlServiceAddInternalAdsTest.BANNER_VAR_TEMPLATE_RESOURCE_ID,
                AdGraphQlServiceAddInternalAdsTest.BANNER_VAR_VALUE);
    }

    private void checkAddedBannerWithModerationInfo(GdAddAdsPayload payload) {
        checkAddedBannerEx(payload, AdGraphQlServiceAddInternalAdsTest.BANNER_MODERATED_PLACE_TEMPLATE_ID,
                BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID, BANNER_VAR_VALUE, new InternalModerationInfo()
                        .withIsSecretAd(MODERATION_INFO.getIsSecretAd())
                        .withCustomComment(MODERATION_INFO.getCustomComment())
                        .withTicketUrl(MODERATION_INFO.getTicketUrl())
                        .withSendToModeration(false)
                        .withStatusShowAfterModeration(MODERATION_INFO.getStatusShowAfterModeration()),
                MODERATION_INFO.getStatusShow(), BannerStatusModerate.NEW);
    }

    private void checkAddedBannerEx(GdAddAdsPayload payload, long templateId, long resourceId, String resourceValue) {
        checkAddedBannerEx(payload, templateId, resourceId, resourceValue, null, true, BannerStatusModerate.YES);
    }

    private void checkAddedBannerEx(GdAddAdsPayload payload, long templateId, long resourceId, String resourceValue,
                                    InternalModerationInfo moderationInfo, boolean statusShow,
                                    BannerStatusModerate statusModerate) {
        Long adId = payload.getAddedAds().get(0).getId();
        Banner actualBanner = bannerTypedRepository.getBanners(shard, List.of(adId), null).get(0);

        Banner expectedBanner = new InternalBanner()
                .withAdGroupId(adGroupId)
                .withDescription(BANNER_DESCRIPTION)
                .withTemplateId(templateId)
                .withTemplateVariables(List.of(
                        new TemplateVariable()
                                .withTemplateResourceId(resourceId)
                                .withInternalValue(resourceValue)))
                .withModerationInfo(moderationInfo)
                .withIsStoppedByUrlMonitoring(false)
                .withStatusShow(statusShow)
                .withStatusModerate(statusModerate)
                .withStatusPostModerate(statusModerate == BannerStatusModerate.YES
                        ? BannerStatusPostModerate.YES : BannerStatusPostModerate.NO);

        assertThat(actualBanner).is(
                matchedBy(beanDiffer(expectedBanner).useCompareStrategy(onlyExpectedFields())));
    }

    private GdAddInternalAds createRequest(GdAddInternalAdsItem inputItem, boolean saveDraft) {
        return createRequest(List.of(inputItem), saveDraft);
    }

    private GdAddInternalAds createRequest(List<GdAddInternalAdsItem> inputItems, boolean saveDraft) {
        return new GdAddInternalAds()
                .withSaveDraft(saveDraft)
                .withAdAddItems(inputItems);
    }

    private GdAddInternalAdsItem createCorrectAddItem(Long adGroupId) {
        return createAddItem(adGroupId, BANNER_TEMPLATE_ID, BANNER_VAR_TEMPLATE_RESOURCE_ID, BANNER_VAR_VALUE);
    }

    private GdAddInternalAdsItem createAddItem(Long adGroupId, long templateId, long resourceId, String value) {
        GdTemplateVariable templateVariable = new GdTemplateVariable()
                .withTemplateResourceId(resourceId)
                .withValue(value);

        return createAddItemEx(adGroupId, templateId, List.of(templateVariable), null);
    }

    private GdAddInternalAdsItem createAddItemForModeratedPlace(Long adGroupId,
                                                                GdInternalModerationInfo moderationInfo) {
        GdTemplateVariable templateVariable = new GdTemplateVariable()
                .withTemplateResourceId(BANNER_MODERATED_PLACE_VAR_TEMPLATE_RESOURCE_ID)
                .withValue(BANNER_VAR_VALUE);

        return createAddItemEx(
                adGroupId, BANNER_MODERATED_PLACE_TEMPLATE_ID, List.of(templateVariable), moderationInfo);
    }

    private GdAddInternalAdsItem createAddItemEx(Long adGroupId, long templateId, List<GdTemplateVariable> variables,
                                                 GdInternalModerationInfo moderationInfo) {
        return new GdAddInternalAdsItem()
                .withAdGroupId(adGroupId)
                .withDescription(BANNER_DESCRIPTION)
                .withTemplateId(templateId)
                .withTemplateVars(variables)
                .withModerationInfo(moderationInfo);
    }

    private static GdDefect getResourceRestrictionsNotFollowedDefect() {
        return toGdDefect(path(field(GdAddInternalAds.AD_ADD_ITEMS.name()),
                index(0),
                field(GdAddInternalAdsItem.TEMPLATE_VARS.name())),
                InternalAdDefects.resourceRestrictionsNotFollowed(DEFAULT_RESOURCE_RESTRICTIONS_ERROR_MESSAGE));
    }

}
