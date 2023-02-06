package ru.yandex.market.billing.payment.dao;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.payment.model.Accrual;
import ru.yandex.market.billing.payment.model.AccrualWithPayoutTrantime;
import ru.yandex.market.billing.payment.model.OrderPayoutTrantime;
import ru.yandex.market.billing.payment.model.TrantimeStatus;
import ru.yandex.market.billing.payment.services.AccrualService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.AccrualProductType;
import ru.yandex.market.core.payment.EntityType;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PayoutStatus;
import ru.yandex.market.core.payment.PaysysTypeCc;
import ru.yandex.market.core.payment.TransactionType;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * Тесты для {@link AccrualDao}
 */
class AccrualDaoTest extends FunctionalTest {
    private static final LocalDateTime TEST_DATE_TIME = LocalDateTime.of(2021, 11, 11, 11, 30, 0);
    @Autowired
    private AccrualService accrualService;

    @Autowired
    private PaymentDao paymentDao;

    private static List<AccrualWithPayoutTrantime> getAccrualsWithPaidOutPayouts() {
        return List.of(
                AccrualWithPayoutTrantime.builder()
                        .setId(3L)
                        .setEntityId(3L)
                        .setEntityType(EntityType.ITEM)
                        .setAccrualProductType(AccrualProductType.PARTNER_PAYMENT)
                        .setPaysysType(PaysysTypeCc.ACC_TINKOFF_CREDIT)
                        .setCheckouterId(3L)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setOrderId(30L)
                        .setPartnerId(300L)
                        .setAmount(3000L)
                        .setCurrency(PaymentOrderCurrency.ILS)
                        .setPaysysPartnerId(30000L)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPayoutStatus(PayoutStatus.NEW)
                        .setAccrualTrantime(TEST_DATE_TIME.atZone(ZoneId.systemDefault()).plusMinutes(30).toInstant())
                        .setOrderPayoutTrantime(TEST_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant())
                        .build()
        );
    }

    private static List<AccrualWithPayoutTrantime> getAccrualsWithCancelledPayout() {
        return List.of(
                AccrualWithPayoutTrantime.builder()
                        .setId(4L)
                        .setEntityId(4L)
                        .setEntityType(EntityType.ITEM)
                        .setAccrualProductType(AccrualProductType.ACC_SUBSIDY)
                        .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                        .setCheckouterId(4L)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setOrderId(40L)
                        .setPartnerId(400L)
                        .setAmount(4000L)
                        .setCurrency(PaymentOrderCurrency.ILS)
                        .setPaysysPartnerId(40000L)
                        .setOperatingUnit(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setPayoutStatus(PayoutStatus.CANCELED)
                        .setOrderPayoutTrantime(TEST_DATE_TIME.plusHours(1).atZone(ZoneId.systemDefault()).toInstant())
                        .setAccrualTrantime(TEST_DATE_TIME.plusHours(1).plusMinutes(30)
                                .atZone(ZoneId.systemDefault()).toInstant())
                        .build()
        );
    }

    private static List<AccrualWithPayoutTrantime> getTotalAccruals() {
        List<AccrualWithPayoutTrantime> accruals = new ArrayList<>();
        accruals.addAll(getAccrualsWithPaidOutPayouts());
        accruals.addAll(getAccrualsWithCancelledPayout());
        return accruals;
    }

    @DisplayName("Получение Id начислений для формирования платежей")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testGetGlobalAccrualIdsForPayoutGeneration.before.csv"
    )
    void testGetGlobalAccrualIdsForPayoutGeneration() {
        List<Long> accrualIds = accrualService.getGlobalAccrualIdsForPayoutGeneration();
        assertThat(accrualIds).hasSize(1);
        assertThat(accrualIds).containsExactly(3L);
    }

    @DisplayName("Получение accrual + order_payout_trantime для генерации payout по Id")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testGetGlobalAccrualIdsForPayoutGeneration.before.csv"
    )
    void testGetGlobalAccrualsForPayoutsGeneration() {
        List<AccrualWithPayoutTrantime> accruals =
                accrualService.getAccrualsWithPayoutTrantimeForPayoutsGeneration(List.of(3L, 4L));
        assertThat(accruals).hasSize(2);
        assertThat(accruals)
                .containsExactlyInAnyOrder(getTotalAccruals().toArray(new AccrualWithPayoutTrantime[0]));
    }

    @DisplayName("Обновление статуса начислений")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testUpdatePayoutStatus.before.csv",
            after = "AccrualDaoTest.testUpdatePayoutStatus.after.csv"
    )
    void testUpdateIsPayoutCreatedStatus() {
        List<Long> ids = StreamEx.of(getAccrualsWithPaidOutPayouts()).map(AccrualWithPayoutTrantime::getId).toList();
        accrualService.updateAccrualsPayoutStatusToPaidOut(ids);
    }

    @DisplayName("Пропуск невалидных org_id")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testInvalidOrgId.before.csv"
    )
    void skipIncorrectAccrualOrgId() {
        List<Long> accrualIds = accrualService.getGlobalAccrualIdsForPayoutGeneration();
        assertThat(accrualIds).isEmpty();
    }

    @DisplayName("Тест получения трантайма из order_payout_trantime")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testAccrualWithPayoutTrantime.before.csv"
    )
    void testAccrualWithPayoutTrantime() {
        // Создаём и сохраняем OrderPayoutTrantime
        OrderPayoutTrantime orderPayoutTrantime = OrderPayoutTrantime.builder()
                .setOrderId(1L)
                .setPartnerId(2L)
                .setTrantime(TEST_DATE_TIME.atZone(ZoneId.systemDefault()).toInstant())
                .setStatus(TrantimeStatus.NEW)
                .setOrgId(OperatingUnit.YANDEX_MARKET)
                .build();
        paymentDao.insertOrderPayoutTrantimesIfNotExists(List.of(orderPayoutTrantime));

        // Создаём и сохраняем Accrual, при этом trantime у Accrual отличается от trantime OrderPayoutTrantime
        Accrual accrual = Accrual.builder()
                .setEntityId(3L)
                .setEntityType(EntityType.ITEM)
                .setAccrualProductType(AccrualProductType.PARTNER_PAYMENT)
                .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                .setCheckouterId(4L)
                .setTransactionType(TransactionType.PAYMENT)
                .setOrderId(1L)
                .setPartnerId(2L)
                .setTrantime(TEST_DATE_TIME.plusHours(1L).atZone(ZoneId.systemDefault()).toInstant())
                .setAmount(5L)
                .setCurrency(PaymentOrderCurrency.RUB)
                .setPaysysPartnerId(6L)
                .setOperatingUnit(OperatingUnit.YANDEX_MARKET)
                .setPayoutStatus(PayoutStatus.NEW)
                .build();
        accrualService.insertAccrualsIfNotExists(List.of(accrual));

        // Получаем Id accrual'ов для внутренних продуктов
        List<Long> accrualIds = accrualService.getDomesticAccrualIdsForPayoutGeneration(null);
        assertThat(accrualIds).hasSize(1);

        // Получаем AccrualWithPayoutTrantime
        List<AccrualWithPayoutTrantime> accrualsWithPayoutTrantimeForPayoutsGeneration =
                accrualService.getAccrualsWithPayoutTrantimeForPayoutsGeneration(accrualIds);
        assertThat(accrualsWithPayoutTrantimeForPayoutsGeneration).hasSize(1);

        // Проверяем, что у AccrualWithPayoutTrantime trantime приехал из OrderPayoutTrantime, а не из Accrual
        AccrualWithPayoutTrantime accrualWithPayoutTrantime = accrualsWithPayoutTrantimeForPayoutsGeneration.get(0);
        assertThat(accrualWithPayoutTrantime.getOrderPayoutTrantime()).isEqualTo(orderPayoutTrantime.getTrantime());
        assertThat(accrualWithPayoutTrantime.getOrderPayoutTrantime()).isNotEqualTo(accrual.getTrantime());
    }

    @DisplayName("Обновление статуса начислений для игнорируемых партнёров")
    @Test
    @DbUnitDataSet(
            before = "AccrualDaoTest.testUpdatePayoutStatusForIgnoredPartnerAccruals.before.csv",
            after = "AccrualDaoTest.testUpdatePayoutStatusForIgnoredPartnerAccruals.after.csv"
    )
    void testUpdatePayoutStatusForIgnoredPartnerAccruals() {
        accrualService.updateAccrualPayoutStatusForPartners(
                PayoutStatus.IGNORED, 0, 6, Set.of(431782L, 2652811L, 2476913L)
        );
    }
}
