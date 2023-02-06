package ru.yandex.direct.ytcomponents.statistics.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytcomponents.model.CampaignDeal;
import ru.yandex.direct.ytcomponents.model.DealStatsResponse;
import ru.yandex.direct.ytcomponents.service.PhraseStatsDynContextProvider;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.ytwrapper.dynamic.dsl.YtQueryUtil.DECIMAL_MULT;

public class CampaignStatRepositoryTest {

    private static final TableSchema RESULT_SCHEMA = new TableSchema.Builder()
            .setUniqueKeys(false)
            .addKey("DealId", ColumnValueType.INT64)
            .addKey("Shows", ColumnValueType.INT64)
            .addKey("Clicks", ColumnValueType.INT64)
            .addKey("Spent", ColumnValueType.INT64)
            .build();

    private CampaignStatRepository campaignStatRepository;
    private YtDynamicContext ytDynamicContext;

    @Before
    public void setUp() {

        UnversionedRowset emptyRowset = new UnversionedRowset(RESULT_SCHEMA, Collections.emptyList());

        PhraseStatsDynContextProvider ytDynContextProvider = mock(PhraseStatsDynContextProvider.class);
        ytDynamicContext = mock(YtDynamicContext.class);
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(emptyRowset);
        when(ytDynContextProvider.getContext()).thenReturn(ytDynamicContext);
        campaignStatRepository = new CampaignStatRepository(ytDynContextProvider);
    }

    @Test
    public void success_onEmptyResponse() {
        Collection<CampaignDeal> requests = singletonList(new CampaignDeal()
                .withCampaignId(1L)
                .withDealId(2L)
        );
        Map<Long, DealStatsResponse> statistics = campaignStatRepository.getDealsStatistics(requests);
        assertThat(statistics).isEmpty();
    }

    @Test
    public void success_onNotEmptyResponse() {
        UnversionedRowset rowset = new UnversionedRowset(RESULT_SCHEMA, singletonList(
                row(1L, 2L, 3L, 4L)
        ));
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowset);

        Collection<CampaignDeal> requests = singletonList(new CampaignDeal()
                .withCampaignId(2L)
                .withDealId(1L)
        );

        Map<Long, DealStatsResponse> statistics = campaignStatRepository.getDealsStatistics(requests);
        assertThat(statistics).hasSize(1);
        assertThat(statistics.get(1L)).isEqualToComparingFieldByField(
                new DealStatsResponse()
                        .withDealId(1L)
                        .withShows(2L)
                        .withClicks(3L)
                        .withSpent(BigDecimal.valueOf(4L).divide(DECIMAL_MULT)));
    }

    @SuppressWarnings("SameParameterValue")
    private static UnversionedRow row(Long dealId, Long shows, Long clicks, Long spent) {
        return new UnversionedRow(asList(longValue(0, dealId),
                longValue(1, shows),
                longValue(2, clicks),
                longValue(3, spent)));
    }

    private static UnversionedValue longValue(Integer id, Long value) {
        return new UnversionedValue(id, ColumnValueType.INT64, false, value);
    }
}
