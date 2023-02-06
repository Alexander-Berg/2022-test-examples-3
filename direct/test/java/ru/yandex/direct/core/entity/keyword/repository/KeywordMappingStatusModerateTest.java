package ru.yandex.direct.core.entity.keyword.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.keyword.model.StatusModerate;
import ru.yandex.direct.dbschema.ppc.enums.BidsStatusmoderate;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordMappingStatusModerateTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{StatusModerate.NO, BidsStatusmoderate.No},
                new Object[]{StatusModerate.YES, BidsStatusmoderate.Yes},
                new Object[]{StatusModerate.NEW, BidsStatusmoderate.New},
                new Object[]{null, null}
        );
    }

    private StatusModerate modelValue;
    private BidsStatusmoderate dbValue;

    public KeywordMappingStatusModerateTest(StatusModerate modelValue, BidsStatusmoderate dbStatus) {
        this.modelValue = modelValue;
        this.dbValue = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                StatusModerate.toSource(modelValue),
                is(dbValue));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                StatusModerate.fromSource(dbValue),
                is(modelValue));
    }
}
