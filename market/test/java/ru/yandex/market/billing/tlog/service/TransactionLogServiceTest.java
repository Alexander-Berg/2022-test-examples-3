package ru.yandex.market.billing.tlog.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.model.tlog.TransactionLogItem;
import ru.yandex.market.billing.tlog.dao.PartnerMarketingTransactionLogDao;
import ru.yandex.market.billing.tlog.dao.TransactionLogDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

public class TransactionLogServiceTest extends FunctionalTest {

    @Autowired
    private TransactionLogDao revenueTransactionLogDao;

    @Autowired
    private TransactionLogService<TransactionLogItem> revenuesTransactionLogService;

    @Autowired
    private PartnerMarketingTransactionLogDao partnerMarketingTransactionLogDao;

    @Autowired
    private TransactionLogService<TransactionLogItem> partnerMarketingTransactionLogService;

    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testToJsonNodesWithPayloadColumn.before.csv"
    )
    void testToJsonNodesWithPayloadColumn() {
        ZonedDateTime eventTime = ZonedDateTime.of(LocalDateTime.parse("2019-01-29T00:00:00"), ZoneId.systemDefault());
        ZonedDateTime transactionTime =
                ZonedDateTime.of(LocalDateTime.parse("2019-01-29T02:00:00"), ZoneId.systemDefault());
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 2," +
                "    \"event_time\": \"" + DATE_TIME_FORMATTER.format(eventTime) + "\"," +
                "    \"transaction_time\": \"" + DATE_TIME_FORMATTER.format(transactionTime) + "\"," +
                "    \"service_id\": 612," +
                "    \"partner_id\": 22," +
                "    \"client_id\": \"45\"," +
                "    \"product\": \"ff_storage_billing\"," +
                "    \"amount\": \"-24.56\"," +
                "    \"currency\": \"RUB\"," +
                "    \"aggregation_sign\": 1," +
                "    \"key\": \"{\\\"billing_timestamp\\\":\\\"2019-09-29 00:00:00.0\\\"," +
                "\\\"shop_sku\\\":\\\"sku_3\\\",\\\"supplier_id\\\":\\\"22\\\",\\\"supply_id\\\":\\\"15\\\"}\"," +
                "    \"previous_transaction_id\": \"1\"," +
                "    \"ignore_in_balance\": false," +
                "    \"nds\": 1," +
                "    \"payload\": \"{\\\"end_datetime\\\":\\\"2020-03-31T15:30:00.000000+10:00\\\"," +
                "\\\"start_datetime\\\":\\\"2020-03-01T15:30:00.000000+10:00\\\",\\\"type\\\":\\\"type2\\\"}\"," +
                "    \"org_id\": null" +
                "  }" +
                "]";

        List<TransactionLogItem> transactionLogItems = revenueTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = revenuesTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testToJsonNodesWithoutPayloadColumn.before.csv"
    )
    void testToJsonNodesWithoutPayloadColumn() {
        ZonedDateTime eventTime = ZonedDateTime.of(LocalDateTime.parse("2019-01-29T00:00:00"), ZoneId.systemDefault());
        ZonedDateTime transactionTime =
                ZonedDateTime.of(LocalDateTime.parse("2019-01-29T02:00:00"), ZoneId.systemDefault());
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 2," +
                "    \"event_time\": \"" + DATE_TIME_FORMATTER.format(eventTime) + "\"," +
                "    \"transaction_time\": \"" + DATE_TIME_FORMATTER.format(transactionTime) + "\"," +
                "    \"service_id\": 612," +
                "    \"partner_id\": 22," +
                "    \"client_id\": \"45\"," +
                "    \"product\": \"ff_storage_billing\"," +
                "    \"amount\": \"-24.56\"," +
                "    \"currency\": \"RUB\"," +
                "    \"aggregation_sign\": 1," +
                "    \"key\": \"{\\\"billing_timestamp\\\":\\\"2019-09-29 00:00:00.0\\\"," +
                "\\\"shop_sku\\\":\\\"sku_3\\\",\\\"supplier_id\\\":\\\"22\\\",\\\"supply_id\\\":\\\"15\\\"}\"," +
                "    \"previous_transaction_id\": \"1\"," +
                "    \"ignore_in_balance\": false," +
                "    \"nds\": 1," +
                "    \"payload\": null," +
                "    \"org_id\": null" +
                "  }" +
                "]";

        List<TransactionLogItem> transactionLogItems = revenueTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = revenuesTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testPartnerMarketingTransactionLogItemToJsonNodesWithoutPayloadColumn" +
                    ".before.csv"
    )
    void testPartnerMarketingTransactionLogItemToJsonNodesWithoutPayloadColumn() {
        var expectedEventTime = OffsetDateTime.parse("2021-05-28T20:30:40+10:00");
        var expectedTransactionTime = OffsetDateTime.parse("2021-05-29T14:29:00+10:00");
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 1," +
                "    \"event_time\": \"" + DATE_TIME_FORMATTER.format(expectedEventTime) + "\"," +
                "    \"transaction_time\": \"" + DATE_TIME_FORMATTER.format(expectedTransactionTime) + "\"," +
                "    \"service_id\": 666," +
                "    \"partner_id\": 42," +
                "    \"client_id\": \"104\"," +
                "    \"product\": \"marketing_campaign_fixed_billing\"," +
                "    \"amount\": \"9.00\"," +
                "    \"currency\": \"RUB\"," +
                "    \"aggregation_sign\": 1," +
                "    \"key\": \"{}\"," +
                "    \"previous_transaction_id\": \"null\"," +
                "    \"ignore_in_balance\": false," +
                "    \"nds\": 1," +
                "    \"payload\": null," +
                "    \"org_id\": null" +
                "  }" +
                "]";

        List<TransactionLogItem> transactionLogItems =
                partnerMarketingTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = partnerMarketingTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }
}
