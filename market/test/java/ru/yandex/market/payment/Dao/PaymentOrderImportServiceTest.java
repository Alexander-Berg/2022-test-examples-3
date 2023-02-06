package ru.yandex.market.payment.Dao;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.payment.PaymentOrderCurrency;
import ru.yandex.market.core.order.payment.PaymentOrderFactoring;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payment.Service.PaymentOrderImportService;
import ru.yandex.market.payment.YtDao.PaymentOrderYtDao;
import ru.yandex.market.payment.model.PaymentOrder;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для {@link PaymentOrderImportService}
 */
@ExtendWith(MockitoExtension.class)
public class PaymentOrderImportServiceTest extends FunctionalTest {


    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 6);

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    private PaymentOrderImportService paymentOrderImportService;
    @Autowired
    private PaymentOrderDao paymentOrderDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Yt yt;

    @Mock
    private Cypress cypress;

    private static final List<PaymentOrder> PAYMENT_ORDERS = List.of(
            PaymentOrder.builder()
                    .setId(1L)
                    .setClientId(1L)
                    .setContractId(1L).setServiceId(1L)
                    .setTransactionType(TransactionType.REFUND)
                    .setSecuredPayment(true)
                    .setFactoring(PaymentOrderFactoring.MARKET)
                    .setProductType(ProductType.SUBSIDY)
                    .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                    .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZONE_ID).toInstant())
                    .setAmount(100L)
                    .setCurrency(PaymentOrderCurrency.RUB)
                    .setExportedToTlog(true).build(),
            PaymentOrder.builder()
                    .setId(2L)
                    .setClientId(1L)
                    .setContractId(1L).setServiceId(1L)
                    .setTransactionType(TransactionType.REFUND)
                    .setSecuredPayment(true)
                    .setFactoring(PaymentOrderFactoring.MARKET)
                    .setProductType(ProductType.SUBSIDY)
                    .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                    .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZONE_ID).toInstant())
                    .setAmount(100L)
                    .setCurrency(PaymentOrderCurrency.RUB)
                    .setExportedToTlog(true).build());

    @Test
    @DisplayName("Импорт команд на выплату (позитивный тест)")
    @DbUnitDataSet(
            before = "PaymentOrderImportServiceTest.importPaymentOrder.before.csv",
            after = "PaymentOrderImportServiceTest.importPaymentOrder.after.csv"
    )
    void testImportPaymentOrder() {
        PaymentOrderYtDao paymentOrderYtDao = mockPaymentOrderYtDao();

        PaymentOrderImportService paymentOrderImportService = new PaymentOrderImportService(paymentOrderYtDao,
                transactionTemplate, paymentOrderDao);

        paymentOrderImportService.process(IMPORT_DATE);

        Mockito.verify(paymentOrderYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(paymentOrderYtDao, Mockito.times(1))
                .processPaymentOrder(Mockito.eq(IMPORT_DATE));
        Mockito.verifyNoMoreInteractions(paymentOrderYtDao);
    }

    @Test
    @DisplayName("Импорт команд на выплату при несуществующих табличках (негативный тест)")
    void testImportPaymentOrderWithNonExistentTable() {
        Mockito.when(yt.cypress()).thenReturn(cypress);
        Mockito.when(cypress.exists(any(YPath.class))).thenReturn(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> paymentOrderImportService.process(IMPORT_DATE));
    }


    private static PaymentOrderYtDao mockPaymentOrderYtDao() {
        PaymentOrderYtDao paymentOrderYtDao = Mockito.mock(PaymentOrderYtDao.class);

        Mockito.doAnswer(invocation -> true).when(paymentOrderYtDao).verifyYtTablesExistence(any(LocalDate.class));

        Mockito.when(paymentOrderYtDao.processPaymentOrder(any(LocalDate.class)))
                .thenAnswer(invocation -> PAYMENT_ORDERS);


        return paymentOrderYtDao;
    }
}
