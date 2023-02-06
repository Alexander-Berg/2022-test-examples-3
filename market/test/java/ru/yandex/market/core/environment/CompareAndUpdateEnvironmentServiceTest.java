package ru.yandex.market.core.environment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.assertj.core.api.Assertions.assertThat;

@DbUnitDataSet(before = "CompareAndUpdateEnvironmentServiceTest.before.csv")
class CompareAndUpdateEnvironmentServiceTest extends FunctionalTest {

    private static final String DEFAULT_VALUE = "defaultValue";

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private CompareAndUpdateEnvironmentService compareAndUpdateEnvironmentService;

    @Test
    void testCompareAndUpdateValue() {
        final boolean updated = compareAndUpdateEnvironmentService.compareAndUpdateValue("var", "2", "1");
        assertTrue(updated);

        final String newValue = environmentService.getValue("var", DEFAULT_VALUE);
        assertEquals("2", newValue);
    }

    @Test
    void testCompareAndUpdateValues() {
        final boolean updated = compareAndUpdateEnvironmentService.compareAndUpdateValue("var2", "5", "2");
        assertTrue(updated);

        final List<String> values = environmentService.getValues("var2", Collections.emptyList());
        assertThat(values).containsExactlyInAnyOrder("1", "3", "5");
    }

    @Test
    void testCompareAndDoNotUpdateValue() {
        final boolean updated = compareAndUpdateEnvironmentService.compareAndUpdateValue("var", "2", "5");
        assertFalse(updated);

        final String newValue = environmentService.getValue("var", DEFAULT_VALUE);
        assertEquals("1", newValue);
    }
}
