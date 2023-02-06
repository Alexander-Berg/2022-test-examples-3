package ru.yandex.market.yt.partner_stats;

import java.time.LocalDate;
import java.util.Iterator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

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
import ru.yandex.market.core.order.OrderService;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mbi.yt.replicator.YtReplicator;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "ExportPartnerStatsPeriodYtService.before.csv")
public class ExportPartnerStatsPeriodYtServiceTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private OrderService orderService;

    private ExportPartnerStatsPeriodYtService exportService;

    private YtTemplate ytTemplate;

    private YtReplicator ytReplicator;

    @BeforeEach
    void setUp() {
        mockYt();
        exportService = new ExportPartnerStatsPeriodYtService(
                namedParameterJdbcTemplate,
                ytTemplate,
                ytReplicator,
                "test_path",
                orderService
        );
    }

    @Test
    void exportStatsToYt() {
        exportService.exportForPeriod(LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now());

        ArgumentCaptor<Iterator> captor = ArgumentCaptor.forClass(Iterator.class);

        verify(ytTemplate.getClusters()[0].getYt().tables()).write(
                any(),
                anyBoolean(),
                any(),
                any(YTableEntryType.class),
                captor.capture());

        ObjectNode jsonNodeInactive = (ObjectNode) captor.getValue().next();
        ObjectNode jsonNodeActive = (ObjectNode) captor.getValue().next();

        JsonTestUtil.assertEquals(
                jsonNodeActive.toString(),
                "{\n" +
                        "  \"partner_id\": 10,\n" +
                        "  \"business_id\": 100,\n" +
                        "  \"client_id\": 1001,\n" +
                        "  \"program\": \"DROPSHIP_BY_SELLER\",\n" +
                        "  \"program_status\": \"SUCCESS\",\n" +
                        "  \"inn\": \"10011001\",\n" +
                        "  \"income_contract_id\": 100101,\n" +
                        "  \"income_contract\": \"101101/21\",\n" +
                        "  \"outcome_contract_id\": 100101,\n" +
                        "  \"external_id\": \"ОФ-101010\",\n" +
                        "  \"org_name\": \"ReRoRu\",\n" +
                        "  \"kpp\": \"some_kpp\",\n" +
                        "  \"manager\": \"Онтото\",\n" +
                        "  \"nds_type\": \"ОСН\",\n" +
                        "  \"was_active\": true\n" +
                        "}"
        );

        JsonTestUtil.assertEquals(
                jsonNodeInactive.toString(),
                "{\n" +
                        "  \"partner_id\": 11,\n" +
                        "  \"business_id\": 101,\n" +
                        "  \"client_id\": 1002,\n" +
                        "  \"program\": \"DROPSHIP_BY_SELLER\",\n" +
                        "  \"program_status\": \"SUCCESS\",\n" +
                        "  \"inn\": \"10011002\",\n" +
                        "  \"income_contract_id\": 100102,\n" +
                        "  \"income_contract\": \"101101/21\",\n" +
                        "  \"outcome_contract_id\": 100102,\n" +
                        "  \"external_id\": \"ОФ-101010\",\n" +
                        "  \"org_name\": \"YaeMiko\",\n" +
                        "  \"kpp\": \"some_kpp2\",\n" +
                        "  \"manager\": \"Онтота\",\n" +
                        "  \"nds_type\": \"ОСН\",\n" +
                        "  \"was_active\": false\n" +
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
