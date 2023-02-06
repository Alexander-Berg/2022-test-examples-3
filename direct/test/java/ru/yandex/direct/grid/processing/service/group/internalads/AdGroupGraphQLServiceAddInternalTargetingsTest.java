package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

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
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.DeviceNamesAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsMobileAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsMobileRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsYandexPlusRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingYandexUidsRequest;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddAdGroupPayload;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.service.group.mutation.AddInternalAdGroupsMutationService;
import ru.yandex.direct.grid.processing.util.GraphQlTestExecutor;
import ru.yandex.direct.grid.processing.util.TestAuthHelper;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static ru.yandex.direct.core.testing.data.TestAdGroupAdditionalTargetings.YANDEX_UIDS;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.FILTERING;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.TARGETING;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.invalidUnion;
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
public class AdGroupGraphQLServiceAddInternalTargetingsTest {

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
    private AddInternalAdGroupsMutationService addInternalAdGroupsMutationService;

    @Autowired
    Steps steps;

    @Autowired
    AdGroupRepository adGroupRepository;

    @Autowired
    AdGroupAdditionalTargetingRepository adGroupAdditionalTargetingRepository;

    @Autowired
    UserRepository userRepository;

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

    private ClientInfo clientInfo;
    private Integer shard;
    private User operator;
    private Long campaignId;
    private Long clientUid;

    @Before
    public void before() {
        clientInfo = steps.internalAdProductSteps().createDefaultInternalAdProduct();
        clientUid = clientInfo.getUid();

        shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        TestAuthHelper.setDirectAuthentication(operator);

        CampaignInfo campaignInfo = steps.campaignSteps().createActiveInternalDistribCampaign(clientInfo);
        campaignId = campaignInfo.getCampaignId();
    }

    @Test
    public void addInternalAdGroup_NoTargetings() {
        var request = createRequest(createAddItem());

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).isEmpty();
    }

    @Test
    public void addInternalAdGroup_SeveralTargetings() {
        var item = createAddItem().withTargetings(getTargetings());
        var request = createRequest(item);

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        //noinspection unchecked
        assertThat(actual).is(matchedBy(containsInAnyOrder(
                createDefaultTargetingMatcher(groupId, IsMobileAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(groupId, YandexUidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(YANDEX_UIDS)))
        )));
    }

    @Test
    public void addInternalAdGroup_InvalidTargetingsUnion() {
        var invalidUnion = getUnionWithIsMobileTargeting()
                .withTargetingIsYandexPlus(createDefaultTargeting(GdAdditionalTargetingIsYandexPlusRequest::new));
        var request = createRequest(createAddItem().withTargetings(List.of(invalidUnion)));


        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), invalidUnion()))));

        addInternalAdGroupsMutationService.addInternalAdGroups(operator.getUid(), UidAndClientId.of(clientUid,
                clientInfo.getClientId()), request
        );
    }

    @Test
    public void addInternalAdGroup_DuplicateTargetingsUnion() {
        var unionsWithDuplicates = List.of(getUnionWithIsMobileTargeting(), getUnionWithIsMobileTargeting());
        var request = createRequest(createAddItem().withTargetings(unionsWithDuplicates));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        addInternalAdGroupsMutationService.addInternalAdGroups(operator.getUid(), UidAndClientId.of(clientUid,
                clientInfo.getClientId()), request
        );
    }

    @Test
    public void addInternalAdGroup_DuplicateTargetingsWithDifferentValuesUnion() {
        var union1 = getDeviceNamesUnion(List.of("Value 1"), TARGETING);
        var union2 = getDeviceNamesUnion(List.of("Value 2"), TARGETING);
        var request = createRequest(createAddItem().withTargetings(List.of(union1, union2)));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        addInternalAdGroupsMutationService.addInternalAdGroups(operator.getUid(), UidAndClientId.of(clientUid,
                clientInfo.getClientId()), request
        );
    }

    @Test
    public void addInternalAdGroup_positiveAndNegativeTargetings() {
        var value1 = List.of("Value 1");
        var union1 = getDeviceNamesUnion(value1, TARGETING);
        var value2 = List.of("Value 2");
        var union2 = getDeviceNamesUnion(value2, FILTERING);
        var request = createRequest(createAddItem().withTargetings(List.of(union1, union2)));

        GdAddAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(ADD_MUTATION, request, operator);
        validateResponseSuccessful(payload);

        Long groupId = payload.getAddedAdGroupItems().get(0).getAdGroupId();
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).isInstanceOf(DeviceNamesAdGroupAdditionalTargeting.class);
        assertThat(actual.get(1)).isInstanceOf(DeviceNamesAdGroupAdditionalTargeting.class);
        assertThat(actual.get(0).getTargetingMode() != actual.get(1).getTargetingMode()).isTrue();
    }

    @Test
    public void addInternalAdGroup_positiveAndNegativeNotAllowedTargetings() {
        // Для булевых таргетингов запрещено одновременно передавать позитивный и негативный таргетинг
        var union1 = getIsYandexPlusUnion(TARGETING);
        var union2 = getIsYandexPlusUnion(FILTERING);
        var request = createRequest(createAddItem().withTargetings(List.of(union1, union2)));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        addInternalAdGroupsMutationService.addInternalAdGroups(operator.getUid(), UidAndClientId.of(clientUid,
                clientInfo.getClientId()), request
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

    private static List<GdAdditionalTargetingUnion> getTargetings() {
        return List.of(
                getUnionWithIsMobileTargeting(),
                new GdAdditionalTargetingUnion()
                        .withTargetingYandexUids(createDefaultTargeting(GdAdditionalTargetingYandexUidsRequest::new)
                                .withValue(YANDEX_UIDS))
        );
    }

    private static GdAdditionalTargetingUnion getUnionWithIsMobileTargeting() {
        return new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(createDefaultTargeting(GdAdditionalTargetingIsMobileRequest::new));
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

    private GdAddInternalAdGroupsItem createAddItem() {
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

    private static Path targetingPath(int index) {
        return path(field(GdAddInternalAdGroups.ADD_ITEMS), index(0),
                field(GdAddInternalAdGroupsItem.TARGETINGS), index(index));
    }
}
