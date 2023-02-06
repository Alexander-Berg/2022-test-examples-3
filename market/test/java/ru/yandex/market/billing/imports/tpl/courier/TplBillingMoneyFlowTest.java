package ru.yandex.market.billing.imports.tpl.courier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.payment.executors.CourierPayoutsFromAccrualsExecutor;
import ru.yandex.market.billing.payment.services.PaymentOrderDraftExecutor;
import ru.yandex.market.billing.payment.services.PaymentOrderFromDraftExecutor;
import ru.yandex.market.billing.payment.services.TplCourierTransactionToAccrualExecutor;
import ru.yandex.market.billing.tlog.collection.PayoutsTransactionLogCollectionExecutor;
import ru.yandex.market.billing.tlog.executor.CourierTransactionLogCollectionExecutor;
import ru.yandex.market.billing.util.yt.YtCluster;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;

import static ru.yandex.market.billing.imports.tpl.courier.MoneyFlowTransactionYtTestUtil.mockYt;

/**
 * Интеграционный тест всего УВ для биллинга курьерки
 */
public class TplBillingMoneyFlowTest extends FunctionalTest {

    @Autowired
    TplCourierTransactionToAccrualExecutor tplCourierTransactionToAccrualExecutor;

    @Autowired
    CourierPayoutsFromAccrualsExecutor courierPayoutsFromAccrualsExecutor;

    @Autowired
    PaymentOrderDraftExecutor tplPaymentOrderDraftExecutor;

    @Autowired
    PaymentOrderFromDraftExecutor tplCourierPaymentOrderFromDraftExecutor;

    @Autowired
    CourierTransactionLogCollectionExecutor courierTransactionLogCollectionExecutor;

    @Autowired
    PayoutsTransactionLogCollectionExecutor payoutsPaymentsTransactionLogCollectionExecutor;

    @Autowired
    PayoutsTransactionLogCollectionExecutor payoutsExpensesTransactionLogCollectionExecutor;

    @Autowired
    TplCourierMoneyFlowTransactionImportExecutor tplCourierMoneyFlowTransactionImportExecutor;

    @Autowired
    Yt yt;

    @Autowired
    YtCluster ytCluster;

    @Autowired
    TestableClock clock;

    @AfterEach
    void tearDown() {
        clock.clearFixed();
    }

    @Test
    @DisplayName("Тест для для партнерских курьеров")
    @DbUnitDataSet(
            before = "TplBillingMoneyFlowTest.PartnerCourier.before.csv",
            after = "TplBillingMoneyFlowTest.PartnerCourier.after.csv"
    )
    void testCourier() {
        mockYt(ytCluster, yt, getPartnerCourierTransactions());
        var now = LocalDateTime.of(
                        LocalDate.of(2022, 6, 5),
                        LocalTime.of(13, 58)
                )
                .atZone(DateTimes.MOSCOW_TIME_ZONE)
                .toInstant();
        clock.setFixed(now, ZoneId.systemDefault());

        runMoneyFlow();
    }

    @Test
    @DisplayName("Тест для самозанятых курьеров")
    @DbUnitDataSet(
            before = "TplBillingMoneyFlowTest.SelfEmployedCourier.before.csv",
            after = "TplBillingMoneyFlowTest.SelfEmployedCourier.after.csv"
    )
    void testSelfEmployed() {
        mockYt(ytCluster, yt, getSelfEmployedTransactions());
        var now = LocalDateTime.of(
                        LocalDate.of(2022, 6, 6),
                        LocalTime.of(13, 58)
                )
                .atZone(DateTimes.MOSCOW_TIME_ZONE)
                .toInstant();
        clock.setFixed(now, ZoneId.systemDefault());

        runMoneyFlow();
    }

    private void runMoneyFlow() {
        tplCourierMoneyFlowTransactionImportExecutor.doJob();
        tplCourierTransactionToAccrualExecutor.doJob();
        courierPayoutsFromAccrualsExecutor.doJob();
        tplPaymentOrderDraftExecutor.doJob();
        tplCourierPaymentOrderFromDraftExecutor.doJob();
        courierTransactionLogCollectionExecutor.doJob();
        payoutsPaymentsTransactionLogCollectionExecutor.doJob();
        payoutsExpensesTransactionLogCollectionExecutor.doJob();
    }

    private List<Map<String, Object>> getPartnerCourierTransactions() {
        return List.of(
                //MGT
                Map.ofEntries(
                        Map.entry("id", 1L),
                        Map.entry("userShiftId", 2L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-06-04"),
                        Map.entry("trantime", "2022-06-05"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 1703L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "PARTNER"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                ),
                //KGT
                Map.ofEntries(
                        Map.entry("id", 2L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-06-04"),
                        Map.entry("trantime", "2022-06-05"),
                        Map.entry("partnerId", 101L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "KGT"),
                        Map.entry("amount", 1800L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "PARTNER"),
                        Map.entry("clientId", "111111"),
                        Map.entry("contractId", "222222"),
                        Map.entry("personId", "333333")
                ),
                //FINE
                Map.ofEntries(
                        Map.entry("id", 3L),
                        Map.entry("transactionType", "fines"),
                        Map.entry("eventTime", "2022-06-04"),
                        Map.entry("trantime", "2022-06-05"),
                        Map.entry("partnerId", 101L),
                        Map.entry("paymentType", "REFUND"),
                        Map.entry("productType", "KGT"),
                        Map.entry("amount", 100L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "PARTNER"),
                        Map.entry("clientId", "111111"),
                        Map.entry("contractId", "222222"),
                        Map.entry("personId", "333333")
                )
        );
    }

    private List<Map<String, Object>> getSelfEmployedTransactions() {
        return List.of(
                Map.ofEntries(
                        Map.entry("id", 1L),
                        Map.entry("userShiftId", 1L),
                        Map.entry("transactionType", "payments"),
                        Map.entry("eventTime", "2022-06-04"),
                        Map.entry("trantime", "2022-06-05"),
                        Map.entry("partnerId", 100L),
                        Map.entry("paymentType", "PAYMENT"),
                        Map.entry("productType", "MGT"),
                        Map.entry("amount", 1703L),
                        Map.entry("currency", "RUB"),
                        Map.entry("isCorrection", false),
                        Map.entry("orgId", 64554L),
                        Map.entry("userType", "SELF_EMPLOYED"),
                        Map.entry("clientId", "1234567"),
                        Map.entry("contractId", "987654"),
                        Map.entry("personId", "11223344")
                )
        );
    }

}
