package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.adgroup.repository.AdGroupRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.IsYandexPlusAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsYandexPlusRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYandexUidsRequest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.service.group.mutation.UpdateInternalAdGroupsMutationService;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.FILTERING;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.TARGETING;
import static ru.yandex.direct.grid.processing.util.GraphQlTestExecutor.validateResponseSuccessful;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class AdGroupGraphQLServiceUpdateInternalTargetingsTest {

    private static final String GROUP_NAME = "aaaa";
    private static final long GROUP_LEVEL = 1L;
    private static final int RF = 3;
    private static final int RF_RESET = 1;
    private static final LocalDateTime START_TIME = LocalDateTime.now().withNano(0);
    private static final LocalDateTime FINISH_TIME = START_TIME.plusDays(100);
    private static final List<Integer> GROUP_REGION_IDS = singletonList((int) Region.MOSCOW_REGION_ID);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

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

    @Autowired
    UpdateInternalAdGroupsMutationService updateInternalAdGroupsMutationService;

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

    private static final GraphQlTestExecutor.TemplateMutation<GdUpdateInternalAdGroups, GdUpdateAdGroupPayload> UPDATE_MUTATION =
            new GraphQlTestExecutor.TemplateMutation<>("updateInternalAdGroups", UPDATE_MUTATION_TEMPLATE,
                    GdUpdateInternalAdGroups.class, GdUpdateAdGroupPayload.class);

    private ClientInfo clientInfo;
    private Integer shard;
    private User operator;
    private Long groupId;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        AdGroupInfo adGroup = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo, GROUP_LEVEL, RF, RF_RESET);
        groupId = adGroup.getAdGroupId();
    }

    @Test
    public void updateInternalAdGroup_addSingleTargeting() {
        var union = new GdAdditionalTargetingUnion()
                .withTargetingIsYandexPlus(
                        new GdAdditionalTargetingIsYandexPlusRequest()
                            .withTargetingMode(TARGETING)
                            .withJoinType(ANY)
                );
        var request = createRequest(createUpdateItem().withTargetings(List.of(union)));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).is(matchedBy(contains(
                createDefaultTargetingMatcher(groupId, IsYandexPlusAdGroupAdditionalTargeting.class, null)
        )));
    }

    @Test
    public void updateInternalAdGroup_updateSingleTargeting() {
        addYandexUidsTargeting();

        var newValue = List.of("2021110101545123184");
        var union = getYandexUidsUnion(newValue);
        var request = createRequest(createUpdateItem().withTargetings(List.of(union)));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).is(matchedBy(contains(
                createDefaultTargetingMatcher(groupId, YandexUidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(newValue)))
        )));
    }

    @Test
    public void updateInternalAdGroup_updateSeveralTargetings() {
        addYandexUidsTargeting();
        addDeviceNamesTargeting(List.of("Device 1"));

        var newUidsValue = List.of("2021110101545123184");
        var union1 = getYandexUidsUnion(newUidsValue);
        var newDeviceNamesValue = List.of("Another device");
        var union2 = getDeviceNamesUnion(newDeviceNamesValue, TARGETING);
        var request = createRequest(createUpdateItem().withTargetings(List.of(union1, union2)));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        //noinspection unchecked
        assertThat(actual).is(matchedBy(containsInAnyOrder(
                createDefaultTargetingMatcher(groupId, YandexUidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(newUidsValue))),
                createDefaultTargetingMatcher(groupId, DeviceNamesAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(newDeviceNamesValue)))
        )));
    }

    @Test
    public void updateInternalAdGroup_duplicateTargetings() {
        var value = List.of("2021110101545123184");
        var union1 = getYandexUidsUnion(value);
        var union2 = getYandexUidsUnion(value);
        var request = createRequest(createUpdateItem().withTargetings(List.of(union1, union2)));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        updateInternalAdGroupsMutationService.updateInternalAdGroups(getOwner(clientInfo), operator.getUid(), request);
    }

    @Test
    public void updateInternalAdGroup_duplicateTargetingsWithDifferentValues() {
        var union1 = getYandexUidsUnion(List.of("2021110101545123184"));
        var union2 = getYandexUidsUnion(List.of("1021110101545123184"));
        var request = createRequest(createUpdateItem().withTargetings(List.of(union1, union2)));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        updateInternalAdGroupsMutationService.updateInternalAdGroups(getOwner(clientInfo), operator.getUid(), request);
    }

    @Test
    public void updateInternalAdGroup_positiveAndNegativeTargetings() {
        var union1 = getDeviceNamesUnion(List.of("Device 1"), TARGETING);
        var union2 = getDeviceNamesUnion(List.of("Device 2"), FILTERING);
        var request = createRequest(createUpdateItem().withTargetings(List.of(union1, union2)));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getUpdatedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).isInstanceOf(DeviceNamesAdGroupAdditionalTargeting.class);
        assertThat(actual.get(1)).isInstanceOf(DeviceNamesAdGroupAdditionalTargeting.class);
        assertThat(actual.get(0).getTargetingMode() != actual.get(1).getTargetingMode()).isTrue();
    }

    @Test
    public void updateInternalAdGroup_positiveAndNegativeNotAllowedTargetings() {
        // Для булевых таргетингов запрещено одновременно передавать позитивный и негативный таргетинг
        var union1 = getIsYandexPlusUnion(TARGETING);
        var union2 = getIsYandexPlusUnion(FILTERING);
        var request = createRequest(createUpdateItem().withTargetings(List.of(union1, union2)));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        updateInternalAdGroupsMutationService.updateInternalAdGroups(getOwner(clientInfo), operator.getUid(), request);
    }

    private static GdAdditionalTargetingUnion getYandexUidsUnion(List<String> value) {
        return new GdAdditionalTargetingUnion()
                .withTargetingYandexUids(
                        new GdAdditionalTargetingYandexUidsRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(value)
                );
    }

    private static GdAdditionalTargetingUnion getDeviceNamesUnion(List<String> value, GdAdditionalTargetingMode mode) {
        return new GdAdditionalTargetingUnion()
                .withTargetingDeviceNames(
                        new GdAdditionalTargetingDeviceNamesRequest()
                                .withTargetingMode(mode)
                                .withJoinType(ANY)
                                .withValue(value)
                );
    }

    private static GdAdditionalTargetingUnion getIsYandexPlusUnion(GdAdditionalTargetingMode mode) {
        return new GdAdditionalTargetingUnion()
                .withTargetingIsYandexPlus(
                        new GdAdditionalTargetingIsYandexPlusRequest()
                                .withTargetingMode(mode)
                                .withJoinType(ANY)
                );
    }

    private void addYandexUidsTargeting() {
        var targeting = new YandexUidsAdGroupAdditionalTargeting()
                .withAdGroupId(groupId)
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(List.of("1021110101545123184"));
        adGroupAdditionalTargetingRepository.add(clientInfo.getShard(), clientInfo.getClientId(), List.of(targeting));
    }

    private void addDeviceNamesTargeting(List<String> value) {
        var targeting = new DeviceNamesAdGroupAdditionalTargeting()
                .withAdGroupId(groupId)
                .withTargetingMode(AdGroupAdditionalTargetingMode.TARGETING)
                .withJoinType(AdGroupAdditionalTargetingJoinType.ANY)
                .withValue(value);
        adGroupAdditionalTargetingRepository.add(clientInfo.getShard(), clientInfo.getClientId(), List.of(targeting));
    }

    private static Matcher<Object> createDefaultTargetingMatcher(
            Long adGroupId, Class<? extends AdGroupAdditionalTargeting> targeting,
            @Nullable Matcher<Object> valueMatcher) {
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

    private static GdUpdateInternalAdGroups createRequest(GdUpdateInternalAdGroupsItem... inputItems) {
        return new GdUpdateInternalAdGroups().withUpdateItems(asList(inputItems));
    }

    private GdUpdateInternalAdGroupsItem createUpdateItem() {
        return new GdUpdateInternalAdGroupsItem()
                .withId(groupId)
                .withName(GROUP_NAME)
                .withLevel(GROUP_LEVEL)
                .withRf(RF)
                .withRfReset(RF_RESET)
                .withStartTime(START_TIME)
                .withFinishTime(FINISH_TIME)
                .withRegionIds(GROUP_REGION_IDS);
    }

    private static Path targetingPath(int index) {
        return path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS), index(0),
                field(GdUpdateInternalAdGroupsItem.TARGETINGS), index(index));
    }

    private UidAndClientId getOwner(ClientInfo clientInfo) {
        return UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId());
    }
}
