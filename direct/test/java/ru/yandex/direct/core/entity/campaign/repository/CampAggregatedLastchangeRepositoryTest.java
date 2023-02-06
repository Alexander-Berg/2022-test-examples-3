package ru.yandex.direct.core.entity.campaign.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.jooq.DSLContext;
import org.jooq.InsertValuesStep2;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbschema.ppc.tables.records.CampAggregatedLastchangeRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_AGGREGATED_LASTCHANGE;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampAggregatedLastchangeRepositoryTest {
    @Autowired
    CampAggregatedLastchangeRepository campAggregatedLastchangeRepository;

    @Autowired
    DslContextProvider dslContextProvider;

    @Test
    public void testUpdateCampAggregatedLastchange() {
        long cidForIncreaseLastChange = 1L;
        long cidForReduceLastChange = 2L;
        long cidForNoChangeLastChange = 3L;

        DSLContext dslContext = dslContextProvider.ppc(1);
        LocalDateTime lastChange = LocalDateTime.of(2000, 1, 1, 0, 0)
                .truncatedTo(ChronoUnit.SECONDS);
        InsertValuesStep2<CampAggregatedLastchangeRecord, Long, LocalDateTime> initialQuery =
                dslContext.insertInto(CAMP_AGGREGATED_LASTCHANGE)
                        .columns(CAMP_AGGREGATED_LASTCHANGE.CID, CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE);


        initialQuery = initialQuery.values(cidForIncreaseLastChange, lastChange);
        initialQuery = initialQuery.values(cidForReduceLastChange, lastChange);
        initialQuery = initialQuery.values(cidForNoChangeLastChange, lastChange);

        initialQuery.execute();


        Map<Long, LocalDateTime> lastChangesByCids = ImmutableMap.of(
                cidForIncreaseLastChange, lastChange.plusDays(cidForIncreaseLastChange),
                cidForReduceLastChange, lastChange.minusDays(cidForReduceLastChange),
                cidForNoChangeLastChange, lastChange
        );

        campAggregatedLastchangeRepository.updateCampAggregatedLastchange(1, lastChangesByCids);

        LocalDateTime got = dslContext.select(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE)
                .from(CAMP_AGGREGATED_LASTCHANGE)
                .where(CAMP_AGGREGATED_LASTCHANGE.CID.eq(cidForIncreaseLastChange))
                .fetchOne(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE);
        assertThat(got).isEqualTo(lastChange.plusDays(cidForIncreaseLastChange));

        got = dslContext.select(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE)
                .from(CAMP_AGGREGATED_LASTCHANGE)
                .where(CAMP_AGGREGATED_LASTCHANGE.CID.eq(cidForReduceLastChange))
                .fetchOne(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE);
        assertThat(got).isEqualTo(lastChange);

        got = dslContext.select(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE)
                .from(CAMP_AGGREGATED_LASTCHANGE)
                .where(CAMP_AGGREGATED_LASTCHANGE.CID.eq(cidForNoChangeLastChange))
                .fetchOne(CAMP_AGGREGATED_LASTCHANGE.LAST_CHANGE);
        assertThat(got).isEqualTo(lastChange);
    }

    // TODO add test for getCampaignIdsWhereChildrenWereChanged(..)
}
