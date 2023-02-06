package ru.yandex.direct.ytcore.entity.statistics.repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jooq.Select;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.ytcomponents.service.ConversionsStatsDynContextProvider;
import ru.yandex.direct.ytcomponents.statistics.model.DateRange;
import ru.yandex.direct.ytwrapper.dynamic.context.YtDynamicContext;
import ru.yandex.yt.ytclient.tables.ColumnValueType;
import ru.yandex.yt.ytclient.tables.TableSchema;
import ru.yandex.yt.ytclient.wire.UnversionedRow;
import ru.yandex.yt.ytclient.wire.UnversionedRowset;
import ru.yandex.yt.ytclient.wire.UnversionedValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.yt.ytclient.tables.ColumnValueType.INT64;

public class ConversionStatisticsRepositoryTest {

    private static final TableSchema RESULT_SCHEMA = new TableSchema.Builder()
            .setUniqueKeys(false)
            .addKey("effectiveCampaignId", INT64)
            .addKey("UpdateTime", INT64)
            .addKey("GoalID", INT64)
            .addKey("AttributionType", INT64)
            .addKey("Cost", INT64)
            .addKey("CostCur", INT64)
            .addKey("CostTaxFree", INT64)
            .addKey("GoalsNum", INT64)
            .build();

    private ConversionStatisticsRepository conversionStatisticsRepository;
    private YtDynamicContext ytDynamicContext;

    @Before
    public void setUp() {
        UnversionedRowset emptyRowset = new UnversionedRowset(RESULT_SCHEMA, Collections.emptyList());

        ConversionsStatsDynContextProvider contextProvider = mock(ConversionsStatsDynContextProvider.class);
        ytDynamicContext = mock(YtDynamicContext.class);
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(emptyRowset);
        when(contextProvider.getContext()).thenReturn(ytDynamicContext);

        conversionStatisticsRepository = new ConversionStatisticsRepository(contextProvider);
    }

    @Test
    public void getConversionsStatisticsSuccessEmptyResponse() {
        var response = conversionStatisticsRepository.getConversionsStatistics(List.of(1L, 2L, 3L), Map.of(),
                getDateRange());
        assertThat(response).isEmpty();
    }

    @Test
    public void getConversionsStatisticsSuccessNegativeCosts() {
        var rowset = new UnversionedRowset(RESULT_SCHEMA, List.of(
                rowWithNegativeCosts()
        ));
        when(ytDynamicContext.executeSelect(any(Select.class))).thenReturn(rowset);

        var response = conversionStatisticsRepository.getConversionsStatistics(List.of(1L), Map.of(2L, 1L),
                getDateRange());
        assertThat(response).hasSize(1);

        var statPiece = response.get(0);

        assertThat(statPiece.getCostTaxFree()).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.DOWN));
        assertThat(statPiece.getCostTaxFree().scale()).isEqualTo(2);
    }

    private DateRange getDateRange() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(28);
        return new DateRange()
                .withFromInclusive(startDate)
                .withToInclusive(endDate);
    }

    private static UnversionedRow rowWithNegativeCosts() {
        return new UnversionedRow(List.of(
                longValue(0, 1L), // campaign id
                longValue(1, 123456L), // update time
                longValue(2, 123L), // goal id
                longValue(3, 1L), // attribution type
                longValue(4, -1000000L), // cost
                longValue(5, -1000000L), // cost cur
                longValue(6, -1000000L), // cost tax free
                longValue(7, 15L) // goals num
        ));
    }

    private static UnversionedValue longValue(Integer id, Long value) {
        return new UnversionedValue(id, ColumnValueType.INT64, false, value);
    }
}
