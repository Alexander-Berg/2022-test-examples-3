package ru.yandex.direct.core.entity.turbolanding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategy;
import ru.yandex.direct.core.entity.turbolanding.model.StatusPostModerate;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLanding;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaCounter;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaCounterContainer;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaGoal;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingMetrikaGoalContainer;
import ru.yandex.direct.core.entity.turbolanding.model.TurboLandingWithCountersAndGoals;
import ru.yandex.direct.core.entity.turbolanding.repository.TurboLandingRepository;
import ru.yandex.direct.core.entity.turbolanding.service.TurboLandingService;
import ru.yandex.direct.core.entity.turbolanding.service.UpdateCounterGrantsService;
import ru.yandex.direct.core.entity.turbolanding.service.validation.TurboLandingValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.turbolandings.client.TurboLandingsClient;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.util.Preconditions.checkState;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hibernate.validator.internal.util.CollectionHelper.asSet;
import static org.hibernate.validator.internal.util.CollectionHelper.newArrayList;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.utils.FunctionalUtils.listToMap;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class SaveTurboLandingsTest {
    private static final Long COUNTER_ID_1 = 50001L;
    private static final Long COUNTER_ID_2 = 50002L;
    private static final Long COUNTER_ID_3 = 50003L;
    private static final Long USER_COUNTER_ID = 70001L;
    private static final Long GOAL_ID_1 = 1234L;
    private static final Long GOAL_ID_2 = 5678L;
    private static final Long GOAL_ID_3 = 3456L;
    private static final Long GOAL_ID_4 = 9012L;

    @Autowired
    private Steps steps;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private TurboLandingRepository turboLandingRepository;
    @Autowired
    private TurboLandingValidationService turboLandingValidationService;
    @Autowired
    private TurboLandingsClient turboLandingsClient;


    private UpdateCounterGrantsService updateCounterGrantsService;
    private TurboLandingService turboLandingService;

    private int shard;
    private ClientId clientId;
    private Long operatorUid;


    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId();
        operatorUid = clientInfo.getUid();
        shard = clientInfo.getShard();

        updateCounterGrantsService = mock(UpdateCounterGrantsService.class);
        turboLandingService = new TurboLandingService(shardHelper, turboLandingRepository, updateCounterGrantsService, turboLandingValidationService, turboLandingsClient);
    }

    @Test
    public void saveTurboLandings_callRefreshMetrikaGrants_forNotUserCounter() {
        TurboLandingWithCountersAndGoals turboLanding = turboLandingWithCountersAndGoals(
                clientId,
                asSet(
                    turboLandingCounter(COUNTER_ID_1, false),
                    turboLandingCounter(USER_COUNTER_ID, true)),
                emptySet()
        );
        turboLandingService.saveTurboLandings(operatorUid, clientId, singletonList(turboLanding));

        verify(updateCounterGrantsService).refreshMetrikaGrants(eq(operatorUid),
                eq(singletonMap(clientId, singleton(COUNTER_ID_1))));
    }

    @Test
    public void saveTurboLandings_callRefreshMetrikaGrants_forNewCounter() {
        steps.turboLandingSteps().createTurboLandingWithCountersAndGoals(clientId,
                turboLandingWithCountersAndGoals(clientId,
                        asSet(turboLandingCounter(COUNTER_ID_1, false)),
                        emptySet()
                ));

        TurboLandingWithCountersAndGoals turboLanding = turboLandingWithCountersAndGoals(clientId,
                asSet(
                    turboLandingCounter(COUNTER_ID_1, false),
                    turboLandingCounter(COUNTER_ID_2, false)
                ),
                emptySet()
        );

        turboLandingService.saveTurboLandings(operatorUid, clientId, singletonList(turboLanding));

        verify(updateCounterGrantsService).refreshMetrikaGrants(eq(operatorUid),
                eq(singletonMap(clientId, singleton(COUNTER_ID_2))));
    }

    @Test
    public void saveTurboLandings_callRefreshMetrikaGrants_whenTwoTurboLandings() {
        List<TurboLandingWithCountersAndGoals> turboLandings = asList(
                turboLandingWithCountersAndGoals(clientId, asSet(
                        turboLandingCounter(COUNTER_ID_1, false),
                        turboLandingCounter(COUNTER_ID_2, false),
                        turboLandingCounter(USER_COUNTER_ID, true)),
                        emptySet()
                ),

                turboLandingWithCountersAndGoals(clientId, asSet(
                        turboLandingCounter(COUNTER_ID_2,  false),
                        turboLandingCounter(COUNTER_ID_3,  false)),
                        emptySet()
                )
        );
        turboLandingService.saveTurboLandings(operatorUid, clientId, turboLandings);

        verify(updateCounterGrantsService).refreshMetrikaGrants(eq(operatorUid),
                eq(singletonMap(clientId, asSet(COUNTER_ID_1, COUNTER_ID_2, COUNTER_ID_3))));
    }

    @Test
    public void saveTurboLandings_addToTurboLandingsTable() {
        TurboLandingWithCountersAndGoals turboLanding1 =
                turboLandingWithCountersAndGoals(clientId, emptySet(), emptySet());
        TurboLandingWithCountersAndGoals turboLanding2 =
                turboLandingWithCountersAndGoals(clientId,
                        asSet(turboLandingCounter(COUNTER_ID_1, false)),
                        asSet(
                                turboLandingGoal(GOAL_ID_1, false),
                                turboLandingGoal(GOAL_ID_2, false))
                );
        TurboLandingWithCountersAndGoals turboLanding3 = turboLandingWithCountersAndGoals(clientId,
                asSet(
                    turboLandingCounter(COUNTER_ID_1, false),
                    turboLandingCounter(COUNTER_ID_2, false),
                    turboLandingCounter(USER_COUNTER_ID, true)),
                asSet(
                        turboLandingGoal(GOAL_ID_1, false),
                        turboLandingGoal(GOAL_ID_3, false),
                        turboLandingGoal(GOAL_ID_4, false))
        );

        List<TurboLandingWithCountersAndGoals> turboLandings =
                asList(turboLanding1, turboLanding2, turboLanding3);
        List<Long> turboLandingIds = mapList(turboLandings, TurboLanding::getId);

        turboLandingService.saveTurboLandings(operatorUid, clientId, turboLandings);

        Map<Long, TurboLanding> actualTurboLandingById = getActualTurboLandingsMap(turboLandingIds);

        Map<Long, List<TurboLandingMetrikaCounter>> actualCountersByTurboLandingId =
                getActualCountersByTurboLandingId(turboLandingIds);

        SoftAssertions soft = new SoftAssertions();

        for (var expectedTurboLanding : turboLandings) {
            TurboLanding actualTurboLanding = actualTurboLandingById.get(expectedTurboLanding.getId());

            soft.assertThat(new TurboLandingWithCountersAndGoals(actualTurboLanding))
                    .is(matchedBy(beanDiffer(expectedTurboLanding)
                            .useCompareStrategy(getCompareStrategyForActualFields())));

            List<TurboLandingMetrikaCounter> expectedCounters =  getCountersFromTurboLanding(expectedTurboLanding);
            List<TurboLandingMetrikaCounter> actualCounters = getTurboLandingMetrikaCounters(
                    expectedTurboLanding.getId(), actualCountersByTurboLandingId);

            soft.assertThat(actualCounters).is(matchedBy(beanDiffer(expectedCounters)));
        }
        soft.assertAll();
    }

    @Test
    public void saveTurboLandings_applyDefaultFieldsForModeration() {
        TurboLandingWithCountersAndGoals turboLanding =
                turboLandingWithCountersAndGoals(clientId, emptySet(), emptySet());
        turboLanding.setLastModeratedVersion(null);
        turboLanding.setVersion(0L);
        turboLanding.setStatusPostModerate(null);
        turboLanding.setIsChanged(null);

        turboLandingService.saveTurboLandings(operatorUid, clientId, singletonList(turboLanding));

        // Параметры, которые должны быть установлены, если получили нулевую версию турболендинга
        TurboLanding expected = new TurboLanding()
                .withVersion(0L)
                .withLastModeratedVersion(0L)
                .withStatusPostModerate(StatusPostModerate.YES)
                .withIsChanged(false);

        List<TurboLanding> actualTurboLandings =
                turboLandingRepository.getClientTurboLandingsbyId(shard, clientId, singletonList(turboLanding.getId()));
        checkState(actualTurboLandings.size() > 0, "Турболендинг сохранился.");

        Assertions.assertThat(actualTurboLandings.get(0)).is(matchedBy(beanDiffer(expected)
                .useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void saveTurboLandings_updateTurboLandingsTable() {
        TurboLandingWithCountersAndGoals turboLanding =
                turboLandingWithCountersAndGoals(clientId, emptySet(), emptySet());
        Long turboLandingId = turboLanding.getId();

        turboLandingService.saveTurboLandings(operatorUid, clientId, singletonList(turboLanding));
        List<TurboLanding> oldTurboLandings =
                turboLandingRepository.getClientTurboLandingsbyId(shard, clientId, singletonList(turboLandingId));
        checkState(!oldTurboLandings.isEmpty(), "Турболендинг успешно сохранился.");

        TurboLandingWithCountersAndGoals updatedTurboLanding = new TurboLandingWithCountersAndGoals();
        updatedTurboLanding.withCounters(asSet(turboLandingCounter(COUNTER_ID_1, false)))
                .withGoals(asSet(turboLandingGoal(GOAL_ID_1, false), turboLandingGoal(GOAL_ID_2, false)))
                .withId(turboLanding.getId())
                .withClientId(turboLanding.getClientId())
                .withUrl("updated url")
                .withName("updated name")
                .withPreviewHref("updated preview_href")
                .withTurboSiteHref("updated turbo_site_href")
                .withIsChanged(true)
                .withIsCpaModerationRequired(false)
                .withVersion(turboLanding.getVersion() + 1)
                .withLastModeratedVersion(turboLanding.getLastModeratedVersion() + 1)
                .withStatusPostModerate(StatusPostModerate.NO)
                .withMetrikaCounters("{}");

        checkState(!turboLanding.equals(updatedTurboLanding), "Параметры турболендингов отличаются.");

        turboLandingService.saveTurboLandings(operatorUid, clientId, singletonList(updatedTurboLanding));

        List<TurboLanding> actualTurboLandings =
                turboLandingRepository.getClientTurboLandingsbyId(shard, clientId, singletonList(turboLandingId));

        Map<Long, List<TurboLandingMetrikaCounter>> actualCountersByTurboLandingId =
                getActualCountersByTurboLandingId(singletonList(turboLandingId));

        SoftAssertions soft = new SoftAssertions();

        var actualTurboLanding = new TurboLandingWithCountersAndGoals(actualTurboLandings.get(0));
        soft.assertThat(actualTurboLanding).is(matchedBy(beanDiffer(updatedTurboLanding)
                .useCompareStrategy(getCompareStrategyForActualFields())));

        List<TurboLandingMetrikaCounter> expectedCounters = getCountersFromTurboLanding(updatedTurboLanding);
        List<TurboLandingMetrikaCounter> actualCounters = getTurboLandingMetrikaCounters(
                turboLandingId, actualCountersByTurboLandingId);

        soft.assertThat(actualCounters).is(matchedBy(beanDiffer(expectedCounters)));

        soft.assertAll();
    }

    @Test
    public void saveTurboLandings_addToTurboLandingMetrikaCountersTable() {
        TurboLandingWithCountersAndGoals turboLanding1 =
                turboLandingWithCountersAndGoals(clientId, emptySet(), emptySet());
        TurboLandingWithCountersAndGoals turboLanding2 =
                turboLandingWithCountersAndGoals(clientId,
                        asSet(turboLandingCounter(COUNTER_ID_1, false)),
                        asSet(
                                turboLandingGoal(GOAL_ID_1, false),
                                turboLandingGoal(GOAL_ID_2, false))
        );
        TurboLandingWithCountersAndGoals turboLanding3 =
                turboLandingWithCountersAndGoals(clientId,
                    asSet(
                            turboLandingCounter(COUNTER_ID_1, false),
                            turboLandingCounter(COUNTER_ID_2, false),
                            turboLandingCounter(USER_COUNTER_ID, true)),
                    asSet(
                            turboLandingGoal(GOAL_ID_1, false),
                            turboLandingGoal(GOAL_ID_3, false),
                            turboLandingGoal(GOAL_ID_4, false)));

        List<TurboLandingWithCountersAndGoals> turboLandings = asList(turboLanding1, turboLanding2, turboLanding3);
        List<Long> turboLandingIds = mapList(turboLandings, TurboLanding::getId);

        turboLandingService.saveTurboLandings(operatorUid, clientId, turboLandings);

        List<TurboLandingMetrikaCounterContainer> actualCounters =
                turboLandingRepository.getTurboLandingMetrikaCounters(shard, turboLandingIds);

        List<TurboLandingMetrikaCounterContainer> expectedCounters = asList(
                turboLandingCounterContainer(turboLanding2.getId(), COUNTER_ID_1, false),
                turboLandingCounterContainer(turboLanding3.getId(), COUNTER_ID_1, false),
                turboLandingCounterContainer(turboLanding3.getId(), COUNTER_ID_2, false),
                turboLandingCounterContainer(turboLanding3.getId(), USER_COUNTER_ID, true)
        );
        assertThat(actualCounters, containsInAnyOrder(mapList(expectedCounters, BeanDifferMatcher::beanDiffer)));
    }

    @Test
    public void saveTurboLandings_addToTurboLandingMetrikaGoalsTable() {
        TurboLandingWithCountersAndGoals turboLanding1 = turboLandingWithCountersAndGoals(clientId,
                emptySet(), emptySet());
        TurboLandingWithCountersAndGoals turboLanding2 = turboLandingWithCountersAndGoals(clientId,
                asSet(turboLandingCounter(COUNTER_ID_1, false)),
                asSet(turboLandingGoal(GOAL_ID_1, false), turboLandingGoal(GOAL_ID_2, false))
        );
        TurboLandingWithCountersAndGoals turboLanding3 = turboLandingWithCountersAndGoals(clientId,
                asSet(
                        turboLandingCounter(COUNTER_ID_1, false),
                        turboLandingCounter(COUNTER_ID_2, false),
                        turboLandingCounter(USER_COUNTER_ID, true)),
                asSet(
                        turboLandingGoal(GOAL_ID_1, true),
                        turboLandingGoal(GOAL_ID_3, false),
                        turboLandingGoal(GOAL_ID_4, false)));

        List<TurboLandingWithCountersAndGoals> turboLandings = asList(turboLanding1, turboLanding2, turboLanding3);
        List<Long> turboLandingIds = mapList(turboLandings, TurboLanding::getId);

        turboLandingService.saveTurboLandings(operatorUid, clientId, turboLandings);

        List<TurboLandingMetrikaGoalContainer> actualGoals =
                turboLandingRepository.getTurboLandingMetrikaGoals(shard, turboLandingIds);

        List<TurboLandingMetrikaGoalContainer> expectedGoals = asList(
                turboLandingGoalContainer(turboLanding2.getId(), GOAL_ID_1),
                turboLandingGoalContainer(turboLanding2.getId(), GOAL_ID_2),
                turboLandingGoalContainer(turboLanding3.getId(), GOAL_ID_1).withIsConversionGoal(true),
                turboLandingGoalContainer(turboLanding3.getId(), GOAL_ID_3),
                turboLandingGoalContainer(turboLanding3.getId(), GOAL_ID_4)
        );
        assertThat(actualGoals, containsInAnyOrder(mapList(expectedGoals, BeanDifferMatcher::beanDiffer)));
    }

    private TurboLandingWithCountersAndGoals turboLandingWithCountersAndGoals(ClientId clientId,
            Set<TurboLandingMetrikaCounter> counters, Set<TurboLandingMetrikaGoal> goals) {
        return new TurboLandingWithCountersAndGoals(steps.turboLandingSteps().defaultTurboLanding(clientId))
                .withCounters(counters)
                .withGoals(goals);
    }

    private TurboLandingMetrikaCounter turboLandingCounter(Long counterId, Boolean userCounter) {
        TurboLandingMetrikaCounter counter = new TurboLandingMetrikaCounter();

        counter.setId(counterId);
        counter.setIsUserCounter(userCounter);

        return counter;
    }

    private TurboLandingMetrikaGoal turboLandingGoal(Long goalId, Boolean conversionGoal) {
        TurboLandingMetrikaGoal goal = new TurboLandingMetrikaGoal();

        goal.setId(goalId);
        goal.setIsConversionGoal(conversionGoal);

        return goal;
    }

    private TurboLandingMetrikaCounterContainer turboLandingCounterContainer(Long turboLandingId, Long counterId,
                                                                             Boolean isUserCounter) {
        return new TurboLandingMetrikaCounterContainer()
                .withTurbolandingId(turboLandingId)
                .withCounterId(counterId)
                .withIsUserCounter(isUserCounter);
    }

    private TurboLandingMetrikaGoalContainer turboLandingGoalContainer(Long turboLandingId, Long goalId) {
        return new TurboLandingMetrikaGoalContainer()
                .withTurbolandingId(turboLandingId)
                .withGoalId(goalId)
                .withIsConversionGoal(false);
    }

    private List<TurboLandingMetrikaCounter> getCountersFromTurboLanding(
            TurboLandingWithCountersAndGoals turboLanding) {
        return Optional.ofNullable(turboLanding)
                .map(TurboLandingWithCountersAndGoals::getCounters)
                .map(ArrayList::new)
                .orElse(new ArrayList<>());
    }


    private Map<Long, TurboLanding> getActualTurboLandingsMap(List<Long> turboLandingIds) {
        List<TurboLanding> actualTurboLandings =
                turboLandingRepository.getClientTurboLandingsbyId(shard, clientId, turboLandingIds);
        return listToMap(actualTurboLandings, TurboLanding::getId);
    }

    private Map<Long, List<TurboLandingMetrikaCounter>> getActualCountersByTurboLandingId(List<Long> turboLandingIds) {
        List<TurboLandingMetrikaCounterContainer> actualCounterContainers =
                turboLandingRepository.getTurboLandingMetrikaCounters(shard, turboLandingIds);

        Map<Long, List<TurboLandingMetrikaCounter>> actualCountersByTurboLandingId = new HashMap<>();
        for (var counterContainer : actualCounterContainers) {
            Long turboLandingId = counterContainer.getTurbolandingId();
            actualCountersByTurboLandingId.computeIfAbsent(turboLandingId, id -> newArrayList());
            actualCountersByTurboLandingId.get(turboLandingId)
                    .add(new TurboLandingMetrikaCounter()
                            .withId(counterContainer.getCounterId())
                            .withIsUserCounter(counterContainer.getIsUserCounter())
                    );
        }
        return actualCountersByTurboLandingId;
    }

    private List<TurboLandingMetrikaCounter> getTurboLandingMetrikaCounters(Long turboLandingId,
                            Map<Long, List<TurboLandingMetrikaCounter>> countersByTurboLandingId) {
        return Optional.ofNullable(countersByTurboLandingId.get(turboLandingId))
                .orElse(newArrayList());
    }

    private DefaultCompareStrategy getCompareStrategyForActualFields() {
        return allFieldsExcept(newPath("counters"), newPath("goals"));
    }
}
