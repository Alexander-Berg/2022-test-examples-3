package ru.yandex.direct.grid.processing.service.group;

import java.util.List;

import com.fasterxml.jackson.databind.util.RawValue;
import com.google.common.collect.Iterables;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.DynamicTextAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.bidmodifier.BidModifier;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobile;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierMobileAdjustment;
import ru.yandex.direct.core.entity.bidmodifier.BidModifierType;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierLevel;
import ru.yandex.direct.core.entity.bidmodifiers.repository.BidModifierRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.DynamicBannerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdDefect;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobile;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifierMobileAdjustmentItem;
import ru.yandex.direct.grid.processing.model.bidmodifier.mutation.GdUpdateBidModifiers;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayloadItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateDynamicAdGroup;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateDynamicAdGroupItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.defect.CollectionDefects;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestBanners.activeDynamicBanner;
import static ru.yandex.direct.grid.processing.model.bidmodifier.GdBidModifierType.MOBILE_MULTIPLIER;
import static ru.yandex.direct.grid.processing.util.GraphQLUtils.map;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceUpdateDynamicTest {
    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    private Steps steps;
    @Autowired
    private AdGroupRepository adGroupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BidModifierRepository bidModifierRepository;

    private static final String NEW_GROUP_NAME = "new name";
    private static final Long NEW_REGION_ID = Region.MOSCOW_REGION_ID;
    private static final List<String> NEW_MINUS_KEYWORDS = asList("word1", "word2");
    private static final String NEW_TRACKING_PARAMS = "utm_source=yandex&utm_medium=cpc";
    private static final Integer NEW_BID_MODIFIER_PERCENT = 110;
    public static final String NEW_FIELD_TO_USE_AS_NAME = "new_field_name";
    public static final String NEW_FIELD_TO_USE_AS_BODY = "new_field_body";

    private static final String UPDATE_MUTATION_NAME = "updateDynamicAdGroups";
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
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateDynamicAdGroup, GdUpdateAdGroupPayload> UPDATE_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>(UPDATE_MUTATION_NAME, UPDATE_MUTATION_TEMPLATE,
                    GdUpdateDynamicAdGroup.class, GdUpdateAdGroupPayload.class);

    private Integer shard;
    private User operator;
    private Long adGroupId;
    private AdGroupInfo adGroupInfo;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        shard = clientInfo.getShard();

        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        adGroupInfo = steps.adGroupSteps().createActiveDynamicTextAdGroup(clientInfo);
        adGroupId = adGroupInfo.getAdGroupId();
    }

    @Test
    public void updateDynamicAdGroups_success() {
        GdUpdateDynamicAdGroup input = createRequest(
                createCorrectUpdateItem(adGroupId).withTrackingParams(NEW_TRACKING_PARAMS));

        graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        DynamicTextAdGroup expectedAdGroup = new DynamicTextAdGroup()
                .withType(AdGroupType.DYNAMIC)
                .withName(NEW_GROUP_NAME)
                .withGeo(singletonList(NEW_REGION_ID))
                .withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withTrackingParams(NEW_TRACKING_PARAMS)
                .withFieldToUseAsName(NEW_FIELD_TO_USE_AS_NAME)
                .withFieldToUseAsBody(NEW_FIELD_TO_USE_AS_BODY);

        assertThat(actualAdGroup).is(matchedBy(beanDiffer(expectedAdGroup).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateDynamicAdGroups_checkPayload() {
        GdUpdateDynamicAdGroup input = createRequest(createCorrectUpdateItem(adGroupId));

        GdUpdateAdGroupPayload actualPayload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input,
                operator);
        GdUpdateAdGroupPayload expectedPayload = new GdUpdateAdGroupPayload()
                .withUpdatedAdGroupItems(singletonList(new GdUpdateAdGroupPayloadItem()
                        .withAdGroupId(adGroupId)));

        assertThat(actualPayload).is(matchedBy(beanDiffer(expectedPayload)));
    }

    @Test
    public void updateDynamicAdGroup_validationError_whenTooLongTrackingParams() {
        String trackingParams = "a=" + RandomStringUtils.randomAlphanumeric(1024);
        GdUpdateDynamicAdGroup input = createRequest(
                createCorrectUpdateItem(adGroupId).withTrackingParams(trackingParams));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateDynamicAdGroup.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateDynamicAdGroupItem.TRACKING_PARAMS.name())),
                CollectionDefects.maxStringLength(1024),
                true)
                .withWarnings(null);
        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void updateDynamicAdGroups_withLibraryMinusKeywordsIds() {
        Long newPackId = steps.minusKeywordsPackSteps()
                .createMinusKeywordsPack(adGroupInfo.getClientInfo())
                .getMinusKeywordPackId();

        GdUpdateDynamicAdGroup input = createRequest(
                createCorrectUpdateItem(adGroupId).withLibraryMinusKeywordsIds(singletonList(newPackId)));

        graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(adGroupId)).get(0);
        assertThat(actualAdGroup.getLibraryMinusKeywordsIds()).containsExactly(newPackId);
    }

    @Test
    public void updateDynamicAdGroups_withBidModifiers() {
        GdUpdateBidModifiers gdBidModifiers = new GdUpdateBidModifiers()
                .withBidModifierMobile(new GdUpdateBidModifierMobile()
                        .withAdGroupId(adGroupId)
                        .withCampaignId(adGroupInfo.getCampaignId())
                        .withAdjustment(new GdUpdateBidModifierMobileAdjustmentItem()
                                .withPercent(NEW_BID_MODIFIER_PERCENT))
                        .withEnabled(true)
                        .withType(MOBILE_MULTIPLIER));

        GdUpdateDynamicAdGroup input = createRequest(
                createCorrectUpdateItem(adGroupId).withBidModifiers(gdBidModifiers));

        graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        List<BidModifier> actualBidModifiers = bidModifierRepository
                .getByAdGroupIds(shard,
                        singletonMap(adGroupId, adGroupInfo.getCampaignId()),
                        singleton(BidModifierType.MOBILE_MULTIPLIER),
                        singleton(BidModifierLevel.ADGROUP));

        BidModifier expectedBidModifier = new BidModifierMobile()
                .withEnabled(true)
                .withMobileAdjustment(new BidModifierMobileAdjustment()
                        .withPercent(NEW_BID_MODIFIER_PERCENT));

        assertThat(actualBidModifiers).is(matchedBy(beanDiffer(singletonList(expectedBidModifier))
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void updateDynamicAdGroup_validationError_whenGeoNotCorrespondWithBannerLang() {
        steps.adGroupSteps().setAdGroupProperty(adGroupInfo, AdGroup.GEO, singletonList(Region.TURKEY_REGION_ID));
        var banner = activeDynamicBanner(adGroupInfo.getCampaignId(), adGroupInfo.getAdGroupId())
                .withBody("Türkçe")
                .withLanguage(null);
        DynamicBannerInfo bannerInfo = steps.bannerSteps().createActiveDynamicBanner(banner, adGroupInfo);

        GdUpdateDynamicAdGroup input = createRequest(
                createCorrectUpdateItem(adGroupId)
                        .withRegionIds(singletonList((int) Region.MOSCOW_REGION_ID)));
        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        SoftAssertions softAssertions = new SoftAssertions();

        List<Long> actualGeo = adGroupRepository.getAdGroups(shard, singleton(adGroupId)).get(0).getGeo();
        softAssertions.assertThat(actualGeo).as("actualGeo").containsExactly(Region.TURKEY_REGION_ID);

        GdDefect defect = Iterables.getFirst(payload.getValidationResult().getErrors(), null);
        GdDefect expectedDefect = new GdDefect()
                .withCode("AdGroupDefectIds.Geo.BAD_GEO")
                .withParams(map("language", new RawValue("TURKISH"),
                        "bannerId", bannerInfo.getBannerId()))
                .withPath("updateItems[0].geo");
        softAssertions.assertThat(defect).as("error").isEqualTo(expectedDefect);
        softAssertions.assertThat(payload.getValidationResult().getErrors()).hasSize(1);

        softAssertions.assertAll();
    }

    private GdUpdateDynamicAdGroup createRequest(GdUpdateDynamicAdGroupItem adGroupItem) {
        return new GdUpdateDynamicAdGroup().withUpdateItems(singletonList(adGroupItem));
    }

    private GdUpdateDynamicAdGroupItem createCorrectUpdateItem(Long adGroupId) {
        return new GdUpdateDynamicAdGroupItem()
                .withId(adGroupId)
                .withName(NEW_GROUP_NAME)
                .withRegionIds(singletonList(NEW_REGION_ID.intValue()))
                .withMinusKeywords(NEW_MINUS_KEYWORDS)
                .withBidModifiers(new GdUpdateBidModifiers())
                .withFieldToUseAsName(NEW_FIELD_TO_USE_AS_NAME)
                .withFieldToUseAsBody(NEW_FIELD_TO_USE_AS_BODY);
    }
}
