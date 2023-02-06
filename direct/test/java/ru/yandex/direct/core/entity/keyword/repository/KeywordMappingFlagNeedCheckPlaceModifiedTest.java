package ru.yandex.direct.core.entity.keyword.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.dbschema.ppc.enums.BidsWarn;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordMappingFlagNeedCheckPlaceModifiedTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{true, BidsWarn.Yes},
                new Object[]{false, BidsWarn.No},
                new Object[]{null, null}
        );
    }

    private Boolean modelStatus;
    private BidsWarn dbStatus;

    public KeywordMappingFlagNeedCheckPlaceModifiedTest(Boolean modelStatus, BidsWarn dbStatus) {
        this.modelStatus = modelStatus;
        this.dbStatus = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                KeywordMapping.needCheckPlaceModifiedToDbFormat(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                KeywordMapping.needCheckPlaceModifiedFromDbFormat(dbStatus),
                is(modelStatus));
    }
}
