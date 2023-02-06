package ru.yandex.market.mbo.randomizers;

import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValues;

import io.github.benas.randombeans.api.EnhancedRandom;
import io.github.benas.randombeans.api.Randomizer;

/**
 * @author s-ermakov
 */
public class ParameterValuesRandomizer implements Randomizer<ParameterValues> {
    private static final int PARAMETER_VALUES_SIZE = 2;

    private final EnhancedRandom enhancedRandom;
    private final ParameterValueRandomizer randomizer;

    public ParameterValuesRandomizer(ParameterValueRandomizer parameterValueRandomizer, EnhancedRandom enhancedRandom) {
        this.enhancedRandom = enhancedRandom;
        this.randomizer = parameterValueRandomizer;
    }

    @Override
    public ParameterValues getRandomValue() {
        int size = enhancedRandom.nextInt(PARAMETER_VALUES_SIZE);

        ParameterValues result = ParameterValues.of(randomizer.getRandomValue());
        for (int i = 0; i < size; i++) {
            ParameterValue parameterValue = randomizer.getRandomValue(result.getType());
            parameterValue.setParamId(result.getParamId());
            parameterValue.setXslName(result.getXslName());
            result.addValue(parameterValue);
        }

        return result;
    }
}
