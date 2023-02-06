package ru.yandex.market.ff.model.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RegistryUnitType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCount;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.dto.registry.UnitMeta;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;

public class RegistryUnitTest extends SoftAssertionSupport {
    @Test
    void testCopy() {
        var registryUnit = createRegistryUnitWithoutNulls();

        var fieldNamesWithNullValue = ReflectionTestUtils.findFieldNamesWithNullOrDefaultValue(
                registryUnit,
                RegistryUnitEntity.class
        );

        if (!fieldNamesWithNullValue.isEmpty()) {
            assertions.fail(
                    "All fields of copying object must be set. Field names with null or default values: " +
                            fieldNamesWithNullValue
            );
        }

        var actualUnitRegistry = registryUnit.copy(2L);

        var expectedUnitRegistry = createRegistryUnitWithoutNulls();
        // ignore id, items and status history
        expectedUnitRegistry.setId(null);
        expectedUnitRegistry.setRegistryId(2L);
        expectedUnitRegistry.setCreatedAt(null);
        expectedUnitRegistry.setUpdatedAt(null);
        expectedUnitRegistry.setParentUnits(new ArrayList<>());
        expectedUnitRegistry.setChildUnits(new ArrayList<>());

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
                actualUnitRegistry,
                expectedUnitRegistry,
                RegistryUnitEntity.class,
                fieldValuesConsumer
        );
    }

    private RegistryUnitEntity createRegistryUnitWithoutNulls() {
        var registryUnit = new RegistryUnitEntity();
        var createdDate = LocalDateTime.of(2020, 5, 10, 0, 0);
        var updatedDate = LocalDateTime.of(2021, 5, 10, 0, 0);
        registryUnit.setRegistryId(1L);
        registryUnit.setCreatedAt(createdDate);
        registryUnit.setId(1L);
        registryUnit.setChildUnits(List.of(registryUnit));
        registryUnit.setUpdatedAt(updatedDate);
        registryUnit.setIdentifiers(RegistryUnitId.builder()
                .part(UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("aboba").build()).build());
        registryUnit.setMeta(UnitMeta.builder().barcode("barcode").build());
        registryUnit.setType(RegistryUnitType.BOX);
        registryUnit.setUnitCountsInfo(UnitCountsInfo.of(UnitCount.of(UnitCountType.FIT, 1)));
        registryUnit.setParentIds(List.of(RegistryUnitId.builder()
                .part(UnitPartialId.builder().type(RegistryUnitIdType.CIS).value("aboba").build()).build()));
        registryUnit.setParentUnits(List.of(registryUnit));
        return registryUnit;
    }
}
