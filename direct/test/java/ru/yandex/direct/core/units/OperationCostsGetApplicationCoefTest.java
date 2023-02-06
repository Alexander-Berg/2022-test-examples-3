package ru.yandex.direct.core.units;

import javax.annotation.ParametersAreNonnullByDefault;

import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.api.v5.units.ApiUnitsService.UNITS_COSTS_CONF;

@ParametersAreNonnullByDefault
public class OperationCostsGetApplicationCoefTest {

    private static final Costs COSTS = new Costs(ConfigFactory.load(UNITS_COSTS_CONF));

    @Test
    public void getApplicationCoef_defaultCoef() {
        assertThat(COSTS.getApplicationCoef("d41d8cd98f00b204e9800998ecf8427e")).isEqualByComparingTo(1.0);
    }

    @Test
    public void getApplicationCoef_customCoef() {
        assertThat(COSTS.getApplicationCoef("c4c65be43049495099bd13baafddde6b")).isEqualByComparingTo(0.8);
    }

    @Test
    public void getApplicationCoef_CommanderCoef() {
        assertThat(COSTS.getApplicationCoef("c135345132e9449dab6416f3cc34ffab")).isEqualByComparingTo(0.3);
    }
}
