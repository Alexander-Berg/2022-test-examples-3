package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.FeaturesInPPAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YpCookiesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingFeaturesInPPRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYpCookiesRequest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.TARGETING;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@ParametersAreNonnullByDefault
@RunWith(JUnitParamsRunner.class)
public class AdGroupGraphQLServiceAddTargetingsTest {
    private static final String GROUP_NAME = "aaaa";
    private static final long GROUP_LEVEL = 1L;
    private static final int RF = 3;
    private static final int RF_RESET = 1;
    private static final LocalDateTime START_TIME = LocalDateTime.now().withNano(0);
    private static final LocalDateTime FINISH_TIME = START_TIME.plusDays(100);
    private static final List<Integer> GROUP_REGION_IDS = singletonList((int) Region.MOSCOW_REGION_ID);
    private static final Set<String> STRING_SET = Set.of("1021110101545123184", "2021110101545123184");
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
            + "    addedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";
    private static final GraphQlTestExecutor.TemplateMutation<GdAddInternalAdGroups, GdAddAdGroupPayload> ADD_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>("addInternalAdGroups", ADD_MUTATION_TEMPLATE,
                    GdAddInternalAdGroups.class, GdAddAdGroupPayload.class);
    private static User operator;
    private static Long campaignId;
    private static Integer shard;
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Autowired
    GraphQlTestExecutor graphQlTestExecutor;
    @Autowired
    Steps steps;
    @Autowired
    AdGroupRepository adGroupRepository;
    @Autowired
    AdGroupAdditionalTargetingRepository adGroupAdditionalTargetingRepository;
    @Autowired
    UserRepository userRepository;

    public static Object[] testData() {
        return new Object[][]{
                {
                        "FeaturesInPPRequest",
                        new GdAdditionalTargetingUnion()
                                .withTargetingFeaturesInPP(createDefaultTargeting(GdAdditionalTargetingFeaturesInPPRequest::new)
                                .withValue(STRING_SET)),
                        FeaturesInPPAdGroupAdditionalTargeting.class
                },
                {
                        "YpCookiesRequest",
                        new GdAdditionalTargetingUnion()
                                .withTargetingYpCookies(createDefaultTargeting(GdAdditionalTargetingYpCookiesRequest::new)
                                .withValue(STRING_SET)),
                        YpCookiesAdGroupAdditionalTargeting.class
                }
        };
    }

    @Before
    public void before() {
        ClientInfo clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);
        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    @Parameters(method = "testData")
    @TestCaseName("addInternalAdGroupTargeting {0}")
    public void addInternalAdGroupTargeting(@SuppressWarnings("unused") String description,
                                            GdAdditionalTargetingUnion addedTargeting,
                                            Class<? extends AdGroupAdditionalTargeting> expectedTargetingClass) {
        var item = createAddItem(campaignId).withTargetings(List.of(addedTargeting));
        var request = createRequest(item);
        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);
        validateResponseSuccessful(payload);
        Long groupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);
        assertThat(actual).is(matchedBy(contains(createDefaultTargetingMatcher(groupId,
                expectedTargetingClass,
                hasProperty("value", equalTo(STRING_SET))))
        ));
    }

    private static <T extends GdAdditionalTargetingRequest> T createDefaultTargeting(Supplier<T> targetingCreator) {
        T targeting = targetingCreator.get();
        targeting
                .withTargetingMode(TARGETING)
                .withJoinType(ANY);
        return targeting;
    }

    private static GdAddInternalAdGroups createRequest(GdAddInternalAdGroupsItem... inputItems) {
        return new GdAddInternalAdGroups().withAddItems(asList(inputItems));
    }

    private static GdAddInternalAdGroupsItem createAddItem(Long campaignId) {
        return new GdAddInternalAdGroupsItem()
                .withCampaignId(campaignId)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME)
                .withRegionIds(GROUP_REGION_IDS);
    }

    private static Matcher<Object> createDefaultTargetingMatcher(Long adGroupId, Class<?
            extends AdGroupAdditionalTargeting> targeting, @Nullable Matcher<Object> valueMatcher) {
        Matcher<Object> commonMatcher = allOf(
                instanceOf(targeting),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("targetingMode", equalTo(AdGroupAdditionalTargetingMode.TARGETING)),
                hasProperty("joinType", equalTo(AdGroupAdditionalTargetingJoinType.ANY))
        );
        if (valueMatcher == null) {
            return commonMatcher;
        }
        return allOf(commonMatcher, valueMatcher);
    }
}
