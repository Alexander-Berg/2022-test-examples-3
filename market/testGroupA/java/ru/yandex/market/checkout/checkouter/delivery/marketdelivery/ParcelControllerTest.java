package ru.yandex.market.checkout.checkouter.delivery.marketdelivery;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.ParcelHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.order.Color.BLUE;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_SORTING_CENTER_HARDCODED;
import static ru.yandex.market.checkout.test.providers.OrderProvider.SHOP_ID_WITH_SORTING_CENTER;

/**
 * @author mmetlov
 */
public class ParcelControllerTest extends AbstractWebTestBase {

    @Autowired
    protected OrderPayHelper orderPayHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private ParcelHelper parcelHelper;

    @BeforeEach
    public void prepare() {
        trustMockConfigurer.mockWholeTrust();
    }


    /**
     * MARKETCHECKOUT-5227: При изменении даты у заказа в отгузке, котороая едет через СЦ,
     * не сбрасывается трек-код от СЦ.
     */


    @Test
    public void shouldUpdateParcelDeliveredAt() throws Exception {
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withDeliveryType(DeliveryType.DELIVERY)
                .withShopId(SHOP_ID_WITH_SORTING_CENTER)
                .withColor(BLUE)
                .buildParameters();

        parameters.getReportParameters().getOrder().getItems()
                .forEach(oi -> oi.setWarehouseId(MOCK_SORTING_CENTER_HARDCODED.intValue()));

        Order order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);

        Optional<Parcel> originParcelOptional = order.getDelivery().getParcels().stream().findFirst();

        assertTrue(originParcelOptional.isPresent());
        Parcel originParcel = originParcelOptional.get();
        assertNull(originParcel.getDeliveredAt());

        Instant expectedDeliveredAtTS = Instant.now();
        // action
        parcelHelper.updateParcelDeliveredAt(order, originParcel, expectedDeliveredAtTS)
                .andExpect(status().isOk());

        // check
        Order updatedOrder = orderService.getOrder(order.getId());
        Optional<Parcel> updatedOptionalParcel = updatedOrder.getDelivery().getParcels()
                .stream()
                .filter(p -> p.getId().equals(originParcel.getId()))
                .findAny();
        assertTrue(updatedOptionalParcel.isPresent());

        Parcel updatedParcel = updatedOptionalParcel.get();

        assertNotNull(updatedParcel.getDeliveredAt());
        assertEquals(
                updatedParcel.getDeliveredAt().truncatedTo(ChronoUnit.SECONDS),
                expectedDeliveredAtTS.truncatedTo(ChronoUnit.SECONDS)
        );
    }
}
