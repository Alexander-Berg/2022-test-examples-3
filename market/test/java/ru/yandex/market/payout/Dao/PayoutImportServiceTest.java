package ru.yandex.market.payout.Dao;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

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
import ru.yandex.market.payout.Service.PayoutImportService;
import ru.yandex.market.payout.YtDao.PayoutYtDao;
import ru.yandex.market.payout.model.Payout;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Тест для {@link PayoutImportService}
 */
@ExtendWith(MockitoExtension.class)
class PayoutImportServiceTest extends FunctionalTest {


    private static final LocalDate IMPORT_DATE = LocalDate.of(2021, 8, 6);

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Autowired
    private PayoutImportService payoutImportService;
    @Autowired
    private PayoutDao payoutDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private Yt yt;

    @Mock
    private Cypress cypress;

    private static final List<Payout> PAYOUT_CORRECTION = List.of(
            Payout.builder()
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
                    .setPayoutGroupId(22312L)
                    .setEntityType(EntityType.DELIVERY)
                    .setPaysysPartnerId(1L).build(),
            Payout.builder()
                    .setPayoutId(2L)
                    .setEntityId(2L)
                    .setCheckouterId(1L)
                    .setTransactionType(TransactionType.REFUND)
                    .setProductType(ProductType.PARTNER_PAYMENT)
                    .setPaysysTypeCc(PaysysTypeCc.ACC_APPLE_PAY)
                    .setOrderId(1L)
                    .setPartnerId(1L)
                    .setTrantime(IMPORT_DATE.atStartOfDay().atZone(ZONE_ID).toInstant())
                    .setAmount(1000L)
                    .setPayoutGroupId(22312L)
                    .setEntityType(EntityType.DELIVERY)
                    .setPaysysPartnerId(null).build());

    @Test
    @DisplayName("Импорт денег, которые надо выплатить или удержать для магазина (позитивный тест)")
    @DbUnitDataSet(
            before = "PayoutDaoTest/PayoutImportServiceTest.importPayout.before.csv",
            after = "PayoutDaoTest/PayoutImportServiceTest.importPayout.after.csv"
    )
    void testImportPayout() {
        PayoutYtDao payoutYtDao = mockPayoutYtDao();

        PayoutImportService payoutImportService =
                new PayoutImportService(payoutYtDao,
                        transactionTemplate, payoutDao);

        payoutImportService.process(IMPORT_DATE);

        Mockito.verify(payoutYtDao, Mockito.times(1))
                .verifyYtTablesExistence(Mockito.eq(IMPORT_DATE));
        Mockito.verify(payoutYtDao, Mockito.times(1))
                .processPayout(Mockito.eq(IMPORT_DATE), any());
        Mockito.verifyNoMoreInteractions(payoutYtDao);
    }

    @Test
    @DisplayName("Импорт денег при несуществующих табличках (негативный тест)")
    void testImportPayoutWithNonExistentTable() {
        Mockito.when(yt.cypress()).thenReturn(cypress);
        Mockito.when(cypress.exists(any(YPath.class))).thenReturn(false);
        Assertions.assertThrows(IllegalStateException.class,
                () -> payoutImportService.process(IMPORT_DATE));
    }


    private static PayoutYtDao mockPayoutYtDao() {
        PayoutYtDao payoutYtDao = Mockito.mock(PayoutYtDao.class);

        Mockito.doAnswer(invocation -> true).when(payoutYtDao).verifyYtTablesExistence(any(LocalDate.class));

        doAnswer(invocation -> {
            Consumer<List<Payout>> consumer = invocation.getArgument(1);
            consumer.accept(PAYOUT_CORRECTION);
            return null;
        }).when(payoutYtDao).processPayout(any(LocalDate.class), any());

        return payoutYtDao;
    }

}
