package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import graphql.ExecutionResult;
import graphql.GraphQLError;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.adgroup.model.InternalAdGroup;
import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroup.service.validation.AdGroupDefects;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledApp;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.MobileInstalledAppsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.ShowDatesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEngine;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.BrowserEnginesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.region.validation.RegionIdDefects;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.model.api.GdValidationResult;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingVersioned;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingBrowserEnginesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingMobileInstalledAppsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingShowDatesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingTimeRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.service.validation.GridDefectIds;
import ru.yandex.direct.grid.processing.util.GraphQLUtils;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.TemplateMutation;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.differ.AlwaysEqualsDiffer;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationHelper.toGdValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.mapSet;
import static ru.yandex.direct.utils.ListUtils.integerToLongList;
import static ru.yandex.direct.utils.ListUtils.longToIntegerList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class AdGroupGraphQlServiceUpdateInternalTest {

    private static final List<Integer> NEW_GROUP_REGION_IDS = singletonList(
            Long.valueOf(Region.SAINT_PETERSBURG_REGION_ID).intValue());
    private static final String NEW_GROUP_NAME = "bbbb";
    private static final long OLD_GROUP_LEVEL = 0L;
    private static final long NEW_GROUP_LEVEL = 99L;
    private static final int OLD_RF = 5;
    private static final int OLD_RF_RESET = 2;
    private static final int OLD_MAX_CLICKS_COUNT = 5;
    private static final int OLD_MAX_CLICKS_PERIOD = 60;
    private static final int OLD_MAX_STOPS_COUNT = 10;
    private static final int OLD_MAX_STOPS_PERIOD = 300;
    private static final int RF = 3;
    private static final int RF_RESET = 1;
    private static final int MAX_CLICKS_COUNT = 7;
    private static final int MAX_CLICKS_PERIOD = 100;
    private static final int MAX_STOPS_COUNT = 8;
    private static final int MAX_STOPS_PERIOD = 200;
    private static final LocalDateTime START_TIME = LocalDateTime.now().withNano(0);
    private static final LocalDateTime FINISH_TIME = START_TIME.plusDays(100);
    private static final Set<String> MOBILE_INSTALLED_APPS_URLS = ImmutableSet.of(
            "http://play.google.com/store/apps/details?id=ru.yandex.searchplugin",
            "https://apps.apple.com/ru/app/andeks-market/id425354015");
    private static final Set<String> INVALID_MOBILE_INSTALLED_APPS_URLS = ImmutableSet.of(
            "http://play.google.com/store/apps/details?id=ru.yandex.searchplugin",
            "https://yandex.ru",
            "https://apps.apple.com/ru/app/andeks-market/id425354015");

    private static final DefaultCompareStrategy COMPARE_STRATEGY_ALL_BUT_LAST_CHANGE = onlyExpectedFields()
            .forFields(newPath("lastChange"), newPath("lastChange")).useDiffer(new AlwaysEqualsDiffer());

    private static final DefaultCompareStrategy COMPARE_STRATEGY_ALL_BUT_COMPUTED_FIELDS = onlyExpectedFields()
            .forFields(newPath("lastChange"), newPath("lastChange")).useDiffer(new AlwaysEqualsDiffer())
            .forFields(newPath("statusBsSynced"), newPath("statusBsSynced")).useDiffer(new AlwaysEqualsDiffer())
            .forFields(newPath("statusShowsForecast"), newPath("statusShowsForecast")).useDiffer(new AlwaysEqualsDiffer());

    private static final DefaultCompareStrategy COMPARE_STRATEGY_ALL_BUT_ID = onlyExpectedFields()
            .forFields(newPath("id"), newPath("id")).useDiffer(new AlwaysEqualsDiffer());

    private static final DefaultCompareStrategy COMPARE_STRATEGY_ALL_BUT_ID_AND_VALUE = onlyExpectedFields()
            .forFields(newPath("id"), newPath("id")).useDiffer(new AlwaysEqualsDiffer())
            .forFields(newPath("value"), newPath("value")).useDiffer(new AlwaysEqualsDiffer());

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    Steps steps;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    AdGroupAdditionalTargetingRepository adGroupAdditionalTargetingRepository;

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
            + "    updatedAdGroupItems {\n"
            + "         adGroupId,\n"
            + "     }\n"
            + "  }\n"
            + "}";

    private static final TemplateMutation<GdUpdateInternalAdGroups, GdUpdateAdGroupPayload> UPDATE_MUTATION =
            new TemplateMutation<>("updateInternalAdGroups", UPDATE_MUTATION_TEMPLATE,
                    GdUpdateInternalAdGroups.class, GdUpdateAdGroupPayload.class);


    private Integer shard;
    private User operator;
    private Long existentAdGroupId;

    @Before
    public void before() {
        ClientInfo clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveInternalAdGroup(
                campaignInfo, OLD_GROUP_LEVEL, OLD_RF, OLD_RF_RESET,
                OLD_MAX_CLICKS_COUNT, OLD_MAX_CLICKS_PERIOD, OLD_MAX_STOPS_COUNT, OLD_MAX_STOPS_PERIOD);
        existentAdGroupId = adGroup.getAdGroupId();
    }

    @Test
    public void updateInternalAdGroup_Success() {
        GdUpdateInternalAdGroups input = createRequest(createCorrectUpdateItem(existentAdGroupId));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_WithoutRf_Success() {
        GdUpdateInternalAdGroups input = createRequest(createCorrectUpdateItem(existentAdGroupId)
                .withRf(null)
                .withRfReset(null)
                .withMaxClicksCount(null)
                .withMaxClicksPeriod(null)
                .withMaxStopsCount(null)
                .withMaxStopsPeriod(null));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(null)
                .withRfReset(null)
                .withMaxClicksCount(null)
                .withMaxClicksPeriod(null)
                .withMaxStopsCount(null)
                .withMaxStopsPeriod(null)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_WithInvalidRequest_ReturnsRequestError() {
        GdUpdateInternalAdGroups input = createRequest(createCorrectUpdateItem(null));

        List<GraphQLError> graphQLErrors = graphQlTestExecutor.doMutation(UPDATE_MUTATION, input, operator).getErrors();
        assertThat(graphQLErrors).isNotEmpty();
    }

    @Test
    public void updateInternalAdGroup_WithNonExistentRegion_ReturnsValidationError() {
        GdUpdateInternalAdGroups input = createRequest(
                createCorrectUpdateItem(existentAdGroupId).withRegionIds(singletonList(Integer.MAX_VALUE)));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateInternalAdGroupsItem.REGION_IDS.name())),
                RegionIdDefects.geoIncorrectRegions(Objects.toString(Integer.MAX_VALUE)))
                .withWarnings(null);

        DefaultCompareStrategy validationCompareStrategy = DefaultCompareStrategies.allFieldsExcept(
                newPath("errors", "\\d+", "params"));

        assertThat(payload.getValidationResult())
                .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(validationCompareStrategy)));
    }

    @Test
    public void updateInternalAdGroup_OneValidAndOneWithNonExistentCampaignId_ReturnsNullForInvalid() {
        GdUpdateInternalAdGroups input = createRequest(
                createCorrectUpdateItem(Long.MAX_VALUE), // incorrect
                createCorrectUpdateItem(existentAdGroupId));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(payload.getUpdatedAdGroupItems()).hasSize(2);

            //noinspection unchecked
            softly.assertThat(payload.getUpdatedAdGroupItems()).is(
                    matchedBy(contains(nullValue(), notNullValue())));

            GdValidationResult expectedGdValidationResult = toGdValidationResult(
                    path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()),
                            index(0),
                            field(GdUpdateInternalAdGroupsItem.ID.name())),
                    CommonDefects.objectNotFound())
                    .withWarnings(null);

            assertThat(payload.getValidationResult())
                    .is(matchedBy(beanDiffer(expectedGdValidationResult).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateVersionedTargeting_Success() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        AdGroup adGroupBeforeChange = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingBrowserEngines(new GdAdditionalTargetingBrowserEnginesRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(singletonList(new GdAdditionalTargetingVersioned().withTargetingValueEntryId(1L)
                                .withMinVersion("1.1").withMaxVersion("2.2"))));
        updateItem = createCorrectUpdateItem(existentAdGroupId).withTargetings(List.of(union));
        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        AdGroup adGroupAfterChange = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, existentAdGroupId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adGroupAfterChange).is(
                    matchedBy(beanDiffer(adGroupBeforeChange).useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_LAST_CHANGE)));
            softly.assertThat(targetingList.size()).isEqualTo(1);
            softly.assertThat(targetingList.get(0)).is(matchedBy(beanDiffer(new BrowserEnginesAdGroupAdditionalTargeting()
                    .withAdGroupId(existentAdGroupId)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(singletonList(new BrowserEngine().withTargetingValueEntryId(1L)
                            .withMinVersion("1.1").withMaxVersion("2.2"))))
                    .useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_ID)));
        });

        union = new GdAdditionalTargetingUnion()
                .withTargetingBrowserEngines(new GdAdditionalTargetingBrowserEnginesRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(singletonList(new GdAdditionalTargetingVersioned().withTargetingValueEntryId(1L))));
        updateItem = createCorrectUpdateItem(existentAdGroupId).withTargetings(List.of(union));
        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        AdGroup adGroupAfterSecondChange =
                adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        List<AdGroupAdditionalTargeting> targetingList2 =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, existentAdGroupId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adGroupAfterSecondChange).is(
                    matchedBy(beanDiffer(adGroupBeforeChange).useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_LAST_CHANGE)));
            softly.assertThat(targetingList2.size()).isEqualTo(1);
            softly.assertThat(targetingList2.get(0)).is(matchedBy(beanDiffer(new BrowserEnginesAdGroupAdditionalTargeting()
                    .withAdGroupId(existentAdGroupId)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(singletonList(new BrowserEngine().withTargetingValueEntryId(1L))))
                    .useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_ID)));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateShowDates_Success() {
        InternalAdGroup adGroupBeforeChange = (InternalAdGroup) adGroupRepository.getAdGroups(shard,
                singletonList(existentAdGroupId)).get(0);

        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingShowDates(new GdAdditionalTargetingShowDatesRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(Set.of(
                                LocalDate.of(2018, 1, 1),
                                LocalDate.of(2020, 12, 31))));
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withName(adGroupBeforeChange.getName())
                .withLevel(adGroupBeforeChange.getLevel())
                .withRf(adGroupBeforeChange.getRf())
                .withRfReset(adGroupBeforeChange.getRfReset())
                .withMaxClicksCount(adGroupBeforeChange.getMaxClicksCount())
                .withMaxClicksPeriod(adGroupBeforeChange.getMaxClicksPeriod())
                .withMaxStopsCount(adGroupBeforeChange.getMaxStopsCount())
                .withMaxStopsPeriod(adGroupBeforeChange.getMaxStopsPeriod())
                .withStartTime(adGroupBeforeChange.getStartTime())
                .withFinishTime(adGroupBeforeChange.getFinishTime())
                .withRegionIds(longToIntegerList(adGroupBeforeChange.getGeo()))
                .withTargetings(List.of(union));

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        AdGroup adGroupAfterChange = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, existentAdGroupId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adGroupAfterChange).is(
                    matchedBy(beanDiffer(adGroupBeforeChange).useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_COMPUTED_FIELDS)));
            softly.assertThat(targetingList.size()).isEqualTo(1);
            softly.assertThat(targetingList.get(0)).is(matchedBy(beanDiffer(new ShowDatesAdGroupAdditionalTargeting()
                    .withAdGroupId(existentAdGroupId)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                    .withValue(Set.of(
                            LocalDate.of(2018, 1, 1),
                            LocalDate.of(2020, 12, 31))))
                    .useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_ID)));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateShowDates_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingShowDates(new GdAdditionalTargetingShowDatesRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(emptySet()));
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withTargetings(List.of(union));

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_SHOW_DATES),
                        field(GdAdditionalTargetingShowDatesRequest.VALUE.name())),
                CollectionDefects.notEmptyCollection())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void updateInternalAdGroup_UpdateMobileInstalledApps_Success() {
        InternalAdGroup adGroupBeforeChange = (InternalAdGroup) adGroupRepository.getAdGroups(shard,
                singletonList(existentAdGroupId)).get(0);

        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingMobileInstalledApps(new GdAdditionalTargetingMobileInstalledAppsRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(MOBILE_INSTALLED_APPS_URLS));
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withName(adGroupBeforeChange.getName())
                .withLevel(adGroupBeforeChange.getLevel())
                .withRf(adGroupBeforeChange.getRf())
                .withRfReset(adGroupBeforeChange.getRfReset())
                .withMaxClicksCount(adGroupBeforeChange.getMaxClicksCount())
                .withMaxClicksPeriod(adGroupBeforeChange.getMaxClicksPeriod())
                .withMaxStopsCount(adGroupBeforeChange.getMaxStopsCount())
                .withMaxStopsPeriod(adGroupBeforeChange.getMaxStopsPeriod())
                .withStartTime(adGroupBeforeChange.getStartTime())
                .withFinishTime(adGroupBeforeChange.getFinishTime())
                .withRegionIds(longToIntegerList(adGroupBeforeChange.getGeo()))
                .withTargetings(List.of(union));

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        AdGroup adGroupAfterChange = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        List<AdGroupAdditionalTargeting> targetingList =
                adGroupAdditionalTargetingRepository.getByAdGroupId(shard, existentAdGroupId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(adGroupAfterChange).is(
                    matchedBy(beanDiffer(adGroupBeforeChange).useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_COMPUTED_FIELDS)));
            softly.assertThat(targetingList.size()).isEqualTo(1);
            softly.assertThat(targetingList.get(0)).is(matchedBy(beanDiffer(new MobileInstalledAppsAdGroupAdditionalTargeting()
                    .withAdGroupId(existentAdGroupId)
                    .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                    .withJoinType(AdGroupAdditionalTargetingJoinType.ANY))
                    .useCompareStrategy(COMPARE_STRATEGY_ALL_BUT_ID_AND_VALUE)));
            softly.assertThat(((MobileInstalledAppsAdGroupAdditionalTargeting) targetingList.get(0)).getValue())
                    .is(matchedBy(containsInAnyOrder(mapSet(MOBILE_INSTALLED_APPS_URLS,
                            url -> beanDiffer(new MobileInstalledApp()
                                    .withStoreUrl(url)).useCompareStrategy(onlyExpectedFields())))));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateMobileInstalledApps_ReturnsValidationError() {
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingMobileInstalledApps(new GdAdditionalTargetingMobileInstalledAppsRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(INVALID_MOBILE_INSTALLED_APPS_URLS));
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withTargetings(List.of(union));

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_MOBILE_INSTALLED_APPS),
                        field(GdAdditionalTargetingShowDatesRequest.VALUE.name())),
                CommonDefects.invalidValue())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    @Test
    public void updateInternalAdGroup_UpdateTimeTargeting_ReturnsValidationError() {
        List<List<Integer>> coefs = createEmptyTimeBoard();
        coefs.get(0).set(0, 20); // для внутренней рекламы коэф. отличные от 0 или 100 невалидны
        GdAdditionalTargetingUnion union = new GdAdditionalTargetingUnion()
                .withTargetingTime(new GdAdditionalTargetingTimeRequest()
                        .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                        .withJoinType(GdAdditionalTargetingJoinType.ANY)
                        .withValue(
                                new GdTimeTarget()
                                        .withTimeBoard(coefs)
                                        .withEnabledHolidaysMode(false)
                                        .withUseWorkingWeekends(false)
                                        .withIdTimeZone(-1L)
                        ));
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withTargetings(List.of(union));

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        ExecutionResult executionResult = graphQlTestExecutor.doMutation(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateInternalAdGroupsItem.TARGETINGS.name()),
                        index(0),
                        field(GdAdditionalTargetingUnion.TARGETING_TIME),
                        field(GdAdditionalTargetingShowDatesRequest.VALUE.name()),
                        field(GdTimeTarget.TIME_BOARD.name())),
                new Defect<>(GridDefectIds.TimeTarget.INVALID_TIME_BOARD_FORMAT))
                .withWarnings(List.of());

        assertSoftly(softly -> {
            softly.assertThat(executionResult.getErrors()).hasSize(1);
            softly.assertThat(GraphQLUtils.getGdValidationResults(executionResult.getErrors()).get(0))
                    .isEqualToComparingFieldByFieldRecursively(expectedGdValidationResult);
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateStartTimeAndDeleteFinishTime_Success() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        LocalDateTime newStartTime = START_TIME.plusDays(1);

        updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withStartTime(newStartTime)
                .withFinishTime(null);

        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withStartTime(newStartTime)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateFinishTimeAndDeleteStartTime_Success() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        LocalDateTime newFinishTime = FINISH_TIME.plusDays(1);

        updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withStartTime(null)
                .withFinishTime(newFinishTime);

        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withFinishTime(newFinishTime)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_DeleteStartAndFinishTimes_Success() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withStartTime(null)
                .withFinishTime(null);

        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateStartAndFinishTimes_Success() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        LocalDateTime newStartTime = START_TIME.minusDays(100);
        LocalDateTime newFinishTime = START_TIME.plusYears(1);

        updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withStartTime(newStartTime)
                .withFinishTime(newFinishTime);

        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        Long returnedAdGroupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        AdGroup actualAdGroup = adGroupRepository.getAdGroups(shard, singletonList(existentAdGroupId)).get(0);

        InternalAdGroup expectedInternalAdGroup = new InternalAdGroup()
                .withType(AdGroupType.INTERNAL)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withStartTime(newStartTime)
                .withFinishTime(newFinishTime)
                .withGeo(integerToLongList(NEW_GROUP_REGION_IDS));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(returnedAdGroupId).isEqualTo(existentAdGroupId);
            softly.assertThat(actualAdGroup).is(
                    matchedBy(beanDiffer(expectedInternalAdGroup).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void updateInternalAdGroup_UpdateStartAndFinishTimes_ReturnsValidationError() {
        GdUpdateInternalAdGroupsItem updateItem = createCorrectUpdateItem(existentAdGroupId);

        GdUpdateInternalAdGroups input = createRequest(updateItem);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);
        validateResponseSuccessful(payload);

        updateItem = createCorrectUpdateItem(existentAdGroupId)
                .withStartTime(FINISH_TIME)
                .withFinishTime(START_TIME);

        input = createRequest(updateItem);

        payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, input, operator);

        GdValidationResult expectedGdValidationResult = toGdValidationResult(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS.name()), index(0),
                        field(GdUpdateInternalAdGroupsItem.FINISH_TIME.name())),
                AdGroupDefects.finishTimeShouldBeGreaterThanStartTime())
                .withWarnings(null);

        assertThat(payload.getValidationResult()).is(matchedBy(beanDiffer(expectedGdValidationResult)));
    }

    private GdUpdateInternalAdGroups createRequest(GdUpdateInternalAdGroupsItem... inputItems) {
        return new GdUpdateInternalAdGroups().withUpdateItems(asList(inputItems));
    }

    private GdUpdateInternalAdGroupsItem createCorrectUpdateItem(Long adGroupId) {
        return new GdUpdateInternalAdGroupsItem()
                .withId(adGroupId)
                .withName(NEW_GROUP_NAME)
                .withLevel(NEW_GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withMaxClicksCount(MAX_CLICKS_COUNT)
                .withMaxClicksPeriod(MAX_CLICKS_PERIOD)
                .withMaxStopsCount(MAX_STOPS_COUNT)
                .withMaxStopsPeriod(MAX_STOPS_PERIOD)
                .withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME)
                .withRegionIds(NEW_GROUP_REGION_IDS);
    }

    private static List<List<Integer>> createEmptyTimeBoard() {
        return new ArrayList<>(Collections.nCopies(DAYS_PER_WEEK, new ArrayList<>(Collections.nCopies(HOURS_PER_DAY,
                0))));
    }
}
