package ru.yandex.direct.core.entity.metrika.service.validation;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.AUDIENCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.CDP_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.ECOMMERCE;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.GOAL;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.INTERESTS;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.LAL_SEGMENT;
import static ru.yandex.direct.core.entity.retargeting.model.GoalType.SEGMENT;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.CollectionDefects.inCollection;
import static ru.yandex.direct.validation.defect.CollectionDefects.notInCollection;
import static ru.yandex.direct.validation.defect.CommonDefects.invalidValue;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@ParametersAreNonnullByDefault
@RunWith(MockitoJUnitRunner.class)
public class MetrikaGoalsValidationServiceLalTest {
    private static final ClientId CLIENT_ID = ClientId.fromLong(1L);
    private static final List<Goal> EXISTING_GOALS = List.of(
            goal(1L, GOAL, "Goal"),
            goal(1_000_000_001L, SEGMENT, "Segment"),
            goal(2_000_000_001L, AUDIENCE, "Audience"), // Goal with existing LAL-segment ↓↓↓
            (Goal) goal(1_500_000_001L, LAL_SEGMENT, "Lal").withParentId(2_000_000_001L),
            goal(2_000_000_002L, AUDIENCE, "Audience"),
            goal(2_600_000_001L, CDP_SEGMENT, "CdpSegment"),
            goal(3_000_000_001L, ECOMMERCE, "Ecommerce")
    );

    @InjectMocks
    private MetrikaGoalsValidationService service;

    @Test
    public void testSuccess() {
        var vr = service.validateGoalsForLalSegmentCreation(
                List.of(1L, 1_000_000_001L, 2_000_000_002L, 2_600_000_001L, 3_000_000_001L), EXISTING_GOALS);
        assertThat(vr).is(matchedBy(hasNoErrorsAndWarnings()));
    }

    @Test
    public void testUnknownParentId() {
        var vr = service.validateGoalsForLalSegmentCreation(List.of(100500L), EXISTING_GOALS);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), inCollection()))));
    }

    @Test
    public void testWrongParentType() {
        var wrongTypeGoal = goal(2_499_001_101L, INTERESTS, "Interests");
        var existingGoals = List.of(wrongTypeGoal);
        var vr = service.validateGoalsForLalSegmentCreation(List.of(2_499_001_101L), existingGoals);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), invalidValue()))));
    }

    @Test
    public void testTypeConstraintIsCheckedOnlyForExistingGoals() {
        var wrongTypeGoal = goal(2_499_001_101L, INTERESTS, "Interests");
        var existingGoals = List.of(wrongTypeGoal);
        var vr = service.validateGoalsForLalSegmentCreation(List.of(1L), existingGoals);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), inCollection()))));
    }

    @Test
    public void testLalSegmentAlreadyExists() {
        var vr = service.validateGoalsForLalSegmentCreation(List.of(2_000_000_001L), EXISTING_GOALS);

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(path(index(0)), notInCollection()))));
    }

    /**
     * Creates new {@link Goal} with specified id, type and name.
     * <p>
     * In fact, type is computed inside {@link Goal#setId(Long)} method and cannot be
     * changed by {@link Goal#setType(GoalType)} call. So type here is needed only
     * for documentation purposes.
     */
    private static Goal goal(Long id, GoalType type, String name) {
        return (Goal) new Goal()
                .withId(id)
                .withType(type)
                .withName(name);
    }
}
