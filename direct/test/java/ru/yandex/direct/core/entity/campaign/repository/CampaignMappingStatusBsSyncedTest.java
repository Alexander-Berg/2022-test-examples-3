package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.StatusBsSynced;
import ru.yandex.direct.dbschema.ppc.enums.CampaignsStatusbssynced;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class CampaignMappingStatusBsSyncedTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{StatusBsSynced.NO, CampaignsStatusbssynced.No},
                new Object[]{StatusBsSynced.YES, CampaignsStatusbssynced.Yes},
                new Object[]{StatusBsSynced.SENDING, CampaignsStatusbssynced.Sending},
                new Object[]{null, null}
        );
    }

    private StatusBsSynced modelStatus;
    private CampaignsStatusbssynced dbStatus;

    public CampaignMappingStatusBsSyncedTest(StatusBsSynced modelStatus, CampaignsStatusbssynced dbStatus) {
        this.modelStatus = modelStatus;
        this.dbStatus = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.statusBsSyncedToDb(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.statusBsSyncedFromDb(dbStatus),
                is(modelStatus));
    }
}
