package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.dbschema.ppc.enums.CampaignsArchived;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@RunWith(Parameterized.class)
public class CampaignMappingArchivedTest {
    @Parameterized.Parameters(name = "model format: {0}, db format: {1}")
    public static Collection<Object[]> params() {
        return asList(
                new Object[]{true, CampaignsArchived.Yes},
                new Object[]{false, CampaignsArchived.No},
                new Object[]{null, null}
        );
    }

    private Boolean modelStatus;
    private CampaignsArchived dbStatus;

    public CampaignMappingArchivedTest(Boolean modelStatus, CampaignsArchived dbStatus) {
        this.modelStatus = modelStatus;
        this.dbStatus = dbStatus;
    }

    @Test
    public void testToDbFormat() {
        assertThat("Конвертация типа в формат базы должна быть однозначной",
                CampaignMappings.archivedToDb(modelStatus),
                is(dbStatus));
    }

    @Test
    public void testFromDbFormat() {
        assertThat("Конвертация типа в формат модели должна быть однозначной",
                CampaignMappings.archivedFromDb(dbStatus),
                is(modelStatus));
    }
}
