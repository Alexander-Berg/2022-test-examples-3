package ru.yandex.market.checkout.checkouter.order;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.test.providers.AddressProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.common.report.model.MarketSearchRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ActualDeliveryRequestBuilderTest {

    @Test
    void withOrderMustExtractRegionFromOrderFromBuyerAddressIfExist() {
        Order order = OrderProvider.orderBuilder()
                .deliveryBuilder(
                        DeliveryProvider.deliveryBuilder()
                                .buyerAddress(AddressProvider.getAddress(address -> address.setPreciseRegionId(321L)))
                                .regionId(777L)
                ).build();
        MarketSearchRequest request = new ActualDeliveryRequestBuilder()
                .withOrder(order)
                .build();

        assertThat(request.getRegionId(), is(321L));
    }

    @Test
    void withOrderMustExtractRegionFromDeliveryWhenBuyerAddressNotExist() {
        Order order = OrderProvider.orderBuilder()
                .deliveryBuilder(
                        DeliveryProvider.deliveryBuilder()
                                .buyerAddress(AddressProvider.getAddress(address -> address.setPreciseRegionId(null)))
                                .regionId(777L)
                ).build();
        MarketSearchRequest request = new ActualDeliveryRequestBuilder()
                .withOrder(order)
                .build();

        assertThat(request.getRegionId(), is(777L));
    }

    @Test
    void withOrderInRegionMustSetSpecifyRegion() {
        Order order = OrderProvider.orderBuilder()
                .deliveryBuilder(DeliveryProvider.deliveryBuilder().regionId(777L))
                .build();
        MarketSearchRequest request = new ActualDeliveryRequestBuilder()
                .withOrderInRegion(order, 16L)
                .build();

        assertThat(request.getRegionId(), is(16L));
    }

}
