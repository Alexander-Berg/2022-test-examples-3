package ru.yandex.direct.core.units;

import java.util.Collections;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.junit.Before;
import org.junit.Test;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.units.Costs.COST_MULTIPLIERS_CONFIG_KEY;
import static ru.yandex.direct.core.units.Costs.DEFAULT_REQUEST_ERROR_COST_CONFIG_KEY;
import static ru.yandex.direct.core.units.Costs.OPERATIONS_COSTS_CONFIG_KEY;
import static ru.yandex.direct.core.units.Costs.REQUEST_ERROR_COSTS_CONFIG_KEY;

public class CostsGetServiceErrorCostTest {
    private static final int DEFAULT_REQUEST_ERROR_COST = 1;
    private static final int KNOWN_ERROR_CODE = 2;
    private static final int KNOWN_ERROR_COST = 3;
    private static final int UNKNOWN_ERROR_CODE = 4;

    private Costs costs;

    @Before
    public void setUp() {
        costs = new Costs(
                ConfigFactory.empty()
                        .withValue(
                                COST_MULTIPLIERS_CONFIG_KEY,
                                ConfigValueFactory.fromMap(emptyMap()))
                        .withValue(
                                OPERATIONS_COSTS_CONFIG_KEY,
                                ConfigValueFactory.fromMap(emptyMap()))
                        .withValue(
                                DEFAULT_REQUEST_ERROR_COST_CONFIG_KEY,
                                ConfigValueFactory.fromAnyRef(DEFAULT_REQUEST_ERROR_COST))
                        .withValue(
                                REQUEST_ERROR_COSTS_CONFIG_KEY,
                                ConfigValueFactory.fromMap(
                                        Collections.singletonMap(
                                                Integer.toString(KNOWN_ERROR_CODE),
                                                KNOWN_ERROR_COST))));
    }

    @Test
    public void testIfErrorCodeIsNull() {
        assertThat(
                costs.getServiceErrorCost(null), equalTo(DEFAULT_REQUEST_ERROR_COST));
    }

    @Test
    public void testIfErrorCodeIsKnown() {
        assertThat(
                costs.getServiceErrorCost(KNOWN_ERROR_CODE), equalTo(KNOWN_ERROR_COST));
    }

    @Test
    public void testIfErrorCodeIsUnknown() {
        assertThat(
                costs.getServiceErrorCost(UNKNOWN_ERROR_CODE), equalTo(DEFAULT_REQUEST_ERROR_COST));
    }
}
