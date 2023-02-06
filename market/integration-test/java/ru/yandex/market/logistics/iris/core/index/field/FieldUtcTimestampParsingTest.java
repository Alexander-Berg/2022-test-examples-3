package ru.yandex.market.logistics.iris.core.index.field;

import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import ru.yandex.market.logistics.iris.core.index.ChangeTrackingReferenceIndex;
import ru.yandex.market.logistics.iris.core.index.ReferenceIndexerTestFactory;
import ru.yandex.market.logistics.iris.core.index.implementation.ChangeTrackingReferenceIndexer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.logistics.iris.core.index.JsonMatchers.matchingJsonWithoutOrder;

@RunWith(BlockJUnit4ClassRunner.class)
public class FieldUtcTimestampParsingTest {

    private ChangeTrackingReferenceIndexer referenceIndexer = ReferenceIndexerTestFactory.getIndexer();
    private PredefinedFieldProvider predefinedFieldProvider = ReferenceIndexerTestFactory.getProvider();

    /**
     * Проверяем корректность десериализации null'овых utcTimestamp'ов для всех PredefinedField'ов.
     */
    @Test
    public void nullTimestampDeserialization() {
        predefinedFieldProvider.getFields().forEach((fieldName, field) -> {
            String nullJson = "{\"" + fieldName + "\":{\"value\":null,\"utcTimestamp\":null}}";

            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(nullJson);

            assertThat(index.getUtcTimestamp(field))
                .as("Checking utcTimestamp for field [" + fieldName + "] = MIN")
                .contains(LocalDate.of(1970, 1, 1).atStartOfDay(ZoneOffset.UTC));
        });
    }

    /**
     * Проверяем корректность десериализации отсутствующего utcTimestamp для всех PredefinedField'ов.
     */
    @Test
    public void missingTimestampDeserialization() {
        predefinedFieldProvider.getFields().forEach((fieldName, field) -> {
            String nullJson = "{\"" + fieldName + "\":{\"value\":null}}";

            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(nullJson);

            assertThat(index.getUtcTimestamp(field))
                .as("Checking utcTimestamp for field [" + fieldName + "] = MIN")
                .contains(LocalDate.of(1970, 1, 1).atStartOfDay(ZoneOffset.UTC));
        });
    }

    /**
     * Проверяем корректность сериализации null'ового utcTimestamp для всех PredefinedField'ов.
     */
    @Test
    public void nullTimestampSerialization() {
        predefinedFieldProvider.getFields().forEach((fieldName, field) -> {
            String initialJson = "{\"" + fieldName + "\":{\"value\":null,\"utcTimestamp\":null}}";

            ChangeTrackingReferenceIndex index = referenceIndexer.fromJson(initialJson);
            String actualJson = index.toJson(referenceIndexer);

            String expectedJson = "{\"" + fieldName + "\":{\"value\":null,\"utcTimestamp\":\"1970-01-01T00:00:00\"}}";

            assertThat(actualJson)
                .as("Checking that JSONS for field [" + fieldName + "] are equal")
                .is(matchingJsonWithoutOrder(expectedJson));
        });
    }
}
