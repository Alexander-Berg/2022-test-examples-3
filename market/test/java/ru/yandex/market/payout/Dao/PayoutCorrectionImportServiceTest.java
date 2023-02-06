package ru.yandex.market.payout.Dao;

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
import ru.yandex.market.core.order.payment.EntityType;
import ru.yandex.market.core.order.payment.PaysysTypeCc;
import ru.yandex.market.core.order.payment.ProductType;
import ru.yandex.market.core.order.payment.TransactionType;
import ru.yandex.market.payout.Service.PayoutCorrectionImportService;
import ru.yandex.market.payout.YtDao.PayoutCorrectionYtDao;
import ru.yandex.market.payout.model.PayoutCorrection;

import static org.mockito.ArgumentMatchers.any;

/**
 * Тест для {@link PayoutCorrectionImportService}
 */
@ExtendWith(MockitoExtension.class)
class PayoutCorrectionImportServiceTest extends FunctionalTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 6);

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    private PayoutCorrectionImportService payoutCorrectionImportService;
    @Autowired
    private PayoutCorrectionDao payoutCorrectionDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Yt yt;

    @Mock
    private Cypress cypress;

    private static final List<PayoutCorrection> PAYOUT_CORRECTION = List.of(
            PayoutCorrection.builder()
                    .setPayoutId(1L)
                    .setEntityId(1L)
                    .setCheckouterId(1L)
                    .setTransactionType(TransactionType.REFUND)
                    .setProductType(ProductType.PARTNER_PAYMENT)
                    .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                    .setOrderId(1L)
                    .setPartnerId(1L)
                    .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZONE_ID).toInstant())
                    .setAmount(1000L)
                    .setUuid("11122")
                    .setLogin("22111")
                    .setStTicket("1122")
                    .setPayoutGroupId(22312L)
                    .setEntityType(EntityType.DELIVERY)
                    .setAccrualCorrectionId(666L)
                    .setPaysysPartnerId(1L).build(),
            PayoutCorrection.builder()
                    .setPayoutId(2L)
                    .setEntityId(1L)
                    .setCheckouterId(1L)
                    .setTransactionType(TransactionType.REFUND)
                    .setProductType(ProductType.PARTNER_PAYMENT)
                    .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                    .setOrderId(1L)
                    .setPartnerId(1L)
                    .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZONE_ID).toInstant())
                    .setAmount(1000L)
                    .setUuid("11122")
                    .setLogin("22111")
                    .setStTicket("1122")
                    .setPayoutGroupId(22312L)
                    .setEntityType(EntityType.DELIVERY)
                    .setAccrualCorrectionId(null)
                    .setPaysysPartnerId(null).build());

    @Test
    @DisplayName("Импорт корректировки денег, которые надо выплатить или удержать для магазина (позитивный тест)")
    @DbUnitDataSet(
            before = "PayoutCorrectionDaoTest/PayoutCorrectionImportServiceTest.importPayoutCorrection.before.csv",
            after = "PayoutCorrectionDaoTest/PayoutCorrectionImportServiceTest.importPayoutCorrection.after.csv"
    )
    void testImportPayoutCorrection() {
        PayoutCorrectionYtDao payoutCorrectionYtDao = mockPayoutCorrectionYtDao();

        PayoutCorrectionImportService payoutCorrectionImportService =
                new PayoutCorrectionImportService(payoutCorrectionYtDao,
                        transactionTemplate, payoutCorrectionDao);

        payoutCorrectionImportService.process(IMPORT_DATE);

        Mockito.verify(payoutCorrectionYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(payoutCorrectionYtDao, Mockito.times(1))
                .processPayoutCorrection(Mockito.eq(IMPORT_DATE));
        Mockito.verifyNoMoreInteractions(payoutCorrectionYtDao);
    }

    @Test
    @DisplayName("Импорт корректировки денег при несуществующих табличках (негативный тест)")
    void testImportPayoutCorrectionWithNonExistentTable() {
        Mockito.when(yt.cypress()).thenReturn(cypress);
        Mockito.when(cypress.exists(any(YPath.class))).thenReturn(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> payoutCorrectionImportService.process(IMPORT_DATE));
    }


    private PayoutCorrectionYtDao mockPayoutCorrectionYtDao() {
        PayoutCorrectionYtDao payoutCorrectionYtDao = Mockito.mock(PayoutCorrectionYtDao.class);

        Mockito.doAnswer(invocation -> true).when(payoutCorrectionYtDao).verifyYtTablesExistence(any(LocalDate.class));

        Mockito.when(payoutCorrectionYtDao.processPayoutCorrection(any(LocalDate.class)))
                .thenAnswer(invocation -> PAYOUT_CORRECTION);


        return payoutCorrectionYtDao;
    }

}
