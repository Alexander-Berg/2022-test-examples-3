package ru.yandex.market.payout.Dao;

import java.time.LocalDate;
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
import ru.yandex.market.payout.Service.PayoutGroupPaymentOrderImportService;
import ru.yandex.market.payout.YtDao.PayoutGroupPaymentOrderYtDao;
import ru.yandex.market.payout.model.PayoutGroupPaymentOrder;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для {@link PayoutGroupPaymentOrderImportService}
 */
@ExtendWith(MockitoExtension.class)
class PayoutGroupPaymentOrderImportServiceTest extends FunctionalTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 6);

    @Autowired
    private PayoutGroupPaymentOrderImportService payoutGroupPaymentOrderImportService;
    @Autowired
    private PayoutGroupPaymentOrderDao payoutGroupPaymentOrderDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Yt yt;

    @Mock
    private Cypress cypress;

    private static final List<PayoutGroupPaymentOrder> PAYOUT_GROUP_PAYMENT_ORDERS = List.of(
            new PayoutGroupPaymentOrder(1L, 3L),
            new PayoutGroupPaymentOrder(2L, 4L)
    );

    @Test
    @DisplayName("Импорт денег, которые надо выплатить или удержать для магазина (позитивный тест)")
    @DbUnitDataSet(
            before = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderImportServiceTest" +
                    ".importPayoutGroupPaymentOrder.before.csv",
            after = "PayoutGroupPaymentOrderDaoTest/PayoutGroupPaymentOrderImportServiceTest" +
                    ".importPayoutGroupPaymentOrder.after.csv"
    )
    void testImportPayoutGroupPaymentOrder() {
        PayoutGroupPaymentOrderYtDao payoutGroupPaymentOrderYtDao = mockPayoutGroupPaymentOrderYtDao();

        PayoutGroupPaymentOrderImportService payoutGroupPaymentOrderImportService =
                new PayoutGroupPaymentOrderImportService(payoutGroupPaymentOrderYtDao,
                        transactionTemplate, payoutGroupPaymentOrderDao);

        payoutGroupPaymentOrderImportService.process(IMPORT_DATE);

        Mockito.verify(payoutGroupPaymentOrderYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(payoutGroupPaymentOrderYtDao, Mockito.times(1))
                .processPayoutGroupPaymentOrder(Mockito.eq(IMPORT_DATE));
        Mockito.verifyNoMoreInteractions(payoutGroupPaymentOrderYtDao);
    }

    @Test
    @DisplayName("Импорт денег при несуществующих табличках (негативный тест)")
    void testImportPayoutGroupPaymentOrderWithNonExistentTable() {
        Mockito.when(yt.cypress()).thenReturn(cypress);
        Mockito.when(cypress.exists(any(YPath.class))).thenReturn(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> payoutGroupPaymentOrderImportService.process(IMPORT_DATE));
    }


    private static PayoutGroupPaymentOrderYtDao mockPayoutGroupPaymentOrderYtDao() {
        PayoutGroupPaymentOrderYtDao payoutGroupPaymentOrderYtDao = Mockito.mock(PayoutGroupPaymentOrderYtDao.class);

        Mockito.doAnswer(invocation -> true).when(payoutGroupPaymentOrderYtDao).verifyYtTablesExistence(any(LocalDate.class));

        Mockito.when(payoutGroupPaymentOrderYtDao.processPayoutGroupPaymentOrder(any(LocalDate.class)))
                .thenAnswer(invocation -> PAYOUT_GROUP_PAYMENT_ORDERS);


        return payoutGroupPaymentOrderYtDao;
    }
}
