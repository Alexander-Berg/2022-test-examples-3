package ru.yandex.market.billing.commission;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.model.OrderItemBilledAmount;
import ru.yandex.market.core.fulfillment.model.BillingServiceType;
import ru.yandex.market.core.fulfillment.model.BillingUnit;
import ru.yandex.market.core.fulfillment.model.ValueType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DbOrderBilledAmountDaoTest extends FunctionalTest {

    private static final LocalDateTime TIMESTAMP = LocalDateTime.of(2018, 2, 10, 5, 2, 1);
    private static final LocalDate JANUARY_19 = LocalDate.of(2018, 1, 19);
    private static final LocalDate JANUARY_20 = LocalDate.of(2018, 1, 20);

    @Autowired
    private DbOrderBilledAmountDao dbOrderBilledAmountDao;

    @Test
    void testPersist() {
        List<OrderItemBilledAmount> testAmounts = Arrays.asList(
                OrderItemBilledAmount.builder()
                        .setOfferId("offer_id_3")
                        .setServiceType(BillingServiceType.FF_PROCESSING)
                        .setTrantime(LocalDateTime.now())
                        .setRawAmount(BigDecimal.ZERO)
                        .setCount(1)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .build(),
                OrderItemBilledAmount.builder()
                        .setOfferId("offer_id_4")
                        .setServiceType(BillingServiceType.FF_PROCESSING)
                        .setTrantime(LocalDateTime.now())
                        .setRawAmount(BigDecimal.ZERO)
                        .setCount(1)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .build()
        );

        DbOrderBilledAmountDao dao = mock(DbOrderBilledAmountDao.class);

        doCallRealMethod().when(dao).persist(anyList());

        doAnswer(invocation -> {
            List<OrderItemBilledAmount> orderItemBilledAmounts = invocation.getArgument(0);
            List<String> offerIds = orderItemBilledAmounts.stream()
                    .map(OrderItemBilledAmount::getOfferId)
                    .collect(Collectors.toList());

            assertThat(offerIds, containsInAnyOrder("offer_id_3", "offer_id_4"));

            return null;
        }).when(dao).persistOrderItemBilledAmounts(anyList());


        dao.persist(testAmounts);

        verify(dao, times(1)).persistOrderItemBilledAmounts(anyList());
    }

    @Test
    @DbUnitDataSet(
            before = "DbOrderBilledAmountDaoTest.testPersistOrderItemBilledAmounts.before.csv",
            after = "DbOrderBilledAmountDaoTest.testPersistOrderItemBilledAmounts.after.csv"
    )
    void testPersistOrderItemBilledAmounts() {
        final List<OrderItemBilledAmount> amounts = Arrays.asList(
                OrderItemBilledAmount.builder()
                        .setItemId(2)
                        .setOrderId(2)
                        .setFeedId(20)
                        .setOfferId("offerId_2")
                        .setSupplierId(3302)
                        .setServiceType(BillingServiceType.FF_CUSTOMER_RETURN)
                        .setTrantime(TIMESTAMP)
                        .setRawAmount(new BigDecimal(20065432).movePointLeft(5))
                        .setExchangeDate(JANUARY_19)
                        .setCount(1)
                        .setTariffValue(200)
                        .setTariffValueType(ValueType.RELATIVE)
                        .setTariffBillingUnit(BillingUnit.CUBIC_METER)
                        .build(),
                OrderItemBilledAmount.builder()
                        .setItemId(3)
                        .setOrderId(3)
                        .setFeedId(30)
                        .setOfferId("offerId_3")
                        .setSupplierId(3303)
                        .setServiceType(BillingServiceType.FF_STORAGE)
                        .setTrantime(TIMESTAMP)
                        .setRawAmount(new BigDecimal(3005678).movePointLeft(4))
                        .setExchangeDate(JANUARY_20)
                        .setCount(10)
                        .setTariffValue(300)
                        .setTariffValueType(ValueType.ABSOLUTE)
                        .setTariffBillingUnit(BillingUnit.ITEM)
                        .build()
        );

        dbOrderBilledAmountDao.persistOrderItemBilledAmounts(amounts);
    }
}
