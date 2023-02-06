package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.db.SortingInfo;
import ru.yandex.common.util.db.SortingOrder;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.FoundOfferBuilder;
import ru.yandex.market.common.report.model.resale.ResaleSpecs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.checkouter.event.HistorySortingField.DATE;
import static ru.yandex.market.checkout.test.providers.ResaleSpecsProvider.getResaleSpecs;


public class CreateOrderWithResaleItems extends AbstractWebTestBase {

    @Test
    public void shouldReturnItemsWithResaleSpecsIfResaleOrderOptionalPartWasSet() {
        OrderItem firstItem = OrderItemProvider.defaultOrderItem();
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem);
        params.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .resaleSpecs(getResaleSpecs(null)).build()));
        Order createdOrder = orderCreateHelper.createOrder(params);

        Order order = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.RESALE_SPECS));
        ResaleSpecs actualResaleSpecs = order.getItems().iterator().next().getResaleSpecs();
        assertEquals(getResaleSpecs(actualResaleSpecs.getId()), actualResaleSpecs);
    }

    @Test
    public void shouldNotReturnItemsWithResaleSpecsIfResaleOrderOptionalPartWasNotSet() {
        OrderItem firstItem = OrderItemProvider.defaultOrderItem();
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem);
        params.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .resaleSpecs(getResaleSpecs(null)).build()));
        Order createdOrder = orderCreateHelper.createOrder(params);

        Order order = orderService.getOrder(createdOrder.getId(), ClientInfo.SYSTEM,
                null);
        ResaleSpecs actualResaleSpecs = order.getItems().iterator().next().getResaleSpecs();
        assertNull(actualResaleSpecs);
    }

    @Test
    public void shouldReturnResaleSpecsInEventWhenResaleOrderOptionalPartWasSet() {
        OrderItem firstItem = OrderItemProvider.defaultOrderItem();
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem);
        params.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .resaleSpecs(getResaleSpecs(null)).build()));
        Order order = orderCreateHelper.createOrder(params);

        OrderHistoryEvent event = Iterables.getLast(eventService.getPagedOrderHistoryEvents(
                order.getId(),
                Pager.atPage(1, 1),
                new SortingInfo<>(DATE, SortingOrder.ASC),
                null,
                null,
                false,
                ClientInfo.SYSTEM,
                Set.of(OptionalOrderPart.RESALE_SPECS)
        ).getItems());
        ResaleSpecs actualResaleSpecs = event.getOrderAfter().getItems().iterator().next().getResaleSpecs();
        assertEquals(getResaleSpecs(actualResaleSpecs.getId()), actualResaleSpecs);
    }

    @Test
    public void shouldNotReturnResaleSpecsInEventWhenResaleOrderOptionalPartWasNotSet() {
        OrderItem firstItem = OrderItemProvider.defaultOrderItem();
        Parameters params = BlueParametersProvider.defaultBlueOrderParametersWithItems(firstItem);
        params.getReportParameters().setOffers(List.of(FoundOfferBuilder.createFrom(firstItem)
                .resaleSpecs(getResaleSpecs(null)).build()));
        Order order = orderCreateHelper.createOrder(params);

        OrderHistoryEvent event = Iterables.getLast(eventService.getPagedOrderHistoryEvents(
                order.getId(),
                Pager.atPage(1, 1),
                new SortingInfo<>(DATE, SortingOrder.ASC),
                null,
                null,
                false,
                ClientInfo.SYSTEM,
                null
        ).getItems());
        ResaleSpecs actualResaleSpecs = event.getOrderAfter().getItems().iterator().next().getResaleSpecs();
        assertNull(actualResaleSpecs);
    }
}

