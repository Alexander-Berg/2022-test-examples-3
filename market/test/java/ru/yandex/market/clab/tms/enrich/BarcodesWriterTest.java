package ru.yandex.market.clab.tms.enrich;

import org.jooq.SQLDialect;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clab.common.service.barcode.GoodBarcodes;
import ru.yandex.market.clab.common.service.good.GoodErrorService;
import ru.yandex.market.clab.db.jooq.generated.enums.GoodErrorType;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 25.01.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class BarcodesWriterTest {

    private static final long GOOD_ID_1 = 21610992;
    private static final long GOOD_ID_2 = 39292723;

    private BarcodesWriter writer;

    @Mock
    private GoodErrorService goodErrorService;

    @Before
    public void before() {
        MockDataProvider dataProvider = ctx -> new MockResult[]{
            new MockResult()
        };
        MockConnection mockConnection = new MockConnection(dataProvider);
        DefaultDSLContext create = new DefaultDSLContext(mockConnection, SQLDialect.POSTGRES);
        writer = new BarcodesWriter(goodErrorService, create);
    }

    @Test
    public void resolvedCalled() {
        GoodBarcodes barcodes = new GoodBarcodes(GOOD_ID_1, "one-barcode", "two-barcode");

        writer.write(Collections.singletonList(barcodes));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> goodIdsCapture = ArgumentCaptor.forClass(List.class);
        verify(goodErrorService).markResolved(goodIdsCapture.capture(), eq(GoodErrorType.NO_SS_BARCODES));
        assertThat(goodIdsCapture.getValue()).containsExactly(GOOD_ID_1);
    }

    @Test
    public void failedCalled() {
        GoodBarcodes barcodes = new GoodBarcodes(GOOD_ID_1);

        writer.write(Collections.singletonList(barcodes));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> goodIdsCapture = ArgumentCaptor.forClass(List.class);
        verify(goodErrorService).markError(goodIdsCapture.capture(), eq(GoodErrorType.NO_SS_BARCODES));
        assertThat(goodIdsCapture.getValue()).containsExactly(GOOD_ID_1);
    }
}
