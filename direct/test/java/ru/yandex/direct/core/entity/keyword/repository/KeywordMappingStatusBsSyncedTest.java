package ru.yandex.direct.core.entity.keyword.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.dbschema.ppc.enums.BidsStatusbssynced;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class KeywordMappingStatusBsSyncedTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{StatusBsSynced.NO, BidsStatusbssynced.No},
                new Object[]{StatusBsSynced.YES, BidsStatusbssynced.Yes},
                new Object[]{StatusBsSynced.SENDING, BidsStatusbssynced.Sending},
                new Object[]{null, null}
        );
    }

    private StatusBsSynced modelStatus;
    private BidsStatusbssynced dbStatus;

    public KeywordMappingStatusBsSyncedTest(StatusBsSynced modelStatus, BidsStatusbssynced dbStatus) {
        this.modelStatus = modelStatus;
        this.dbStatus = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                KeywordMapping.statusBsSyncedToDbFormat(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                KeywordMapping.statusBsSyncedFromDbFormat(dbStatus),
                is(modelStatus));
    }
}
