package ru.yandex.market.checkout.pushapi.web;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.common.xml.outlets.OutletType;
import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OutletResponse;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartHelper;
import ru.yandex.market.checkout.pushapi.helpers.PushApiCartParameters;

import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CartCacheTest extends AbstractWebTestBase {

    @Autowired
    private PushApiCartHelper pushApiCartHelper;

    @Test
    public void shouldFillCachedValidOutletsXMLTest() throws Exception {
        PushApiCartParameters pushApiCartParameters = new PushApiCartParameters();
//        ShopOutletId{shopId=774, outletCode='20697'} exists in cache
        pushApiCartParameters.setShopId(774);
        mockSettingsForDifferentParameters(pushApiCartParameters);
        CartResponse cart = pushApiCartHelper.cart(pushApiCartParameters);
        checkOutletResponses(cart);
    }

    private void checkOutletResponses(CartResponse cart) {
        for (DeliveryResponse deliveryOption : cart.getDeliveryOptions()) {
            List<OutletResponse> outletResponses = deliveryOption.getOutletResponses();
            Set<Long> outletCodes = Stream.concat(
                    Optional.ofNullable(deliveryOption.getOutletCodes()).orElse(Collections.emptySet()).stream()
                            .map(Long::parseLong),
                    Optional.ofNullable(deliveryOption.getOutletIdsSet()).orElse(Collections.emptySet()).stream()
            ).collect(Collectors.toSet());
            if (isEmpty(outletCodes)) {
                assertTrue(isEmpty(outletResponses));
                continue;
            }

            if (outletCodes.contains(20697L)) {
                assertThat(outletResponses, hasSize(1));
                OutletResponse outletResponse = outletResponses.get(0);
                assertThat(outletResponse.getMarketOutletId(), is(419585L));
                assertThat(outletResponse.getShopOutletCode(), is("20697"));
                assertThat(outletResponse.getDeliveryServiceId(), is(0L));
                assertThat(outletResponse.getType(), is(OutletType.DEPOT));
            } else {
                assertTrue(isEmpty(outletResponses));
            }
        }
    }


}
