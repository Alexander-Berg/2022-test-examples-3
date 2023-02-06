package ru.yandex.direct.core.units;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static ru.yandex.direct.core.units.OperationCosts.CALL_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.MIN_DAILY_LIMIT_KEY;
import static ru.yandex.direct.core.units.OperationCosts.OBJECT_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.OBJECT_ERROR_COST_KEY;
import static ru.yandex.direct.core.units.OperationCosts.REQUEST_ERROR_COST_KEY;

public class OperationCostsTest {

    private OperationCosts operationCosts;
    private OperationCosts operationCostsWithMinDailyLimit;

    @Before
    public void setup() {
        Config config = ConfigFactory.parseMap(ImmutableMap.of(
                CALL_COST_KEY, 10,
                OBJECT_COST_KEY, 20,
                OBJECT_ERROR_COST_KEY, 40,
                REQUEST_ERROR_COST_KEY, 80
        ));
        operationCosts = new OperationCosts(config);

        Config configWithMinDailyLimit = ConfigFactory.parseMap(ImmutableMap.of(
                CALL_COST_KEY, 10,
                OBJECT_COST_KEY, 20,
                OBJECT_ERROR_COST_KEY, 40,
                REQUEST_ERROR_COST_KEY, 80,
                MIN_DAILY_LIMIT_KEY, 168000
        ));

        operationCostsWithMinDailyLimit = new OperationCosts(configWithMinDailyLimit);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void calcRequestCost_correctOnBrokenResults() {
//        MassResult brokenMassAction = MassResult.brokenMassAction(
//                new MassValidationBuilder<>().addOperationalErrors(
//                        Collections.singletonList(new Defect(null, null, 0, null, null, null))).build());
//        int cost = operationCosts.calcRequestCost(brokenMassAction);
//        assertThat(cost, is(90));
    }

    @Test
    public void minDailyLimit() {
        assertThat(operationCosts.getMinDailyLimit(), is(0));
        assertThat(operationCostsWithMinDailyLimit.getMinDailyLimit(), is(168000));
    }
}
