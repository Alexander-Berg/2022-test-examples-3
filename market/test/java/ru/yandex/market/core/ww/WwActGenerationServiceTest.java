package ru.yandex.market.core.ww;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelBox;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPrices;
import ru.yandex.market.core.ww.exception.WwEmptyBoxesException;
import ru.yandex.market.logistics.werewolf.model.entity.DocOrder;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class WwActGenerationServiceTest {

    @Test
    @DisplayName("Verify that parcel box count is required to be greater than 0")
    public void convertToDocOrderThrowsEmptyBoxException() {
        var order = new Order();
        var delivery = new Delivery();
        var parcel = new Parcel();
        parcel.setWeight(1000L);
        parcel.setBoxes(emptyList());
        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);
        //
        assertThatThrownBy(() -> WwActGenerationService.convertToDocOrder(order))
                .isInstanceOf(WwEmptyBoxesException.class);
    }

    @Test
    @DisplayName("Verify that orders with empty subsidies return base itemTotal price")
    public void convertToDocOrderNoPromos() {
        var order = getBasicOrder();
        assertThat(WwActGenerationService.convertToDocOrder(order))
                .isNotNull()
                .extracting(DocOrder::getYandexId, DocOrder::getPartnerId, DocOrder::getAssessedCost,
                        DocOrder::getWeight, DocOrder::getPlacesCount)
                .containsExactly("12345", "777", BigDecimal.valueOf(3300), BigDecimal.valueOf(1.0), 1);
    }

    @Test
    @DisplayName("Verify that orders with empty shopOrderId retuns correct shopOrderId for act generation")
    public void convertToDocOrderNoShopOrderId() {
        var order = getBasicOrderWithoutShopOrderId();
        assertThat(WwActGenerationService.convertToDocOrder(order))
                .isNotNull()
                .extracting(DocOrder::getYandexId, DocOrder::getPartnerId, DocOrder::getAssessedCost,
                        DocOrder::getWeight, DocOrder::getPlacesCount)
                .containsExactly("777", "777", BigDecimal.valueOf(3300), BigDecimal.valueOf(1.0), 1);
    }

    @Test
    @DisplayName("Verify that orders with non-null promo field field return base price + totalSubsidy as a total price")
    public void convertToDocOrderPromos() {
        var order = getBasicOrder();
        var promoPrices = new OrderPrices();
        promoPrices.setSubsidyTotal(BigDecimal.valueOf(198));
        when(order.getTotalWithSubsidy()).thenReturn(null);
        when(order.getPromoPrices()).thenReturn(promoPrices);
        assertThat(WwActGenerationService.convertToDocOrder(order))
                .isNotNull()
                .extracting(DocOrder::getYandexId, DocOrder::getPartnerId, DocOrder::getAssessedCost,
                        DocOrder::getWeight, DocOrder::getPlacesCount)
                .containsExactly("12345", "777", BigDecimal.valueOf(3498), BigDecimal.valueOf(1.0), 1);
    }

    private Order getBasicOrder() {
        var order = new Order();
        var delivery = new Delivery();
        var parcel = new Parcel();
        parcel.setWeight(1000L);
        parcel.setBoxes(Collections.singletonList(new ParcelBox()));
        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);
        order.setItemsTotal(BigDecimal.valueOf(3300));
        order.setShopOrderId("12345");
        order.setId(777L);
        return Mockito.spy(order);
    }

    private Order getBasicOrderWithoutShopOrderId() {
        var order = new Order();
        var delivery = new Delivery();
        var parcel = new Parcel();
        parcel.setWeight(1000L);
        parcel.setBoxes(Collections.singletonList(new ParcelBox()));
        delivery.setParcels(Collections.singletonList(parcel));
        order.setDelivery(delivery);
        order.setItemsTotal(BigDecimal.valueOf(3300));
        order.setId(777L);
        return Mockito.spy(order);
    }
}
