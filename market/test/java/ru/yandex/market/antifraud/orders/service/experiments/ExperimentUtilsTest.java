package ru.yandex.market.antifraud.orders.service.experiments;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ExperimentUtilsTest {

    @Test
    public void findRearr() {
        var rearrFactors = "factor1=value1;factor2=value2;factor3=value3";
        assertThat(ExperimentUtils.findRearr(rearrFactors, "factor2"))
            .get().isEqualTo("value2");
    }

    @Test
    public void findMissingRearr() {
        var rearrFactors = "factor1=value1;factor22=value2;factor3=value3";
        assertThat(ExperimentUtils.findRearr(rearrFactors, "factor2"))
            .isEmpty();
    }
}