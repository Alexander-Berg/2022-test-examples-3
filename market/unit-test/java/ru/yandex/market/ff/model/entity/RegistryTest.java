package ru.yandex.market.ff.model.entity;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RegistryFlowType;
import ru.yandex.market.ff.model.dto.registry.RegistryRestrictedData;
import ru.yandex.market.ff.model.entity.registry.RegistryEntity;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;

public class RegistryTest extends SoftAssertionSupport {
    @Test
    void testCopy() {
        var registry = createRegistryWithoutNulls();

        var fieldNamesWithNullValue = ReflectionTestUtils.findFieldNamesWithNullOrDefaultValue(
                registry,
                RegistryEntity.class
        );

        if (!fieldNamesWithNullValue.isEmpty()) {
            assertions.fail(
                    "All fields of copying object must be set. Field names with null or default values: " +
                            fieldNamesWithNullValue
            );
        }

        var actualRegistry = registry.copyWithoutLinks(2L);

        var expectedRegistry = createRegistryWithoutNulls();
        // ignore id, items and status history
        expectedRegistry.setId(null);
        expectedRegistry.setUnitCounts(null);
        expectedRegistry.setRegistryUnits(null);
        expectedRegistry.setCreatedAt(null);
        expectedRegistry.setUpdatedAt(null);
        expectedRegistry.setRequestId(null);
        expectedRegistry.setRequestId(2L);

        ReflectionTestUtils.AssertingFieldValuesConsumer fieldValuesConsumer =
                (fieldName, actualFieldValue, expectedFieldValue) -> {
                    if (actualFieldValue instanceof Collection) {
                        Collection actual = (Collection) actualFieldValue;
                        Collection expected = (Collection) expectedFieldValue;
                        assertions.assertThat(actual.size()).as(fieldName).isEqualTo(expected.size());
                    } else {
                        assertions.assertThat(actualFieldValue).as(fieldName).isEqualTo(expectedFieldValue);
                    }
                };

        ReflectionTestUtils.compareFieldValues(
                actualRegistry,
                expectedRegistry,
                RegistryEntity.class,
                fieldValuesConsumer
        );
    }

    private RegistryEntity createRegistryWithoutNulls() {
        var registry = new RegistryEntity();
        var createdDate = LocalDateTime.of(2020, 5, 10, 0, 0);
        var updatedDate = LocalDateTime.of(2021, 5, 10, 0, 0);
        var offsetDate = OffsetDateTime.of(createdDate, ZoneOffset.UTC);
        registry.setCreatedAt(createdDate);
        registry.setRegistryUnits(List.of(RegistryUnitEntity.builder().registryId(1L).build()));
        registry.setId(1L);
        registry.setComment("aboba");
        registry.setType(RegistryFlowType.FACT);
        registry.setDate(offsetDate);
        registry.setDocumentId("document");
        registry.setPartnerId("partner");
        registry.setRequestId(1L);
        registry.setRestrictedData(RegistryRestrictedData.builder().build());
        registry.setUpdatedAt(updatedDate);
        registry.setUnitCounts(List.of());
        return registry;
    }
}
