package ru.yandex.direct.api.v5.units;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Ignore;
import org.junit.Test;

import static ru.yandex.direct.api.v5.units.ApiUnitsService.UNITS_COSTS_CONF;

public class PrintCalculatedCostsTest {

    @Test
    @Ignore("This test should be used manually for eye-check")
    public void printCalculatedCosts() {
        Config costs = ConfigFactory.load(UNITS_COSTS_CONF)
                .getConfig("costs");
        System.out.println("Calculated costs: \n" + costs.root().render());
    }
}
