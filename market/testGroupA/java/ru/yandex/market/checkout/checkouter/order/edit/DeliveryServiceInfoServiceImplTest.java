package ru.yandex.market.checkout.checkouter.order.edit;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeliveryServiceInfoServiceImplTest extends AbstractWebTestBase {

    @Autowired
    private DeliveryServiceInfoService deliveryServiceInfoService;

    @Test
    void getPossibleOrderOptionsShouldNotReturnOptions() {
        // Этих служб нет в файле
        final Set<Long> deliveryServiceIds = Set.of(1005288L, 1005111L, 1005363L, 1005453L, 1005393L);
        deliveryServiceIds.forEach(id -> {
            final List<PossibleOrderOption> options = deliveryServiceInfoService.getPossibleOrderOptions(id);
            assertNotNull(options);
            assertThat(options, empty());
        });
    }

    @Test
    void getPossibleOrderOptionsShouldReturnOptions() {
        final List<PossibleOrderOption> options = deliveryServiceInfoService.getPossibleOrderOptions(
                DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID);
        assertNotNull(options);
        assertThat(options, hasSize(1));
    }

    @Test
    void shouldShowPhonesAndSite() {
        final DeliveryServiceCustomerInfo first = deliveryServiceInfoService.getDeliveryServiceCustomerInfoById(
                DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        assertNotNull(first);
        assertThat(first.getPhones(), containsInAnyOrder("+7-(912)-345-67-89", "+7-(912)-345-67-88"));
        assertEquals("www.partner100501-site.ru", first.getTrackOrderSite());

        final DeliveryServiceCustomerInfo second = deliveryServiceInfoService.getDeliveryServiceCustomerInfoById(
                DeliveryProvider.ANOTHER_MOCK_DELIVERY_SERVICE_ID);
        assertNotNull(second);
        assertThat(second.getPhones(), contains("+7-(912)-345-67-80"));
        assertEquals("www.partner2-site.ru", second.getTrackOrderSite());
    }
}
