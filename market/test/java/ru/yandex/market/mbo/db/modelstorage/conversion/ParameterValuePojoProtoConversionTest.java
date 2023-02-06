package ru.yandex.market.mbo.db.modelstorage.conversion;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.randomizers.ParameterValueRandomizer;

/**
 * @author s-ermakov
 */
public class ParameterValuePojoProtoConversionTest {
    private static final long RANDOM_SEED = 1092000L;

    private EnhancedRandom random;
    private ParameterValueRandomizer randomizer;

    @Before
    @SuppressWarnings("checkstyle:magicNumber")
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .seed(RANDOM_SEED)
            .stringLengthRange(3, 10)
            .collectionSizeRange(1, 5)
            .build();

        randomizer = new ParameterValueRandomizer(random);
    }

    @Test
    public void testDoubleConversion() {
        ParameterValue parameterValue = randomizer.getRandomValue();

        ModelStorage.ParameterValue proto = ModelProtoConverter.convert(parameterValue);
        ParameterValue parameterValue2 = ModelProtoConverter.convert(proto);

        Assertions.assertThat(parameterValue2).isEqualToComparingFieldByField(parameterValue);
    }

    @Test
    public void testCopy() {
        ParameterValue parameterValue = randomizer.getRandomValue();
        ParameterValue copy = new ParameterValue(parameterValue);

        Assertions.assertThat(copy).isEqualToComparingFieldByField(parameterValue);
    }
}
