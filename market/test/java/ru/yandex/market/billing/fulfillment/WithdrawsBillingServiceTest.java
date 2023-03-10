package ru.yandex.market.billing.fulfillment;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.FulfillmentTariffDao;
import ru.yandex.market.core.fulfillment.OrderType;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.FulfillmentTariff;
import ru.yandex.market.core.fulfillment.model.ValueType;
import ru.yandex.market.fulfillment.entities.base.DateTimeInterval;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WithdrawsBillingServiceTest extends FunctionalTest {
    @Autowired
    private FulfillmentTariffDao fulfillmentTariffDao;

    @Autowired
    private WithdrawsBillingService withdrawsBillingService;

    private OffsetDateTime createOffsetDateTime(LocalDate date) {
        return ZonedDateTime.of(date, LocalTime.MIDNIGHT, ZoneId.systemDefault()).toOffsetDateTime();
    }

    private FulfillmentTariff createTariff(
            LocalDate from,
            LocalDate to,
            Long dimensionsTo,
            Long weightTo,
            int value) {
        return new FulfillmentTariff(
                new DateTimeInterval(
                        createOffsetDateTime(from),
                        createOffsetDateTime(to)
                ),
                BillingServiceType.FF_WITHDRAW,
                null,
                dimensionsTo,
                weightTo,
                value,
                null,
                null,
                ValueType.ABSOLUTE,
                BillingUnit.ITEM,
                OrderType.FULFILLMENT,
                null,
                null,
                null
        );
    }

    private void mock(List<FulfillmentTariff> list) {
        Mockito.when(fulfillmentTariffDao.getOrderedTariffs(Mockito.any())).thenReturn(list);
    }

    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.before.csv",
            after = "WithdrawsBillingServiceTest.after.csv"
    )
    @Test
    void testService() {
        mock(List.of(
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 11, 1),
                        null,
                        null,
                        2100
                ),
                createTariff(
                        LocalDate.of(2018, 11, 1),
                        LocalDate.MAX,
                        1000L,
                        10000L,
                        2000
                ),
                createTariff(
                        LocalDate.of(2018, 11, 1),
                        LocalDate.MAX,
                        null,
                        null,
                        3000
                )
        ));
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
        withdrawsBillingService.process(LocalDate.of(2018, 5, 22));
    }

    @Test
    @DisplayName("???????? ???? ????, ?????? ?????????? ???? ???????? ?????????????? ???????????????????? (???? ???????? ???????????????? ??????????????)")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.diffDates.before.csv",
            after = "WithdrawsBillingServiceTest.diffDates.after.csv"
    )
    void testWithBillWithDiffFinishedAndCreatedDate() {
        mock(List.of(
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2022, 3, 1),
                        null,
                        null,
                        1
                ),
                createTariff(
                        LocalDate.of(2022, 3, 1),
                        LocalDate.MAX,
                        null,
                        null,
                        2000
                )
        ));
        withdrawsBillingService.process(LocalDate.of(2022, 3, 5));
    }

    @DisplayName("???????????? ?? 0 ?????? ???????????????????? ???????????? ???? ?????????????????????????? ?? shop_info")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.testMissingShopInfoFreeBill.before.csv",
            after = "WithdrawsBillingServiceTest.testMissingShopInfoFreeBill.after.csv"
    )
    @Test
    void testMissingShopInfoFreeBill() {
        withdrawsBillingService.process(LocalDate.of(2018, 5, 25));
    }

    @DisplayName("?????????????? ?????? ?????????????????????? ???????????? ???? ???????????? ????????????????")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.withdrawForMove.before.csv",
            after = "WithdrawsBillingServiceTest.withdrawForMove.after.csv"
    )
    @Test
    void test_withdrawForMove() {
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

    @DisplayName("?????????????? ?? ?????????? ???????????????????? ???????????????? ?? ?????????????? 0")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.withdrawForDisposal.before.csv",
            after = "WithdrawsBillingServiceTest.withdrawForDisposal.after.csv"
    )
    @Test
    void test_withdrawForDisposal() {
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

    @DisplayName("?????????????? ?? ?????????? ???????????????????????????? ???????????????? ?? ?????????????? 0")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.withdrawForInventory.before.csv",
            after = "WithdrawsBillingServiceTest.withdrawForInventory.after.csv"
    )
    @Test
    void test_withdrawForInventory() {
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

    @DisplayName("???????????? ?? ?????????????? ???????????? ??????????????????")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.withdrawFromRostov.before.csv",
            after = "WithdrawsBillingServiceTest.withdrawFromRostov.after.csv"
    )
    @Test
    void test_withdrawFromRostov() {
        mock(List.of(
                createTariff(
                        LocalDate.of(2017, 11, 1),
                        LocalDate.MAX,
                        1000L,
                        10000L,
                        2000
                )
        ));
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

    @DisplayName("???? ???????????? ?????????????? ???????????????????? ?????? ?????????? ?????????????? ???????????????? ??????????????????????????, ???????? ????????????????????")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.bigDimensions.before.csv",
            after = "WithdrawsBillingServiceTest.bigDimensions.after.csv"
    )
    @Test
    void test_bigDimensions() {
        mock(List.of(
            createTariff(
                    LocalDate.of(2017, 11, 1),
                    LocalDate.MAX,
                    1000L,
                    10000L,
                    2000
            ),
            createTariff(
                    LocalDate.of(2017, 11, 1),
                    LocalDate.MAX,
                    null,
                    null,
                    5000
            )
        ));
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

    @DisplayName("???????????? ???????????? ???? ???????????????? ???????? ??????????????????????, ?????????? ?????? ?? ?? ?????????????? ???????????? ?? ???????????? ???? ???????????? ??????????")
    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.failOnIncorrectDate.before.csv"
    )
    @Test
    void shouldFailOnDateVerification_whenGivenIncorrectDate() {
        IllegalStateException thrownException = Assertions.assertThrows(IllegalStateException.class,
                () -> withdrawsBillingService.process(LocalDate.of(2018, 5, 21)));
        assertEquals("Required date 2018-05-21 cannot be used.", thrownException.getMessage());
    }

    @DbUnitDataSet(
            before = "WithdrawsBillingServiceTest.promo.before.csv",
            after = "WithdrawsBillingServiceTest.promo.after.csv"
    )
    @Test
    void testSupplierWithPromo() {
        mock(List.of(
                createTariff(
                        LocalDate.of(2018, 2, 5),
                        LocalDate.of(2018, 11, 1),
                        null,
                        null,
                        2100
                ),
                createTariff(
                        LocalDate.of(2018, 11, 1),
                        LocalDate.MAX,
                        null,
                        null,
                        3000
                ),
                createTariff(
                        LocalDate.of(2018, 11, 1),
                        LocalDate.MAX,
                        1000L,
                        10000L,
                        2000
                )
        ));
        withdrawsBillingService.process(LocalDate.of(2018, 5, 21));
    }

}
