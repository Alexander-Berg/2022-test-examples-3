package ru.yandex.direct.grid.processing.service.group.internalads;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;
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
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.TimeAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.YandexUidsAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.repository.AdGroupAdditionalTargetingRepository;
import ru.yandex.direct.core.entity.adgroupadditionaltargeting.uatraits.model.IsMobileAdGroupAdditionalTargeting;
import ru.yandex.direct.core.entity.campaign.repository.CampaignMappings;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsMobileRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsYandexPlusRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingRequest;
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
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ALL;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.FILTERING;
import static ru.yandex.direct.core.entity.adgroupadditionaltargeting.model.AdGroupAdditionalTargetingMode.TARGETING;
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
public class UpdateInternalAdGroupsMutationServiceTest {

    private static final String GROUP_NAME = "aaaa";
    private static final long GROUP_LEVEL = 1L;
    private static final int RF = 3;
    private static final int RF_RESET = 1;
    private static final LocalDateTime START_TIME = LocalDateTime.now().withNano(0);
    private static final LocalDateTime FINISH_TIME = START_TIME.plusDays(100);
    private static final List<Integer> GROUP_REGION_IDS = singletonList((int) Region.MOSCOW_REGION_ID);
    private static final List<String> YAUID_LIST = ImmutableList.of("1021110101545123184", "2021110101545123184",
            "%42");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Autowired
    private GraphQlTestExecutor graphQlTestExecutor;

    @Autowired
    private UpdateInternalAdGroupsMutationService updateInternalAdGroupsMutationService;

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
        groupId = steps.adGroupSteps().createActiveInternalAdGroup(campaignInfo).getAdGroupId();
    }

    @Test
    public void updateInternalAdGroup_NoTargetings() {
        var request = createRequest(createUpdateItem(groupId));

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request,
                operator);
        validateResponseSuccessful(payload);
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).isEmpty();
    }

    @Test
    public void updateInternalAdGroup_AddSeveralTargetings() {
        var item = createUpdateItem(groupId).withTargetings(getTargetings());
        var request = createRequest(item);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request,
                operator);
        validateResponseSuccessful(payload);

        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).is(matchedBy(containsInAnyOrder(
                createDefaultTargetingMatcher(groupId, IsMobileAdGroupAdditionalTargeting.class, null),
                createDefaultTargetingMatcher(groupId, YandexUidsAdGroupAdditionalTargeting.class,
                        hasProperty("value", equalTo(YAUID_LIST)))
        )));
    }

    @Test
    public void updateInternalAdGroup_UpdateTargetings() {
        adGroupAdditionalTargetingRepository.add(shard, clientInfo.getClientId(), List.of(
                new IsMobileAdGroupAdditionalTargeting()
                        .withAdGroupId(groupId)
                        .withTargetingMode(TARGETING)
                        .withJoinType(ANY)
        ));

        var changedTargeting = new GdAdditionalTargetingIsMobileRequest()
                .withTargetingMode(GdAdditionalTargetingMode.FILTERING)
                .withJoinType(GdAdditionalTargetingJoinType.ALL);
        var item = createUpdateItem(groupId).withTargetings(List.of(
                new GdAdditionalTargetingUnion().withTargetingIsMobile(changedTargeting)));
        var request = createRequest(item);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request,
                operator);
        validateResponseSuccessful(payload);

        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).is(matchedBy(contains(
                createTargetingMatcher(groupId, IsMobileAdGroupAdditionalTargeting.class, null, FILTERING, ALL)
        )));
    }

    @Test
    public void updateInternalAdGroup_RemoveTargetings() {
        adGroupAdditionalTargetingRepository.add(shard, clientInfo.getClientId(), List.of(
                new IsMobileAdGroupAdditionalTargeting()
                        .withAdGroupId(groupId)
                        .withTargetingMode(TARGETING)
                        .withJoinType(ANY)
        ));

        var item = createUpdateItem(groupId).withTargetings(null);
        var request = createRequest(item);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request,
                operator);
        validateResponseSuccessful(payload);
        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(actual).isEmpty();
    }

    @Test
    public void updateInternalAdGroup_InvalidTargetingsUnion() {
        var invalidUnion = getUnionWithIsMobileTargeting()
                .withTargetingIsYandexPlus(createDefaultTargeting(GdAdditionalTargetingIsYandexPlusRequest::new));
        var request = createRequest(createUpdateItem(groupId).withTargetings(List.of(invalidUnion)));


        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), invalidUnion()))));

        updateInternalAdGroupsMutationService.updateInternalAdGroups(getOwner(clientInfo), operator.getUid(), request);
    }

    private UidAndClientId getOwner(ClientInfo clientInfo) {
        return UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId());
    }

    @Test
    public void updateInternalAdGroup_DuplicateTargetingsUnion() {
        var unionsWithDuplicates = List.of(getUnionWithIsMobileTargeting(), getUnionWithIsMobileTargeting());
        var request = createRequest(createUpdateItem(groupId).withTargetings(unionsWithDuplicates));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(0), duplicatedElement()))));
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(targetingPath(1), duplicatedElement()))));

        updateInternalAdGroupsMutationService.updateInternalAdGroups(getOwner(clientInfo), operator.getUid(), request);
    }

    @Test
    public void updateInternalAdGroup_CheckDeletionOfTargetingWhichWeDontUpdate() {
        adGroupAdditionalTargetingRepository.add(shard, clientInfo.getClientId(), List.of(
                new IsMobileAdGroupAdditionalTargeting()
                        .withAdGroupId(groupId)
                        .withTargetingMode(TARGETING)
                        .withJoinType(ANY)
        ));

        var timeTargeting = new TimeAdGroupAdditionalTargeting()
                .withAdGroupId(groupId)
                .withTargetingMode(TARGETING)
                .withJoinType(ANY)
                .withValue(List.of(CampaignMappings.timeTargetFromDb("2BbCbDb3BcCcDc4BdCdDd")));

        adGroupAdditionalTargetingRepository.add(shard, clientInfo.getClientId(), List.of(timeTargeting));

        var changedTargeting = new GdAdditionalTargetingIsMobileRequest()
                .withTargetingMode(GdAdditionalTargetingMode.FILTERING)
                .withJoinType(GdAdditionalTargetingJoinType.ALL);
        var item = createUpdateItem(groupId).withTargetings(List.of(
                new GdAdditionalTargetingUnion().withTargetingIsMobile(changedTargeting)));
        var request = createRequest(item);

        GdUpdateAdGroupPayload payload = graphQlTestExecutor.doMutationAndGetPayload(UPDATE_MUTATION, request,
                operator);
        validateResponseSuccessful(payload);

        List<AdGroupAdditionalTargeting> actual = adGroupAdditionalTargetingRepository.getByAdGroupId(shard, groupId);

        assertThat(timeTargeting).isNotIn(actual);
    }

    private static List<GdAdditionalTargetingUnion> getTargetings() {
        return List.of(
                getUnionWithIsMobileTargeting(),
                new GdAdditionalTargetingUnion()
                        .withTargetingYandexUids(createDefaultTargeting(GdAdditionalTargetingYandexUidsRequest::new)
                                .withValue(YAUID_LIST))
        );
    }

    private static GdAdditionalTargetingUnion getUnionWithIsMobileTargeting() {
        return new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(createDefaultTargeting(GdAdditionalTargetingIsMobileRequest::new));
    }

    private static <T extends GdAdditionalTargetingRequest> T createDefaultTargeting(Supplier<T> targetingCreator) {
        T targeting = targetingCreator.get();
        targeting
                .withTargetingMode(GdAdditionalTargetingMode.TARGETING)
                .withJoinType(GdAdditionalTargetingJoinType.ANY);
        return targeting;
    }

    private static GdUpdateInternalAdGroups createRequest(GdUpdateInternalAdGroupsItem... inputItems) {
        return new GdUpdateInternalAdGroups().withUpdateItems(asList(inputItems));
    }

    private static GdUpdateInternalAdGroupsItem createUpdateItem(Long groupId) {
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

    private static Matcher<Object> createDefaultTargetingMatcher(
            Long adGroupId, Class<? extends AdGroupAdditionalTargeting> targeting,
            @Nullable Matcher<Object> valueMatcher) {
        return createTargetingMatcher(adGroupId, targeting,
                valueMatcher, TARGETING, ANY);
    }

    private static Matcher<Object> createTargetingMatcher(
            Long adGroupId, Class<? extends AdGroupAdditionalTargeting> targeting,
            @Nullable Matcher<Object> valueMatcher,
            AdGroupAdditionalTargetingMode mode,
            AdGroupAdditionalTargetingJoinType joinType) {
        Matcher<Object> commonMatcher = allOf(
                instanceOf(targeting),
                hasProperty("adGroupId", equalTo(adGroupId)),
                hasProperty("targetingMode", equalTo(mode)),
                hasProperty("joinType", equalTo(joinType))
        );

        if (valueMatcher == null) {
            return commonMatcher;
        }

        return allOf(commonMatcher, valueMatcher);
    }

    private static Path targetingPath(int index) {
        return path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS), index(0),
                field(GdUpdateInternalAdGroupsItem.TARGETINGS), index(index));
    }

}
