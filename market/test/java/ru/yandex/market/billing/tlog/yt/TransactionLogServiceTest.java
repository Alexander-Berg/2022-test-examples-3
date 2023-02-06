package ru.yandex.market.billing.tlog.yt;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlog.model.ExpensesTransactionLogItem;
import ru.yandex.market.billing.tlog.model.TransactionLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.util.MbiAsserts;

public class TransactionLogServiceTest extends FunctionalTest {

    @Autowired
    private TransactionLogDao revenueTransactionLogDao;

    @Autowired
    private ExpensesTransactionLogDao expensesTransactionLogDao;

    @Autowired
    private PartnerMarketingTransactionLogDao partnerMarketingTransactionLogDao;

    @Autowired
    private TransactionLogService<TransactionLogItem> revenuesTransactionLogService;

    @Autowired
    private TransactionLogService<ExpensesTransactionLogItem> expensesTransactionLogService;

    @Autowired
    private TransactionLogService<TransactionLogItem> partnerMarketingTransactionLogService;

    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testToJsonNodesWithPayloadColumn.before.csv"
    )
    void testToJsonNodesWithPayloadColumn() {
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 2," +
                "    \"event_time\": \"2019-01-29T00:00:00.000000+10:00\"," +
                "    \"transaction_time\": \"2019-01-29T02:00:00.000000+10:00\"," +
                "    \"service_id\": 612," +
                "    \"partner_id\": 22," +
                "    \"client_id\": \"45\"," +
                "    \"product\": \"ff_storage_billing\"," +
                "    \"amount\": \"-24.56\"," +
                "    \"currency\": \"RUB\"," +
                "    \"aggregation_sign\": 1," +
                "    \"key\": \"{\\\"billing_timestamp\\\":\\\"2019-09-29 00:00:00.0\\\",\\\"shop_sku\\\":\\\"sku_3\\\",\\\"supplier_id\\\":\\\"22\\\",\\\"supply_id\\\":\\\"15\\\"}\"," +
                "    \"previous_transaction_id\": \"1\"," +
                "    \"ignore_in_balance\": false," +
                "    \"nds\": 1," +
                "    \"payload\": \"{\\\"end_datetime\\\":\\\"2020-03-31T15:30:00.000000+10:00\\\",\\\"start_datetime\\\":\\\"2020-03-01T15:30:00.000000+10:00\\\",\\\"type\\\":\\\"type2\\\"}\"" +
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
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 2," +
                "    \"event_time\": \"2019-01-29T00:00:00.000000+10:00\"," +
                "    \"transaction_time\": \"2019-01-29T02:00:00.000000+10:00\"," +
                "    \"service_id\": 612," +
                "    \"partner_id\": 22," +
                "    \"client_id\": \"45\"," +
                "    \"product\": \"ff_storage_billing\"," +
                "    \"amount\": \"-24.56\"," +
                "    \"currency\": \"RUB\"," +
                "    \"aggregation_sign\": 1," +
                "    \"key\": \"{\\\"billing_timestamp\\\":\\\"2019-09-29 00:00:00.0\\\",\\\"shop_sku\\\":\\\"sku_3\\\",\\\"supplier_id\\\":\\\"22\\\",\\\"supply_id\\\":\\\"15\\\"}\"," +
                "    \"previous_transaction_id\": \"1\"," +
                "    \"ignore_in_balance\": false," +
                "    \"nds\": 1," +
                "    \"payload\": null" +
                "  }" +
                "]";

        List<TransactionLogItem> transactionLogItems = revenueTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = revenuesTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }
    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testExpensesTransactionLogItemToJsonNodesWithoutPayloadColumn.before.csv"
    )
    void testExpensesTransactionLogItemToJsonNodesWithoutPayloadColumn() {
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 1," +
                "    \"event_time\": \"2020-12-26T18:30:00.000000+10:00\"," +
                "    \"transaction_time\": \"2020-12-28T18:30:00.000000+10:00\"," +
                "    \"due\": \"2020-05-01T18:30:00.000000+10:00\"," +
                "    \"transaction_type\": \"payment\"," +
                "    \"orig_transaction_id\": null," +
                "    \"service_transaction_id\": \"service_transaction_id_4\"," +
                "    \"service_id\": 104," +
                "    \"client_id\": \"1004\"," +
                "    \"clid\": null," +
                "    \"product\": \"product_name_4\"," +
                "    \"amount\": \"9.00\"," +
                "    \"currency\": \"RUB\"," +
                "    \"detailed_product\": null," +
                 "   \"ignore_in_balance\": false," +
                "    \"payload\": null" +
                "  }" +
                "]";

        List<ExpensesTransactionLogItem> transactionLogItems =
                expensesTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = expensesTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogServiceTest.testPartnerMarketingTransactionLogItemToJsonNodesWithoutPayloadColumn.before.csv"
    )
    void testPartnerMarketingTransactionLogItemToJsonNodesWithoutPayloadColumn() {
        final String expected = "" +
                "[" +
                "  {" +
                "    \"transaction_id\": 1," +
                "    \"event_time\": \"2021-05-28T20:30:40.000000+10:00\"," +
                "    \"transaction_time\": \"2021-05-29T14:29:00.000000+10:00\"," +
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
                "    \"payload\": null" +
                "  }" +
                "]";

        List<TransactionLogItem> transactionLogItems =
                partnerMarketingTransactionLogDao.getTransactionLogItems(0, 500);
        List<JsonNode> jsonNodes = partnerMarketingTransactionLogService.toJsonNodes(transactionLogItems);

        MbiAsserts.assertJsonEquals(expected, jsonNodes.toString());
    }
}
