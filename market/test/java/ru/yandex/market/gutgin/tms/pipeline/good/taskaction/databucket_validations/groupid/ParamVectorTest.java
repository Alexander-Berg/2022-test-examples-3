package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid;

import org.junit.Test;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.groupid.ParamVector.ParameterValue;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ParamVectorTest {

    @Test
    public void initializationWithNullValuesIsCorrect() {
        ParameterValue[] parameterValues = parameterValuesWithNull();
        ParamVector paramVector = new ParamVector(parameterValues);
        assertThat(paramVector).isNotNull();
    }

    @Test
    public void getParameterIdDiff_withNull() {
        ParamVector vector1 = vector(null, null, v(2), v(3), v(4), empty(), null, empty());
        ParamVector vector2 = vector(null, v(1), null, v(3), v(5), null, empty(), empty());
        Set<Long> parameterIdDiff = vector1.getParameterIdDiff(vector2);
        assertThat(parameterIdDiff).containsExactly(1L, 2L, 5L);
    }

    private ParameterValue empty() {
        return new ParameterValue();
    }

    private ParameterValue v(int id) {
        ParameterValue parameterValue = empty();
        parameterValue.value = Integer.toString(id);
        parameterValue.parameterId = ((long) id);
        return parameterValue;
    }

    private ParamVector vector(ParameterValue... values) {
        return new ParamVector(values);
    }

    private ParameterValue[] parameterValuesWithNull() {
        return new ParameterValue[]{
            empty(),
            v(1),
            null
        };
    }
}