package ru.yandex.market.pers.pay.tms;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PayerBalance;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBuilder;
import ru.yandex.market.pers.pay.model.PersPaymentFilter;
import ru.yandex.market.pers.pay.service.PaymentProcessor;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.pers.pay.tms.state.PaymentBillingStatesExecutor;
import ru.yandex.market.pers.pay.tms.util.PersPayUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 31.03.2021
 */
public class VendorJobTest extends PersPayTest {
    public static final long USER_ID = 245242;
    public static final long MODEL_ID = 73534;

    @Autowired
    private TmsPaymentService paymentService;
    @Autowired
    private JdbcTemplate yqlJdbcTemplate;
    @Autowired
    private YtHelper ytHelper;
    @Autowired
    private VendorJob job;

    @BeforeEach
    public void initBeforeTests() {
        // assume this is current time for backend
        // saved time shifts in end_time would be shifted with now() like passed time to this date
        job.changeCurrentTimeSupplier(() -> Instant.parse("2000-04-06T16:20:00Z"));
    }

    @Test
    public void testUnbalancedCleanup() {
        // create
        List<PersPaymentBuilder> paymentsTemplate = List.of(
            MockUtils.testPay(MODEL_ID, USER_ID + 1).payer(PersPayerType.VENDOR, "123"),
            MockUtils.testPay(MODEL_ID, USER_ID + 2).payer(PersPayerType.VENDOR, "123"),
            MockUtils.testPay(MODEL_ID, USER_ID + 3).payer(PersPayerType.VENDOR, "123"),
            MockUtils.testPay(MODEL_ID, USER_ID + 4) //non-vendor, should remain
        );

        paymentService.savePaymentsBulk(paymentsTemplate);

        List<String> unbalancedPays = jdbcTemplate
            .queryForList("select pay_key from pay.unbalanced_pays", String.class);
        assertEquals(4, unbalancedPays.size());
        assertEquals(Set.of(
            paymentsTemplate.get(0).getPayKey(),
            paymentsTemplate.get(1).getPayKey(),
            paymentsTemplate.get(2).getPayKey(),
            paymentsTemplate.get(3).getPayKey()
        ), new HashSet<>(unbalancedPays));

        when(yqlJdbcTemplate.queryForList(any(String.class), eq(String.class))).thenReturn(
            paymentService.getPayments(new PersPaymentFilter()).stream()
                .filter(x -> x.getUser().getIdLong() != USER_ID + 2)
                .map(x -> PaymentBillingStatesExecutor.PERS_PAY_BILLING_REF_PREFIX + x.getId())
                .collect(Collectors.toList())
        );

        // run job
        job.processUnbalancedCsBillingPays();

        unbalancedPays = jdbcTemplate.queryForList("select pay_key from pay.unbalanced_pays", String.class);
        assertEquals(2, unbalancedPays.size());
        assertEquals(Set.of(
            paymentsTemplate.get(1).getPayKey(),
            paymentsTemplate.get(3).getPayKey()
        ), new HashSet<>(unbalancedPays));
    }

    @Test
    public void testBalanceUpload() throws IOException {
        ArgumentCaptor<Function> mappingCaptor = ArgumentCaptor.forClass(Function.class);
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        job.loadBillingBalances();

        verify(ytHelper.getHahnClient(), times(1)).consumeTableBatched(
            any(YPath.class), anyInt(),
            mappingCaptor.capture(),
            consumerCaptor.capture());

        Function<JsonNode, PayerBalance> mapper = mappingCaptor.getValue();
        Consumer<List<PayerBalance>> consumer = consumerCaptor.getValue();

        String json = "[{\"DATASOURCE_ID\":23429,\"VENDOR_ID\":3345,\"ACTUAL_BALANCE\":10000,\"LAST_DATE\":\"2008-03-31 20:42:40\"},\n" +
            "{\"DATASOURCE_ID\":23269,\"VENDOR_ID\":18694,\"ACTUAL_BALANCE\":20105,\"LAST_DATE\":\"2099-03-31 18:29:40\"},\n" +
            "{\"DATASOURCE_ID\":23099,\"VENDOR_ID\":25861,\"ACTUAL_BALANCE\":30055,\"LAST_DATE\":\"2006-03-31 20:55:40\"}]";

        JsonNode arrayNode = new ObjectMapper().readTree(json);
        List<PayerBalance> parsedBalances = IntStream.range(0, arrayNode.size()).mapToObj(arrayNode::get)
            .map(mapper)
            .collect(Collectors.toList());

        assertEquals(0, getCurrentBalance().size());

        consumer.accept(parsedBalances);

        parsedBalances.sort(Comparator.comparing(PayerBalance::getEndTime));
        assertBalance(parsedBalances.get(0), "23099", "300.55", "2006-03-31T22:55:40Z");
        assertBalance(parsedBalances.get(1), "23429", "100", "2008-03-31T22:42:40Z");
        assertBalance(parsedBalances.get(2), "23269", "201.05", "2099-03-31T20:29:40Z");

        assertEquals(3, getCurrentBalance().size());
        List<PayerBalance> balancesInDb = getCurrentBalance();
        balancesInDb.sort(Comparator.comparing(PayerBalance::getEndTime));

        assertBalance(balancesInDb.get(0), "23099", "300.55");
        assertBalance(balancesInDb.get(1), "23429", "100");
        assertBalance(balancesInDb.get(2), "23269", "201.05");
    }

    @Test
    public void testPriceUpload() throws IOException {
        ArgumentCaptor<Function> mappingCaptor = ArgumentCaptor.forClass(Function.class);
        ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

        job.loadVendorPrices();

        verify(ytHelper.getHahnClient(), times(1)).consumeTableBatched(
            any(YPath.class), anyInt(),
            mappingCaptor.capture(),
            consumerCaptor.capture());

        Function<JsonNode, ContentPrice> mapper = mappingCaptor.getValue();
        Consumer<List<ContentPrice>> consumer = consumerCaptor.getValue();

        String json = "[{\"VENDOR_ID\":25861,\"DATASOURCE_ID\":23099,\"MODEL_ID\":216521530,\"PRICE\":1000,\"CHARGE\":\"36.66\",\"TARGET_COUNT\":9,\"PRICE_FINISH_TIME\":\"2021-04-01 01:00:02\"},\n" +
            "{\"VENDOR_ID\":12313,\"DATASOURCE_ID\":24234,\"MODEL_ID\":11155028,\"PRICE\":50,\"CHARGE\":\"1.82\",\"TARGET_COUNT\":0,\"PRICE_FINISH_TIME\":\"2021-04-01 01:00:02\"},\n" +
            "{\"VENDOR_ID\":13123,\"DATASOURCE_ID\":6353,\"MODEL_ID\":216519177,\"PRICE\":90,\"CHARGE\":\"3.3\",\"PRICE_FINISH_TIME\":\"2025-04-01 01:00:02\"}]";

        JsonNode arrayNode = new ObjectMapper().readTree(json);
        List<ContentPrice> parsedPrice = IntStream.range(0, arrayNode.size()).mapToObj(arrayNode::get)
            .map(mapper)
            .collect(Collectors.toList());

        assertEquals(0, getCurrentPrices().size());

        consumer.accept(parsedPrice);

        parsedPrice.sort(Comparator.comparing(ContentPrice::getEndTime));
        assertPrice(parsedPrice.get(0), "23099", 216521530, "1000", "36.66", "2021-04-01T03:00:02Z", 25861, 9);
        assertPrice(parsedPrice.get(1), "24234", 11155028, "50", "1.82", "2021-04-01T03:00:02Z", 12313, null);
        assertPrice(parsedPrice.get(2), "6353", 216519177, "90", "3.3", "2025-04-01T03:00:02Z", 13123, null);

        assertEquals(3, getCurrentPrices().size());
        List<ContentPrice> pricesInDb = getCurrentPrices();
        pricesInDb.sort(Comparator.comparing(ContentPrice::getEndTime));

        assertPrice(pricesInDb.get(0), "23099", 216521530, "1000", "36.66", 25861, 9);
        assertPrice(pricesInDb.get(1), "24234", 11155028, "50", "1.82", 12313, null);
        assertPrice(pricesInDb.get(2), "6353", 216519177, "90", "3.3", 13123, null);
    }

    private void assertBalance(PayerBalance item, String payerId, String balance) {
        assertEquals(PersPayerType.VENDOR, item.getPayer().getType());
        assertEquals(payerId, item.getPayer().getId());
        assertEquals(balance, item.getBalance().toString());
    }

    private void assertBalance(PayerBalance item, String payerId, String balance, String endTime) {
        assertBalance(item, payerId, balance);
        assertEquals(Instant.parse(endTime), item.getEndTime());
    }

    private void assertPrice(ContentPrice item,
                             String payerId,
                             long modelId,
                             String amount,
                             String amountCharge,
                             long vendorId,
                             Integer targetCount) {
        assertEquals(PersPayerType.VENDOR, item.getPayer().getType());
        assertEquals(payerId, item.getPayer().getId());
        assertEquals(PersPayEntityType.MODEL_GRADE, item.getEntity().getType());
        assertEquals(modelId, item.getEntity().getIdLong());
        assertEquals(amount, item.getAmount().toString());
        assertEquals(amountCharge, item.getAmountCharge().toString());

        assertEquals(Map.of(PersPayment.DATA_VENDOR_ID, String.valueOf(vendorId)), item.getData());

        if (targetCount != null) {
            assertEquals(Map.of(PaymentProcessor.TARGET_COUNT_LIMIT, String.valueOf(targetCount)), item.getLimits());
        } else {
            assertNull(item.getLimits());
        }
    }

    private void assertPrice(ContentPrice item,
                             String payerId,
                             long modelId,
                             String amount,
                             String amountCharge,
                             String endTime,
                             long vendorId,
                             Integer targetCount) {
        assertPrice(item, payerId, modelId, amount, amountCharge, vendorId, targetCount);
        assertEquals(Instant.parse(endTime), item.getEndTime());
    }

    private List<PayerBalance> getCurrentBalance() {
        return jdbcTemplate.query(
            "select payer_type, payer_id, balance, end_time\n" +
                "from pay.payer_balance",
            (rs, rowNum) -> {
                PayerBalance result = new PayerBalance(
                    PersPayUtils.parsePayer(rs),
                    rs.getBigDecimal("balance")
                );
                result.setEndTime(rs.getTimestamp("end_time").toInstant());
                return result;
            });
    }

    private List<ContentPrice> getCurrentPrices() {
        return jdbcTemplate.query(
            "select *, 0 as balance\n" +
                "from pay.content_price",
            (rs, idx) -> {
                ContentPrice result = ContentPrice.parse(rs, idx);
                result.setEndTime(rs.getTimestamp("end_time").toInstant());
                return result;
            });
    }

}
