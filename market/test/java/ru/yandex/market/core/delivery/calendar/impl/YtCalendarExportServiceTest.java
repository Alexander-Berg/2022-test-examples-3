package ru.yandex.market.core.delivery.calendar.impl;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Iterator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.impl.ArrayListF;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.operations.YtOperations;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.calendar.DatePeriod;
import ru.yandex.market.core.delivery.calendar.HolidayCalendarGetService;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.replicator.YtReplicator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class YtCalendarExportServiceTest extends FunctionalTest {

    private YtCalendarExportService service;

    @Autowired
    private HolidayCalendarGetService deliveryCalendarService;
    @Autowired
    private EnvironmentService environmentService;

    private YtTemplate ytTemplate;

    private YtReplicator ytReplicator;

    @BeforeEach
    void setUp() {
        mockYt();
        environmentService.setValue(YtHolidayCalendarWriter.ENV_TABLE_LIFETIME_DAYS, "100");
        YtCalendarWriterFactory factory =
                new YtCalendarWriterFactory(ytTemplate, ytReplicator, "test_path", environmentService);
        service = new YtCalendarExportService(factory, deliveryCalendarService);
    }

    @Test
    @DbUnitDataSet(before = "YtCalendarExportServiceTest.before.csv")
    void export() throws IOException {
        LocalDate beginDate = LocalDate.of(2016, 11, 3);
        LocalDate endDate = LocalDate.of(2016, 12, 13);

        service.export(DatePeriod.of(beginDate, endDate));

        ArgumentCaptor<Iterator> captor = ArgumentCaptor.forClass(Iterator.class);

        verify(ytTemplate.getClusters()[0].getYt().tables()).write(
                any(),
                anyBoolean(),
                any(),
                any(YTableEntryType.class),
                captor.capture());

        ObjectNode jsonNode = (ObjectNode) captor.getValue().next();

        JsonTestUtil.assertEquals(
                jsonNode.toString(),
                "{\n" +
                        "  \"shop_id\": 101,\n" +
                        "  \"calendar_id\": 101,\n" +
                        "  \"lms_partner_id\": 505,\n" +
                        "  \"begin_date\": \"2016-11-03\",\n" +
                        "  \"end_date\": \"2016-12-13\",\n" +
                        "  \"holidays\": [\n" +
                        "  ]\n" +
                        "}"
        );
    }

    private void mockYt() {
        Yt yt = Mockito.mock(Yt.class);
        YtTables ytTables = mock(YtTables.class);
        Cypress cypress = mock(Cypress.class);
        YtOperations ytOperations = mock(YtOperations.class);

        //transactions
        YtTransactions transactions = mock(YtTransactions.class);
        Transaction transaction = mock(Transaction.class);
        when(transaction.getId()).thenReturn(new GUID(1L, 1L));
        when(yt.transactions()).thenReturn(transactions);
        when(transactions.startAndGet(any(), anyBoolean(), any())).thenReturn(transaction);

        when(cypress.list(any(YPath.class))).thenReturn(new ArrayListF<>());

        //operations
        when(yt.tables()).thenReturn(ytTables);
        when(yt.operations()).thenReturn(ytOperations);
        when(yt.cypress()).thenReturn(cypress);

        ytTemplate = new YtTemplate(new YtCluster("test", yt));

        ytReplicator = Mockito.mock(YtReplicator.class);
    }
}
