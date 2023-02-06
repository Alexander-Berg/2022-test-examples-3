package ru.yandex.direct.web.entity.crypta.service;

import java.util.List;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.crypta.AudienceType;
import ru.yandex.direct.core.entity.retargeting.model.CryptaGoalScope;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.feature.FeatureName;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.core.entity.inventori.service.CryptaService;
import ru.yandex.direct.web.core.model.retargeting.CryptaGoalWeb;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultCryptaGoalsWithTankerName;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByType;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalByTypeWithTankerName;
import static ru.yandex.direct.core.testing.data.TestFullGoals.defaultGoalWithId;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CryptaServiceTest {
    @Autowired
    private CryptaService cryptaService;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    @Autowired
    private Steps steps;

    private Goal goal;

    private static final Long ADULT_CONTENT_GOAL_ID = 4_294_968_377L;

    @Before
    public void before() {
        testCryptaSegmentRepository.clean();
        testCryptaSegmentRepository.addAll(defaultCryptaGoalsWithTankerName());
        goal = new Goal();
        goal.withId(1L);
    }

    @Test
    public void getSegments() {
        List<CryptaGoalWeb> response = cryptaService.getSegments();
        Assertions.assertThat(response).isNotEmpty();
    }

    @Test
    public void canSelectAll_parentIdNotZero_returnsNull() {
        goal.withParentId(1L);
        assertThat(cryptaService.canSelectAll(goal), nullValue());
    }

    @Test
    public void canSelectAll_parentIdNull_notRegisteredType_returnsTrue() {
        goal.withParentId(null);
        assertThat(cryptaService.canSelectAll(goal), is(true));
    }

    @Test
    public void canSelectAll_parentIdZero_returnsNotNull() {
        AudienceType audienceType = AudienceType.GENDER;
        goal.withParentId(0L);
        goal.withId(audienceType.getTypedValue());
        assertThat(cryptaService.canSelectAll(goal), is(audienceType.isAllValuesAllowed()));
    }

    @Test
    public void canSelectAll_parentIdNull_returnsNotNull() {
        AudienceType audienceType = AudienceType.TRANSPORTATION;
        goal.withParentId(null);
        goal.withId(audienceType.getTypedValue());
        assertThat(cryptaService.canSelectAll(goal), is(audienceType.isAllValuesAllowed()));
    }

    @Test
    public void translateWithInternalScope() {
        testCryptaSegmentRepository.clean();
        String expectedName = "expected_name";

        Goal interest =
                (Goal) defaultGoalByTypeWithTankerName(GoalType.INTERESTS).withParentId(0L).withCryptaScope(Set.of(CryptaGoalScope.INTERNAL_AD)).withName(expectedName);

        testCryptaSegmentRepository.addAll(List.of(interest), Set.of(CryptaGoalScope.INTERNAL_AD));
        CryptaGoalWeb modifiedGoal = cryptaService.getSegments(CryptaGoalScope.INTERNAL_AD).get(0);

        assertThat(modifiedGoal.getName(), is(expectedName));
    }

    @Test
    public void translateWithCommonScope() {
        testCryptaSegmentRepository.clean();
        String notExpectedName = "not_expected_name";

        Goal interest = (Goal) defaultGoalByType(GoalType.INTERESTS).withParentId(0L).withName("not_expected_name");

        testCryptaSegmentRepository.addAll(List.of(interest));
        CryptaGoalWeb modifiedGoal = cryptaService.getSegments().get(0);

        assertThat(modifiedGoal.getName(), Matchers.not(is(notExpectedName)));
    }

    @Test
    public void getSegments_singleRestFiltered() {
        testCryptaSegmentRepository.clean();

        Goal interest1 = (Goal) defaultGoalByTypeWithTankerName(GoalType.INTERESTS).withParentId(0L);
        Goal interest2 = (Goal) defaultGoalByTypeWithTankerName(GoalType.INTERESTS).withParentId(interest1.getId());
        Goal interest3 = defaultGoalByTypeWithTankerName(GoalType.BEHAVIORS);

        Goal contentCategory11 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(0L);
        Goal contentCategory12 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(contentCategory11.getId());
        Goal contentCategory13 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(contentCategory11.getId());

        Goal contentCategory21 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(0L);
        Goal contentCategory22 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(contentCategory21.getId());

        List<Goal> goals = List.of(interest1, interest2, interest3,
                contentCategory11, contentCategory12, contentCategory13,
                contentCategory21, contentCategory22
        );
        testCryptaSegmentRepository.addAll(goals);

        List<CryptaGoalWeb> expectedGoals = mapList(
                List.of(interest1, interest2, interest3,
                        contentCategory11, contentCategory12, contentCategory13,
                        contentCategory21),
                goal -> new CryptaGoalWeb().withId(goal.getId()).withParentId(goal.getParentId())
        );
        expectedGoals.add(createCinemaGenres());

        List<CryptaGoalWeb> actualGoals = cryptaService.getSegments();

        assertThat(actualGoals, containsInAnyOrder(mapList(expectedGoals,
                expectedGoal -> beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields()))));
    }

    @Test
    public void getSegments_cinemaGenresAdded() {
        testCryptaSegmentRepository.clean();

        Goal contentGenre1 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_GENRE)
                .withParentId(0L);
        Goal contentGenre2 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_GENRE)
                .withParentId(0L);
        Goal contentGenre3 = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_GENRE)
                .withParentId(0L);

        List<Goal> goals = List.of(contentGenre1, contentGenre2, contentGenre3);
        testCryptaSegmentRepository.addAll(goals);

        List<CryptaGoalWeb> expectedGoals = mapList(goals, goal ->
                new CryptaGoalWeb().withId(goal.getId()).withParentId(Goal.CINEMA_GENRES_GOAL_ID));
        expectedGoals.add(createCinemaGenres());

        List<CryptaGoalWeb> actualGoals = cryptaService.getSegments();

        assertThat(actualGoals, containsInAnyOrder(mapList(expectedGoals,
                expectedGoal -> beanDiffer(expectedGoal).useCompareStrategy(onlyExpectedFields()))));
    }

    @Test
    public void getSegmentsAdultContentWithoutFutureNotAdded() {
        var clientInfo = steps.clientSteps().createDefaultClient();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_ADULT_CONTENT, false);

        testCryptaSegmentRepository.clean();

        Goal adultContentGoal = (Goal) defaultGoalWithId(ADULT_CONTENT_GOAL_ID, GoalType.CONTENT_CATEGORY)
                .withTankerNameKey("tanker_name_" + goal.getId())
                .withParentId(0L);

        Goal defaultGoal = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(0L);

        List<Goal> goals = List.of(adultContentGoal, defaultGoal);

        testCryptaSegmentRepository.addAll(goals);

        List<CryptaGoalWeb> actualGoals = cryptaService.getSegments(CryptaGoalScope.COMMON, clientInfo.getClientId());

        assertFalse(actualGoals.stream().anyMatch(goal -> goal.getId().equals(ADULT_CONTENT_GOAL_ID) && !goal.getAvailableGroups().isEmpty()));
    }


    @Test
    public void getSegmentsAdultContentWithFutureAdded() {
        var clientInfo = steps.clientSteps().createDefaultClient();

        steps.featureSteps().addClientFeature(clientInfo.getClientId(), FeatureName.ALLOW_ADULT_CONTENT, true);

        testCryptaSegmentRepository.clean();

        Goal adultContentGoal = (Goal) defaultGoalWithId(ADULT_CONTENT_GOAL_ID, GoalType.CONTENT_CATEGORY)
                .withTankerNameKey("tanker_name_" + goal.getId())
                .withParentId(0L);

        Goal defaultGoal = (Goal) defaultGoalByTypeWithTankerName(GoalType.CONTENT_CATEGORY)
                .withParentId(0L);

        List<Goal> goals = List.of(adultContentGoal, defaultGoal);

        testCryptaSegmentRepository.addAll(goals);

        List<CryptaGoalWeb> actualGoals = cryptaService.getSegments(CryptaGoalScope.COMMON, clientInfo.getClientId());

        assertTrue(actualGoals.stream().anyMatch(goal -> goal.getId().equals(ADULT_CONTENT_GOAL_ID) && !goal.getAvailableGroups().isEmpty()));
    }

    private static CryptaGoalWeb createCinemaGenres() {
        return new CryptaGoalWeb()
                .withId(Goal.CINEMA_GENRES_GOAL_ID)
                .withParentId(0L);
    }
}
