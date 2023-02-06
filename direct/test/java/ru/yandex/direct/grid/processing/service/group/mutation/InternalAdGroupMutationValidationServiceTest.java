package ru.yandex.direct.grid.processing.service.group.mutation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.grid.model.campaign.timetarget.GdTimeTarget;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.exception.GridValidationException;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDesktopInstalledAppsRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingDeviceNamesRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingInternalNetworkRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingIsMobileRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingTimeRequest;
import ru.yandex.direct.grid.processing.model.group.additionaltargeting.mutation.GdAdditionalTargetingUnion;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdAddInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroups;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem;
import ru.yandex.direct.grid.processing.model.retargeting.GdGoalMinimal;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleItemReq;
import ru.yandex.direct.grid.processing.model.retargeting.GdRetargetingConditionRuleType;
import ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateInternalAdRetargetingConditionItem;
import ru.yandex.direct.grid.processing.service.validation.GridDefectIds;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.Path;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.joda.time.DateTimeConstants.DAYS_PER_WEEK;
import static org.joda.time.DateTimeConstants.HOURS_PER_DAY;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType.ALL;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingJoinType.ANY;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.FILTERING;
import static ru.yandex.direct.grid.processing.model.group.additionaltargeting.GdAdditionalTargetingMode.TARGETING;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem.RETARGETING_CONDITIONS;
import static ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateInternalAdGroupsItem.TARGETINGS;
import static ru.yandex.direct.grid.processing.model.retargeting.mutation.GdUpdateInternalAdRetargetingConditionItem.CONDITION_RULES;
import static ru.yandex.direct.grid.processing.service.validation.GridDefectDefinitions.invalidUnion;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.gridDefect;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasErrorsWith;
import static ru.yandex.direct.grid.processing.util.validation.GridValidationMatchers.hasValidationResult;
import static ru.yandex.direct.validation.defect.CollectionDefects.duplicatedElement;
import static ru.yandex.direct.validation.defect.CollectionDefects.notEmptyCollection;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
@ParametersAreNonnullByDefault
public class InternalAdGroupMutationValidationServiceTest {
    private static final Long DEFAULT_ID = 1L;
    private static final List<Integer> NEW_GROUP_REGION_IDS = List.of(
            Long.valueOf(Region.SAINT_PETERSBURG_REGION_ID).intValue());
    private static final String NEW_GROUP_NAME = "this is a group name!";

    @Autowired
    private InternalAdGroupMutationValidationService internalAdGroupMutationValidationService;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testValidateUpdateGroup_successWithoutTargetings() {
        var item = createUpdateItem();
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_successWithValidTargeting() {
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingInternalNetwork(
                        new GdAdditionalTargetingInternalNetworkRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_successWithValidRetargetingConditions() {
        var conditionItem = new GdUpdateInternalAdRetargetingConditionItem()
                .withConditionRules(List.of(new GdRetargetingConditionRuleItemReq()
                        .withGoals(singletonList(new GdGoalMinimal().withId(123L)))
                        .withType(GdRetargetingConditionRuleType.OR)));
        var item = createUpdateItem()
                .withRetargetingConditions(List.of(conditionItem));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_invalidRetargetingConditions() {
        var conditionItem = new GdUpdateInternalAdRetargetingConditionItem()
                .withConditionRules(emptyList());
        var item = createUpdateItem()
                .withRetargetingConditions(List.of(conditionItem));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(
                path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS), index(0), field(RETARGETING_CONDITIONS), index(0),
                        field(CONDITION_RULES)),
                notEmptyCollection()))));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_invalidTargetingUnion() {
        // один объект union не может содержать более одного таргетинга
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingInternalNetwork(
                        new GdAdditionalTargetingInternalNetworkRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                )
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(updateTargetingsPath(), invalidUnion()))));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_successSameTargetingsWithDifferentJoinType() {
        var targetingUnion1 = new GdAdditionalTargetingUnion()
                .withTargetingDesktopInstalledApps(
                        new GdAdditionalTargetingDesktopInstalledAppsRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ALL)
                                .withValue(Set.of(1L, 2L))
                );
        var targetingUnion2 = new GdAdditionalTargetingUnion()
                .withTargetingDesktopInstalledApps(
                        new GdAdditionalTargetingDesktopInstalledAppsRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(Set.of(3L, 4L))
                );
        var targetingUnion3 = new GdAdditionalTargetingUnion()
                .withTargetingDesktopInstalledApps(
                        new GdAdditionalTargetingDesktopInstalledAppsRequest()
                                .withTargetingMode(FILTERING)
                                .withJoinType(ALL)
                                .withValue(Set.of(5L))
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion1, targetingUnion2, targetingUnion3));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_duplicateTargetings() {
        var targetingUnion1 = new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var targetingUnion2 = new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion1, targetingUnion2));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(updateTargetingsPath(), duplicatedElement()))));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_duplicateTargetingsWithDifferentValues() {
        var targetingUnion1 = new GdAdditionalTargetingUnion()
                .withTargetingDeviceNames(
                        new GdAdditionalTargetingDeviceNamesRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(List.of("value 1"))
                );
        var targetingUnion2 = new GdAdditionalTargetingUnion()
                .withTargetingDeviceNames(
                        new GdAdditionalTargetingDeviceNamesRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(List.of("value 2"))
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion1, targetingUnion2));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(updateTargetingsPath(), duplicatedElement()))));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }


    @Test
    public void testValidateAddGroup_successWithValidRetargetingConditions() {
        var conditionItem = new GdUpdateInternalAdRetargetingConditionItem()
                .withConditionRules(List.of(new GdRetargetingConditionRuleItemReq()
                        .withGoals(singletonList(new GdGoalMinimal().withId(123L)))
                        .withType(GdRetargetingConditionRuleType.OR)));
        var item = createAddItem()
                .withRetargetingConditions(List.of(conditionItem));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_invalidRetargetingConditions() {
        var conditionItem = new GdUpdateInternalAdRetargetingConditionItem()
                .withConditionRules(emptyList());
        var item = createAddItem()
                .withRetargetingConditions(List.of(conditionItem));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(
                path(field(GdAddInternalAdGroups.ADD_ITEMS), index(0), field(RETARGETING_CONDITIONS), index(0),
                        field(CONDITION_RULES)),
                notEmptyCollection()))));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_successWithoutTargetings() {
        var item = createAddItem();
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_successWithValidTargeting() {
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingInternalNetwork(
                        new GdAdditionalTargetingInternalNetworkRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createAddItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_invalidTargetingUnion() {
        // один объект union не может содержать более одного таргетинга
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingInternalNetwork(
                        new GdAdditionalTargetingInternalNetworkRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                )
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createAddItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(addTargetingsPath(), invalidUnion()))));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_duplicateTargetings() {
        var targetingUnion1 = new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var targetingUnion2 = new GdAdditionalTargetingUnion()
                .withTargetingIsMobile(
                        new GdAdditionalTargetingIsMobileRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                );
        var item = createAddItem()
                .withTargetings(List.of(targetingUnion1, targetingUnion2));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(addTargetingsPath(), duplicatedElement()))));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateAddGroup_duplicateTargetingsWithDifferentValues() {
        var targetingUnion1 = new GdAdditionalTargetingUnion()
                .withTargetingDeviceNames(
                        new GdAdditionalTargetingDeviceNamesRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(List.of("value 1"))
                );
        var targetingUnion2 = new GdAdditionalTargetingUnion()
                .withTargetingDeviceNames(
                        new GdAdditionalTargetingDeviceNamesRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(List.of("value 2"))
                );
        var item = createAddItem()
                .withTargetings(List.of(targetingUnion1, targetingUnion2));
        var input = new GdAddInternalAdGroups()
                .withAddItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(gridDefect(addTargetingsPath(), duplicatedElement()))));

        internalAdGroupMutationValidationService.validateAddGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_validTimeTarget() {
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingTime(
                        new GdAdditionalTargetingTimeRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(new GdTimeTarget()
                                        .withTimeBoard(createTimeBoardOneHourEachDay())
                                        .withEnabledHolidaysMode(false)
                                        .withUseWorkingWeekends(false))
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));
        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    @Test
    public void testValidateUpdateGroup_invalidTimeTarget() {
        var targetingUnion = new GdAdditionalTargetingUnion()
                .withTargetingTime(
                        new GdAdditionalTargetingTimeRequest()
                                .withTargetingMode(TARGETING)
                                .withJoinType(ANY)
                                .withValue(
                                        new GdTimeTarget()
                                                .withTimeBoard(List.of(List.of(0)))
                                                .withEnabledHolidaysMode(false)
                                                .withUseWorkingWeekends(false))
                );
        var item = createUpdateItem()
                .withTargetings(List.of(targetingUnion));
        var input = new GdUpdateInternalAdGroups()
                .withUpdateItems(List.of(item));

        thrown.expect(GridValidationException.class);
        thrown.expect(hasValidationResult(hasErrorsWith(
                gridDefect(path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS),
                                index(0), field(TARGETINGS), index(0), field("targetingTime"),
                                field("value"), field("timeBoard")),
                        new Defect<>(GridDefectIds.TimeTarget.INVALID_TIME_BOARD_FORMAT)))));

        internalAdGroupMutationValidationService.validateUpdateGroup(input);
    }

    private static GdUpdateInternalAdGroupsItem createUpdateItem() {
        return new GdUpdateInternalAdGroupsItem()
                .withId(DEFAULT_ID)
                .withName(NEW_GROUP_NAME)
                .withRegionIds(NEW_GROUP_REGION_IDS)
                .withTargetings(emptyList())
                .withRetargetingConditions(emptyList());
    }

    private static GdAddInternalAdGroupsItem createAddItem() {
        return new GdAddInternalAdGroupsItem()
                .withCampaignId(DEFAULT_ID)
                .withName(NEW_GROUP_NAME)
                .withRegionIds(NEW_GROUP_REGION_IDS)
                .withTargetings(emptyList())
                .withRetargetingConditions(emptyList());
    }

    private static Path updateTargetingsPath() {
        return path(field(GdUpdateInternalAdGroups.UPDATE_ITEMS), index(0), field(TARGETINGS), index(0));
    }

    private static Path addTargetingsPath() {
        return path(field(GdAddInternalAdGroups.ADD_ITEMS), index(0), field(TARGETINGS), index(0));
    }

    private static List<List<Integer>> createTimeBoardOneHourEachDay() {
        List<List<Integer>> result = new ArrayList<>(DAYS_PER_WEEK);
        for (int i = 0; i < DAYS_PER_WEEK; i++) {
            List<Integer> day = new ArrayList<>(Collections.nCopies(HOURS_PER_DAY, 0));
            day.set(0, 100);
            result.add(i, day);
        }
        return result;
    }
}
