package ru.yandex.market.billing.payment.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import org.springframework.context.annotation.Description;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.common.GUID;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.transactions.Transaction;
import ru.yandex.inside.yt.kosher.transactions.YtTransactions;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.payment.dao.PayoutDao;
import ru.yandex.market.billing.payment.model.YtPayoutDto;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.EntityType;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PayoutProductType;
import ru.yandex.market.core.payment.PaysysTypeCc;
import ru.yandex.market.core.payment.TransactionType;
import ru.yandex.market.mbi.util.MbiAsserts;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasIntValue;
import static ru.yandex.market.yt.matchers.JsonNodeMatchers.hasStrValue;

@ExtendWith(MockitoExtension.class)
class PayoutYtServiceTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 8, 1);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final GUID TR_GUID = new GUID(33, 55);

    @Mock
    private Yt yt;

    @Mock
    private Cypress cypress;

    @Value("${market.billing.payout.export.yt.path}")
    private String ytPath;

    @Mock
    private YtTables ytTables;

    @Mock
    private YtTransactions ytTransactions;

    @Mock
    private Transaction transaction;

    @Captor
    private ArgumentCaptor<Iterator<JsonNode>> nodesCaptor;

    @Autowired
    private PayoutDao payoutDao;

    private PayoutYtService payoutYtService;

    @BeforeEach
    void init() {
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

        payoutYtService = new PayoutYtService(yt, ytPath, payoutDao);
    }

    @Description("Экспорт обработанных выплат в Ыть")
    @Test
    @DbUnitDataSet(
            before = "PayoutYtServiceTest.testExportPayoutsToYt.before.csv"
    )
    void testStorePayoutsToYt() {
        //готовим данные
        YPath yPath = YPath.simple(ytPath).child(TEST_DATE.format(DATE_TIME_FORMATTER));

        when(cypress.exists(yPath))
                .thenReturn(true);
        payoutYtService.process(TEST_DATE);

        //проверяем
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

        List<JsonNode> payoutRecords = ImmutableList.copyOf(nodesCaptor.getValue());
        //проверяем значения в Ыть таблице
        assertThat(payoutRecords, Matchers.contains(
                allOf(
                        hasIntValue("payout_id", 11),
                        hasIntValue("entity_id", 2),
                        hasIntValue("checkouter_id", 13),
                        hasIntValue("order_id", 5),
                        hasIntValue("partner_id", 6),
                        hasIntValue("payout_group_id", 2),
                        hasIntValue("paysys_partner_id", 1),
                        hasIntValue("org_id", 64554),
                        hasIntValue("amount", 3000),
                        hasStrValue("entity_type", "item"),
                        hasStrValue("transaction_type", "payment"),
                        hasStrValue("product_type", "partner_payment"),
                        hasStrValue("paysys_type", "acc_sberbank"),
                        hasStrValue("trantime", "2021-08-01T15:00:00+03:00"),
                        hasStrValue("created_at", "2021-08-01T16:00:00+03:00"),
                        hasStrValue("currency", "RUB")
                )
        ));

    }


    @Description("Упасть при наличии необработанных выплат")
    @Test
    @DbUnitDataSet(
            before = "PayoutYtServiceTest.testFailExportPayoutsToYt.before.csv"
    )
    void testFailStorePayoutsToYt() {
        //готовим данные
        YPath yPath = YPath.simple(ytPath).child(TEST_DATE.format(DATE_TIME_FORMATTER));

        when(cypress.exists(yPath))
                .thenReturn(true);
        RuntimeException e = assertThrows(RuntimeException.class, () -> payoutYtService.process(TEST_DATE));
//        проверяем
        assertEquals(e.getMessage(), 1 + " unprocessed payouts found exception on " + TEST_DATE);
    }

    @Test
    @DisplayName("Сериализация в JSON записи")
    void testYtPayoutDtoToJsonNodes() {
        String expected = "" +
                "[" +
                "{" +
                "\"payout_id\":11," +
                "\"entity_id\":2," +
                "\"entity_type\":\"item\"," +
                "\"checkouter_id\":13," +
                "\"transaction_type\":\"payment\"," +
                "\"product_type\":\"partner_payment\"," +
                "\"paysys_type\":\"acc_sberbank\"," +
                "\"order_id\":5," +
                "\"partner_id\":6," +
                "\"trantime\":\"2021-08-01T06:00:00+03:00\"," +
                "\"amount\":3000," +
                "\"payout_group_id\":2," +
                "\"paysys_partner_id\":1," +
                "\"currency\":\"RUB\"," +
                "\"created_at\":\"2021-08-01T03:00:00+03:00\"," +
                "\"org_id\":64554" +
                "}" +
                "]";

        List<YtPayoutDto> ytPayoutDtos = List.of(
                YtPayoutDto.builder()
                        .setOrderId(5L)
                        .setPayoutId(11L)
                        .setEntityId(2L)
                        .setPartnerId(6L)
                        .setCheckouterId(13L)
                        .setPayoutGroupId(2L)
                        .setPaysysPartnerId(1L)
                        .setAmount(3000L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setTrantime(LocalDateTime.of(2021, 8, 1, 3, 0).atZone(ZoneId.of("UTC")).toInstant())
                        .setCreatedAt(LocalDateTime.of(2021, 8, 1, 0, 0).atZone(ZoneId.of("UTC")).toInstant())
                        .setEntityType(EntityType.ITEM)
                        .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                        .setPayoutProductType(PayoutProductType.PARTNER_PAYMENT)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .build()
        );


        List<JsonNode> jsonNodes = payoutYtService.toJsonNodes(ytPayoutDtos);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }
}
