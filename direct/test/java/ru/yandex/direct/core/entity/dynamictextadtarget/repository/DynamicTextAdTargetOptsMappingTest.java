package ru.yandex.direct.core.entity.dynamictextadtarget.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class DynamicTextAdTargetOptsMappingTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{DynamicTextAdTargetMapping.SUSPENDED_OPT, true},
                new Object[]{"", false},
                new Object[]{null, null}
        );
    }

    private String dbStatus;
    private Boolean modelStatus;

    public DynamicTextAdTargetOptsMappingTest(String dbStatus, Boolean modelStatus) {
        this.dbStatus = dbStatus;
        this.modelStatus = modelStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                DynamicTextAdTargetMapping.optsToDb(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                DynamicTextAdTargetMapping.optsFromDb(dbStatus),
                is(modelStatus));
    }
}
