package ru.yandex.market.core.orginfo.model;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для класса {@link OrganizationType}.
 *
 * @author Vadim Lyalin
 */
public class OrganizationTypeTest {
    /**
     * Проверяет, что id уникальны.
     */
    @Test
    void testIdUnique() {
        long idCount = Arrays.stream(OrganizationType.values())
                .map(OrganizationType::getId)
                .distinct()
                .count();

        assertEquals(idCount, OrganizationType.values().length);
    }
}
