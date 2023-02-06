package ru.yandex.market.pers.pay.tms;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.ContentPrice;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayer;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBuilder;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.pers.pay.tms.util.PersPayUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.03.2021
 */
public class UnbalancedPaymentsJobTest extends PersPayTest {
    public static final long USER_ID = 5624524;
    public static final long MODEL_ID = 234234;

    @Autowired
    private UnbalancedPaymentsJob job;

    @Autowired
    private TmsPaymentService paymentService;

    @Test
    public void testProcessMarketUnbalancedPays() {
        String nonBalancePayer = MockUtils.TEST_PAYER + 1;
        String vendorPayerId = "12345";

        PersPayer marketPayer = new PersPayer(PersPayerType.MARKET, MockUtils.TEST_PAYER);
        PersPayer vendorPayer = new PersPayer(PersPayerType.VENDOR, vendorPayerId);

        mockBalance(PersPayerType.MARKET, MockUtils.TEST_PAYER, 1000);
        mockBalance(PersPayerType.VENDOR, vendorPayerId, 1000);

        savePrice(MODEL_ID, 1, PersPayerType.MARKET, MockUtils.TEST_PAYER);
        savePrice(MODEL_ID, 1, PersPayerType.VENDOR, vendorPayerId);

        // create
        List<PersPaymentBuilder> paymentsTemplate = List.of(
            // payed
            MockUtils.testPay(MODEL_ID, USER_ID + 1).amount(100),
            // on moderation
            MockUtils.testPay(MODEL_ID, USER_ID + 2).amount(100),
            // canceled
            MockUtils.testPay(MODEL_ID, USER_ID + 3).amount(100),
            // expired
            MockUtils.testPay(MODEL_ID, USER_ID + 4).amount(100),

            // payed
            MockUtils.testPay(MODEL_ID + 1, USER_ID + 1).amount(150).payer(PersPayerType.VENDOR, vendorPayerId),
            // canceled
            MockUtils.testPay(MODEL_ID + 1, USER_ID + 2).amount(150).payer(PersPayerType.VENDOR, vendorPayerId),
            // moderation
            MockUtils.testPay(MODEL_ID + 1, USER_ID + 3).amount(150).payer(PersPayerType.VENDOR, vendorPayerId),

            //non-balance
            MockUtils.testPay(MODEL_ID + 2, USER_ID + 1).amount(100).payer(PersPayerType.MARKET, nonBalancePayer)
        );

        paymentService.savePaymentsBulk(paymentsTemplate);

        // check unbalanced payments created
        assertEquals(paymentsTemplate.size(),
            jdbcTemplate.queryForObject("select count(*) from pay.unbalanced_pays", Long.class));

        Map<PersPayer, BigDecimal> balance = getCurrentBalance();
        Map<PersPayer, BigDecimal> correctedBalances = getCorrectedBalance(MODEL_ID);

        assertEquals(2, balance.size());
        assertEquals(BigDecimal.valueOf(1000), balance.get(marketPayer));
        assertEquals(BigDecimal.valueOf(1000), balance.get(vendorPayer));

        assertEquals(BigDecimal.valueOf(600), correctedBalances.get(marketPayer));
        assertEquals(BigDecimal.valueOf(550), correctedBalances.get(vendorPayer));

        List<PersPayment> allPayments = paymentService.getPaymentsToProcess(PersPayState.NEW, 1000);
        paymentService.changeState(findPayment(allPayments, MODEL_ID, USER_ID + 1), PersPayState.PAYED, null);
        paymentService.changeState(findPayment(allPayments, MODEL_ID, USER_ID + 2), PersPayState.WAIT_MODERATION, null);
        paymentService.changeState(findPayment(allPayments, MODEL_ID, USER_ID + 3), PersPayState.CANCELED, null);
        paymentService.changeState(findPayment(allPayments, MODEL_ID, USER_ID + 4), PersPayState.EXPIRED, null);

        paymentService.changeState(findPayment(allPayments, MODEL_ID + 1, USER_ID + 1), PersPayState.PAYED, null);
        paymentService.changeState(findPayment(allPayments, MODEL_ID + 1, USER_ID + 2), PersPayState.CANCELED, null);
        paymentService
            .changeState(findPayment(allPayments, MODEL_ID + 1, USER_ID + 3), PersPayState.WAIT_MODERATION, null);

        job.processUnbalancedMarketPays();

        // check completed payments are balanced (only market)
        // failed payments unbalanced records are removed
        // non-balance unbalanced records are removed
        List<String> unbalancedPays = jdbcTemplate.queryForList("select pay_key from pay.unbalanced_pays", String.class);
        assertEquals(3, unbalancedPays.size());
        // market - on moderation + vendor - payed
        assertTrue(unbalancedPays.contains(paymentsTemplate.get(1).getPayKey()));
        assertTrue(unbalancedPays.contains(paymentsTemplate.get(4).getPayKey()));
        assertTrue(unbalancedPays.contains(paymentsTemplate.get(6).getPayKey()));

        // check balance
        balance = getCurrentBalance();
        correctedBalances = getCorrectedBalance(MODEL_ID);

        assertEquals(2, balance.size());
        assertEquals(BigDecimal.valueOf(900), balance.get(marketPayer));
        assertEquals(BigDecimal.valueOf(1000), balance.get(vendorPayer));

        assertEquals(BigDecimal.valueOf(800), correctedBalances.get(marketPayer));
        assertEquals(BigDecimal.valueOf(700), correctedBalances.get(vendorPayer));
    }

    @Test
    public void cleanupWrongUnbalanced() {
        // one payment without balance
        // other with expired balance
        // one should remain
        // another should die in the most unrespectable way
        String nonBalancePayer = MockUtils.TEST_PAYER + 1;

        mockBalance(PersPayerType.MARKET, MockUtils.TEST_PAYER, 1000);

        jdbcTemplate.update("update pay.payer_balance set end_time = now() - interval '10' day");

        // create
        List<PersPaymentBuilder> paymentsTemplate = List.of(
            MockUtils.testPay(MODEL_ID, USER_ID + 1),
            MockUtils.testPay(MODEL_ID, USER_ID + 2).payer(PersPayerType.MARKET, nonBalancePayer)
        );

        paymentService.savePaymentsBulk(paymentsTemplate);

        job.processUnbalancedMarketPays();

        List<String> unbalancedPays = jdbcTemplate.queryForList("select pay_key from pay.unbalanced_pays", String.class);
        assertEquals(1, unbalancedPays.size());
        assertTrue(unbalancedPays.contains(paymentsTemplate.get(0).getPayKey()));
    }

    @Test
    public void cleanupOld() {
        // clean unbalanced record for old-gone payment
        mockBalance(PersPayerType.MARKET, MockUtils.TEST_PAYER, 1000);

        // create
        List<PersPaymentBuilder> paymentsTemplate = List.of(
            MockUtils.testPay(MODEL_ID, USER_ID + 1),
            MockUtils.testPay(MODEL_ID, USER_ID + 2)
        );

        paymentService.savePaymentsBulk(paymentsTemplate);

        jdbcTemplate.update("delete from pay.payment where pay_key = ?", paymentsTemplate.get(0).getPayKey());
        jdbcTemplate.update("update pay.unbalanced_pays set upd_time = now() - interval '2' day");

        job.processUnbalancedMarketPays();

        List<String> unbalancedPays = jdbcTemplate.queryForList("select pay_key from pay.unbalanced_pays", String.class);
        assertEquals(1, unbalancedPays.size());
        assertTrue(unbalancedPays.contains(paymentsTemplate.get(1).getPayKey()));
    }

    @NotNull
    private Map<PersPayer, BigDecimal> getCorrectedBalance(long modelId) {
        return paymentService
            .getActivePrices(Set.of(new PersPayEntity(MODEL_GRADE, modelId).toShortString()))
            .stream()
            .collect(Collectors.toMap(ContentPrice::getPayer, ContentPrice::getBalance, (x, y) -> x));
    }

    private Map<PersPayer, BigDecimal> getCurrentBalance() {
        Map<PersPayer, BigDecimal> balance = new HashMap<>();
        jdbcTemplate.query(
            "select payer_type, payer_id, balance\n" +
                "from pay.payer_balance\n" +
                "where end_time > now()",
            (rs, rowNum) -> {
                balance.put(PersPayUtils.parsePayer(rs), rs.getBigDecimal("balance"));
                return null;
            });
        return balance;
    }

    private long findPayment(List<PersPayment> source, long modelId, long userId) {
        return source.stream()
            .filter(x -> x.getPayKey().equals(MockUtils.testPay(modelId, userId).getPayKey()))
            .map(PersPayment::getId)
            .findFirst()
            .orElseThrow(RuntimeException::new);
    }

    public void mockBalance(PersPayerType payerType, String payerId, int balance) {
        paymentService.saveBalance(payerType, payerId, BigDecimal.valueOf(balance), 1000);
    }

    private void savePrice(long modelId, int amount, PersPayerType payerType, String payerId) {
        ContentPrice price = MockUtils.testPrice(modelId, amount);
        price.setPayer(new PersPayer(payerType, payerId));
        paymentService.savePrice(price, TimeUnit.DAYS.toSeconds(1));
    }

}
