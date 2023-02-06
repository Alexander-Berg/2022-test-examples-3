package ru.yandex.market.logistics.iris.core.index.field;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.ReferenceIndexerTestFactory;
import ru.yandex.market.logistics.iris.core.index.complex.Dimension;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.iris.core.index.JsonMatchers.matchingJson;

@RunWith(BlockJUnit4ClassRunner.class)
public class NullParsingTest {

    private ChangeTrackingReferenceIndexer referenceIndexer = ReferenceIndexerTestFactory.getIndexer();

    private PredefinedFieldProvider predefinedFieldProvider = ReferenceIndexerTestFactory.getProvider();

    private static final List<Field<?>> PREDEFINED_FIELDS_OF_DIMENSION =
            ImmutableList.of(
                    PredefinedFields.WEIGHT_GROSS,
                    PredefinedFields.WEIGHT_TARE,
                    PredefinedFields.WEIGHT_NETT
            );

    /**
     * Проверяем корректность десериализации null'овых value для всех PredefinedField'ов.
     */
    @Test
    public void nullValuesDeserialization() {
        predefinedFieldProvider.getFields().forEach((fieldName, field) -> {
            String nullJson = "{\"" + fieldName + "\":{\"value\":null,\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";

            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(nullJson);

            assertThat(index.containsExactly(field, null))
                .as("Checking that field [" + fieldName + "] has null value set")
                .isTrue();
        });
    }

    /**
     * Проверяем корректность сериализации null'овых value для всех PredefinedField'ов.
     */
    @Test
    public void nullValuesSerialization() {
        predefinedFieldProvider.getFields()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().isNullable())
            .forEach(entry -> {
                String fieldName = entry.getKey();
                Field<?> field = entry.getValue();
                ChangeTrackingReferenceIndex index = referenceIndexer.createEmptyIndex();
                ZonedDateTime updatedDateTIme = ZonedDateTime.of(LocalDateTime.of(2016, 1, 23, 12, 34, 56), ZoneOffset.UTC);
                index.set(field, null, updatedDateTIme);

                String actualJson = index.toJson(referenceIndexer);

                assertThat(actualJson)
                    .as("Checking that field [" + fieldName + "] has null value set")
                    .is(matchingJson(createNullValueJsonForField(fieldName)));
            });
    }

    /**
     * Проверяем корректность десериализации null'овых value для всех Dimension.
     */
    @Test
    public void nullValuesDeserializationWithRating() {

        PREDEFINED_FIELDS_OF_DIMENSION.forEach(field -> {
            String fieldName = field.getFieldName();
            String nullJson = createNullValueJsonForDimensionField(fieldName);

            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(nullJson);

            assertThat(index.containsExactly((Field<? super Dimension>) field, Dimension.of(null)))
                    .as("Checking that field [" + fieldName + "] has null value set")
                    .isTrue();
        });
    }

    private String createNullValueJsonForField(String fieldName) {
        return "{\"" + fieldName + "\":{\"value\":null, \"utcTimestamp\":\'2016-01-23T12:34:56\'}}";
    }

    private String createNullValueJsonForDimensionField(String fieldName) {
        return "{\"" + fieldName
                + "\":{\"value\":{\"value\": null, \"rating\": 10},\"utcTimestamp\":\"2016-01-23T12:34:56\"}}";
    }
}
