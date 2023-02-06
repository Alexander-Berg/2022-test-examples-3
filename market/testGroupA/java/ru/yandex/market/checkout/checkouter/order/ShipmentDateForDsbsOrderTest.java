package ru.yandex.market.checkout.checkouter.order;

import java.time.ZoneId;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CheckoutParametersBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.order.Color.WHITE;
import static ru.yandex.market.checkout.providers.WhiteParametersProvider.defaultWhiteParameters;
import static ru.yandex.market.checkout.util.OrderUtils.firstOrder;

@DisplayName("Тест на работу даты отгрузки для ДБС")
public class ShipmentDateForDsbsOrderTest extends AbstractWebTestBase {

    @Test
    @DisplayName("Дата отгрузки проставляется в РДД, если не приходит от партнера по /order/accept")
    void testShipmentDateSetToDefault() {
        var parameters = defaultWhiteParameters();
        var cart = orderCreateHelper.cart(parameters);
        var multiOrder = orderCreateHelper.mapCartToOrder(cart, parameters);
        pushApiConfigurer.mockAccept(multiOrder.getCarts().get(0), true);
        var multiOrderAfterCheckout = client.checkout(multiOrder,
                CheckoutParametersBuilder.aCheckoutParameters()
                        .withUid(parameters.getBuyer().getUid())
                        .withContext(Context.MARKET)
                        .withHitRateGroup(HitRateGroup.UNLIMIT)
                        .withApiSettings(ApiSettings.PRODUCTION)
                        .withRgb(WHITE)
                        .build());
        var actualOrderFromDatabase = orderService.getOrder(firstOrder(multiOrderAfterCheckout).getId());

        assertNotNull(actualOrderFromDatabase);
        //проверяем, что для ДБС парсел создался, и дата отгрузки = РДД, так как
        assertTrue(CollectionUtils.isNotEmpty(actualOrderFromDatabase.getDelivery().getParcels()));
        assertEquals(actualOrderFromDatabase.getDelivery()
                        .getDeliveryDates()
                        .getFromDate()
                        .toInstant()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                actualOrderFromDatabase.getDelivery()
                        .getParcels()
                        .get(0)
                        .getShipmentDate()
        );
    }
}
