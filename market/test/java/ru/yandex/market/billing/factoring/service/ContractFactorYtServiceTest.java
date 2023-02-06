package ru.yandex.market.billing.factoring.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.factoring.dao.ClientFactorDao;
import ru.yandex.market.billing.factoring.model.ContractFactor;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.PaymentOrderFactoring;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasIntValue;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasStrValue;

@ExtendWith(MockitoExtension.class)
public class ContractFactorYtServiceTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 8, 1);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final GUID TR_GUID = new GUID(33, 55);

    @Autowired
    private ClientFactorDao clientFactorDao;

    @Autowired
    private EnvironmentService environmentService;

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Value("${market.billing.contract-factor.export.yt.path}")
    private String ytPath;

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> nodesCaptor;

    private ContractFactorYtService contractFactorYtService;

    @BeforeEach
    void beforeEach() {
        when(yt.cypress())
                .thenReturn(cypress);

        when(yt.tables())
                .thenReturn(ytTables);

        when(yt.transactions())
                .thenReturn(ytTransactions);

        when(ytTransactions.startAndGet(any()))
                .thenReturn(transaction);

        when(ytTransactions.startAndGet(any(), anyBoolean(), any()))
                .thenReturn(transaction);

        when(transaction.getId())
                .thenReturn(TR_GUID);

        contractFactorYtService = new ContractFactorYtService(clientFactorDao, environmentService, yt, ytPath);
    }

    @Test
    @DisplayName("Сериализация в JSON записи")
    void testContractFactorToJsonNodes() {
        String expected = "" +
                "[" +
                "  {" +
                "    \"contract_id\": 1," +
                "    \"is_active\": true," +
                "    \"factor\": raiffeisen" +
                "  }," +
                "  {" +
                "    \"contract_id\": 2," +
                "    \"is_active\": false," +
                "    \"factor\": alfa" +
                "  }," +
                "  {" +
                "    \"contract_id\": 2," +
                "    \"is_active\": true," +
                "    \"factor\": sber" +
                "  }," +
                "  {" +
                "    \"contract_id\": 3," +
                "    \"is_active\": true," +
                "    \"factor\": market" +
                "  }" +
                "]";

        List<ContractFactor> contractFactors = List.of(
                new ContractFactor(1L, true, PaymentOrderFactoring.RAIFFEISEN),
                new ContractFactor(2L, false, PaymentOrderFactoring.ALFA),
                new ContractFactor(2L, true, PaymentOrderFactoring.SBER),
                new ContractFactor(3L, true, PaymentOrderFactoring.MARKET)
        );

        List<JsonNode> jsonNodes = contractFactorYtService.toJsonNodes(contractFactors);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }

    @DisplayName("Экспорт признака факторинга в YT")
    @Test
    @DbUnitDataSet(before = "ContractFactorYtServiceTest.testStoreContractFactors.csv")
    void testStoreContractFactors() {
        //готовим данные
        YPath yPath = YPath.simple(ytPath).child(TEST_DATE.format(DATE_TIME_FORMATTER));

        when(cypress.exists(yPath))
                .thenReturn(true);

        // вызов
        contractFactorYtService.process(TEST_DATE);

        // проверяем
        Mockito.verify(cypress)
                .exists(eq(yPath));
        Mockito.verify(cypress)
                .remove(eq(Optional.of(TR_GUID)), eq(true), eq(yPath));

        Mockito.verify(ytTables)
                .write(
                        eq(Optional.of(TR_GUID)),
                        anyBoolean(),
                        eq(yPath),
                        any(YTableEntryType.class),
                        nodesCaptor.capture()
                );

        List<JsonNode> agencyRecords = ImmutableList.copyOf(nodesCaptor.getValue());

        assertEquals(4, agencyRecords.size());

        assertThat(agencyRecords,
                Matchers.containsInAnyOrder(
                        allOf(
                                hasIntValue("contract_id", 1),
                                hasStrValue("factor", "raiffeisen")
                        ),
                        allOf(
                                hasIntValue("contract_id", 1),
                                hasStrValue("factor", "alfa")
                        ),
                        allOf(
                                hasIntValue("contract_id", 1),
                                hasStrValue("factor", "sber")
                        ),
                        allOf(
                                hasIntValue("contract_id", 2),
                                hasStrValue("factor", "market")
                        )
                )
        );
    }
}
