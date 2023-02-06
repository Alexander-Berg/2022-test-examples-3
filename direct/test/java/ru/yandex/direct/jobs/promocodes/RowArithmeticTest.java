package ru.yandex.direct.jobs.promocodes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;
import ru.yandex.inside.yt.kosher.cypress.YPath;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RowArithmeticTest {
    private static final Long LAST_ROW_INDEX_VALUE = 8900L;
    private static final int SERVICE_ID = 7;
    private static final YtTable TABLE =
            new YtTable("//home/antivir/prod/export/direct-promocodes/fraud_promo_redirects");

    SafeSearchTearOffPromocodesJob safeSearchTearOffPromocodesJob;
    YtOperator ytOperator;
    PpcProperty<Long> property;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void init() {
        YtProvider ytProvider = mock(YtProvider.class);
        ytOperator = mock(YtOperator.class);
        when(ytOperator.exists(any())).thenReturn(true);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);

        property = (PpcProperty<Long>) mock(PpcProperty.class);
        when(property.getOrDefault(anyLong())).thenReturn(LAST_ROW_INDEX_VALUE);

        safeSearchTearOffPromocodesJob = new SafeSearchTearOffPromocodesJob(SERVICE_ID, ytProvider, property, null,
                null, null, null);
    }

    @Test
    void noNewData_doNothing() {
        // last_index: 8900 → row_count: 8901
        when(ytOperator.readTableRowCount(any())).thenReturn(LAST_ROW_INDEX_VALUE + 1);
        safeSearchTearOffPromocodesJob.execute();
        verify(ytOperator, never()).readTableSnapshot(any(YPath.class), any(), any(), any(), anyInt());
        verify(property, never()).set(anyLong());
    }

    @Test
    void oneNewDataRow_readData() {
        // last_index: 8900, 1 new row → row_count: 8902
        when(ytOperator.readTableRowCount(any())).thenReturn(LAST_ROW_INDEX_VALUE + 2);
        safeSearchTearOffPromocodesJob.execute();
        verify(ytOperator).readTableSnapshot(any(YPath.class), any(), any(), any(), anyInt());
    }

    @Test
    void newData_readRows() {
        // last_index: 8900, 100 new rows → row_count: 9001 → lowerIndex: 8901, upperIndex: 9001
        when(ytOperator.readTableRowCount(any())).thenReturn(LAST_ROW_INDEX_VALUE + 101);
        safeSearchTearOffPromocodesJob.execute();
        FraudPromoRedirectsTableRow fraudPromoRedirectsTableRow = new FraudPromoRedirectsTableRow();
        verify(ytOperator).readTableSnapshot(
                eq(TABLE.ypath(fraudPromoRedirectsTableRow.getFields())
                        .withRange(LAST_ROW_INDEX_VALUE + 1, LAST_ROW_INDEX_VALUE + 101)),
                any(), any(), any(), anyInt());
    }

    @Test
    void newData_updateProperty() {
        when(ytOperator.readTableRowCount(any())).thenReturn(LAST_ROW_INDEX_VALUE + 101);
        when(ytOperator.readTableSnapshot(any(YPath.class), any(), any(), any(), anyInt())).thenReturn(100L);
        safeSearchTearOffPromocodesJob.execute();
        verify(property).set(eq(LAST_ROW_INDEX_VALUE + 100));
    }
}
