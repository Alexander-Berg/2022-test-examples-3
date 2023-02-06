package ru.yandex.market.core.asyncreport.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Тесты для {@link  ReportsType}.
 */
class ReportsTypeTest {

    @ParameterizedTest
    @EnumSource(ReportsType.class)
    void testTypes(final ReportsType type) {
        assertNotNull(type.getEntityName());
        assertNotNull(type.getGroup());
        assertNotNull(type.getId());
        assertNotNull(type.getDescription());
    }

}
