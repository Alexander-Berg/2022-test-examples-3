package ru.yandex.market.partner.content.common.entity;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author s-ermakov
 */
public class ModelTest {

    @Test
    public void testSetSeveralParameterValuesWithEqualParamId() {
        ParameterValue parameterValue1 = new ParameterValue();
        parameterValue1.setParamId(1);
        parameterValue1.setType(ParameterType.STRING);
        parameterValue1.setStringValue("test1");

        ParameterValue parameterValue2 = new ParameterValue();
        parameterValue2.setParamId(1);
        parameterValue2.setType(ParameterType.STRING);
        parameterValue2.setStringValue("test2");

        Model model = new Model();
        model.setParameterList(Arrays.asList(parameterValue1, parameterValue2));

        Assertions.assertThat(model.getParameterList())
            .containsExactlyInAnyOrder(parameterValue1, parameterValue2);
    }
}