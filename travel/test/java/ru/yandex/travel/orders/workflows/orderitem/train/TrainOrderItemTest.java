package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;

import org.javamoney.moneta.Money;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.orders.factories.TrainOrderItemFactory;
import ru.yandex.travel.orders.workflow.orderitem.generic.proto.EOrderItemState;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainOrderItemTest {
    @Test
    public void testCalculateTotalCost() {
        var factory = new TrainOrderItemFactory();
        factory.setOrderItemState(EOrderItemState.IS_RESERVED);
        factory.setTariffAmount(Money.of(BigDecimal.valueOf(3.03), ProtoCurrencyUnit.RUB));
        factory.setServiceAmount(Money.of(BigDecimal.valueOf(40.4), ProtoCurrencyUnit.RUB));
        factory.setFeeAmount(Money.of(BigDecimal.valueOf(100.0), ProtoCurrencyUnit.RUB));
        var trainOrderItem = factory.createTrainOrderItem();

        assertThat(trainOrderItem.totalCostAfterReservation()).isEqualTo(Money.of(BigDecimal.valueOf(143.43), ProtoCurrencyUnit.RUB));
    }
}
