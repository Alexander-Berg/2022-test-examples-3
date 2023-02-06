package ru.yandex.travel.orders.services.finances.billing;

import java.util.UUID;

import org.mockito.Mockito;

import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.finances.BillingTransaction;
import ru.yandex.travel.orders.entities.finances.FinancialEvent;

import static org.mockito.Mockito.when;

class BillingTransactionTestHelper {
    static BillingTransaction.BillingTransactionBuilder mockTransactionBuilder(long id) {
        OrderItem orderItem = Mockito.mock(OrderItem.class);
        when(orderItem.getLogEntityId()).thenReturn(UUID.randomUUID());
        return BillingTransaction.builder()
                .id(id)
                .sourceFinancialEvent(FinancialEvent.builder()
                        .orderItem(orderItem)
                        .build());
    }
}
