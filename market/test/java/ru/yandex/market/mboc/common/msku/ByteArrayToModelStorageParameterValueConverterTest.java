package ru.yandex.market.mboc.common.msku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mboc.common.repo.bindings.proto.ByteArrayToModelStorageParameterValueConverter;

public class ByteArrayToModelStorageParameterValueConverterTest {

    private static final long SEED = 10062123;
    private static final int LIST_SIZE = 10;

    private static final int RETRY_COUNT = 10;

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(SEED)
        .overrideDefaultInitialization(true)
        .build();

    private ByteArrayToModelStorageParameterValueConverter converter =
        new ByteArrayToModelStorageParameterValueConverter();

    @Test
    public void shouldStoreToByteArrayAndRestoreThemBackToEqualLists() {
        List<ModelStorage.ParameterValue> before = new ArrayList<>();
        for (int i = 0; i < RETRY_COUNT; i++) {
            for (int j = 0; j < LIST_SIZE; j++) {
                before.add(ModelStorage.ParameterValue.newBuilder()
                    .setXslName(random.nextObject(String.class))
                    .setBoolValue(random.nextBoolean())
                    .setParamId(random.nextLong())
                    .setOptionId(random.nextInt())
                    .setRuleId(random.nextInt())
                    .setValueSource(random.nextObject(ModelStorage.ModificationSource.class))
                    .setValueType(random.nextObject(MboParameters.ValueType.class))
                    .build());
            }
            Assertions.assertThat(converter.from(converter.to(before)))
                .isEqualTo(before);
            before.clear();
        }
    }

    @Test
    public void shouldConvertNullAndEmptyParametersToEmptyFields() {
        Assertions.assertThat(converter.to(Collections.emptyList()))
            .isNotNull()
            .isEmpty();
        Assertions.assertThat(converter.from(new byte[0]))
            .isNotNull()
            .isEmpty();

        Assertions.assertThat(converter.to(null))
            .isNull();
        Assertions.assertThat(converter.from(null))
            .isNull();
    }
}
