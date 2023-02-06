package ru.yandex.market.checkout.checkouter.trace;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.order.OrderSortingType.BY_DATE;
import static ru.yandex.market.checkout.checkouter.order.OrderSortingType.BY_IMPORTANCE;

public class GetOrdersByUidContextHolderTest {

    private final GetOrdersByUidContextHolder.GetOrdersByUidContextAttributesHolder holder;

    public GetOrdersByUidContextHolderTest() {
        this.holder = new GetOrdersByUidContextHolder.GetOrdersByUidContextAttributesHolder();
    }

    @BeforeEach
    void setUp() {
        holder.clear();
    }

    @Test
    public void holderShouldKeepSortingAttribute() {
        assertEquals(holder.getAttributes().size(), 0);

        GetOrdersByUidContextHolder.setOrderSortingType(BY_DATE);
        assertEquals(holder.getAttributes().get("orderSortingType"), BY_DATE);

        GetOrdersByUidContextHolder.setOrderSortingType(BY_IMPORTANCE);
        assertEquals(holder.getAttributes().get("orderSortingType"), BY_IMPORTANCE);
    }
}
