package ru.yandex.direct.core.entity.metrika.service;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.stub.MetrikaClientStub;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.metrika.client.model.response.CounterGoal;
import ru.yandex.direct.metrika.client.model.response.GoalConversionInfo;

import static org.assertj.core.api.Assertions.assertThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MetrikaGoalsConversionServiceTest {
    private static final Long METRIKA_GOAL_ID = 12349L;
    private static final Long METRIKA_COUNTER_ID = 1233L;

    @Autowired
    private MetrikaClientStub metrikaClientStub;

    @Autowired
    private MetrikaGoalsConversionService metrikaGoalsConversionService;

    @Autowired
    private Steps steps;

    private ClientId clientId;
    private UserInfo defaultUser;

    @Before
    public void before() {
        defaultUser = steps.userSteps().createDefaultUser();
        clientId = defaultUser.getClientId();

        metrikaClientStub.addCounterGoal(METRIKA_COUNTER_ID.intValue(), new CounterGoal()
                .withId(METRIKA_GOAL_ID.intValue())
                .withType(CounterGoal.Type.URL));
    }

    @Test
    public void getGoalsConversion() {
        metrikaClientStub.addUserCounter(defaultUser.getUid(), METRIKA_COUNTER_ID.intValue());
        var visitsCount = new GoalConversionInfo(METRIKA_GOAL_ID, 19L, null);
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(METRIKA_COUNTER_ID.intValue(), METRIKA_GOAL_ID,
                visitsCount.getCount());

        var goalsConversion = metrikaGoalsConversionService.getGoalsConversion(clientId,
                Set.of(METRIKA_COUNTER_ID));

        assertThat(goalsConversion)
                .hasSize(1)
                .containsEntry(METRIKA_GOAL_ID, visitsCount);
    }

    @Test
    public void getGoalsConversionOfUnavailableCounter() {
        long visitsCount = 19L;
        metrikaClientStub.addConversionVisitsCountToGoalIdForTwoWeeks(METRIKA_COUNTER_ID.intValue(), METRIKA_GOAL_ID,
                visitsCount);

        var goalsConversion = metrikaGoalsConversionService.getGoalsConversion(clientId,
                Set.of(METRIKA_COUNTER_ID));

        assertThat(goalsConversion).isEmpty();
    }
}
