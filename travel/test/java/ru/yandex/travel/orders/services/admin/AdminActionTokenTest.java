package ru.yandex.travel.orders.services.admin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.commons.proto.ErrorException;
import ru.yandex.travel.hotels.common.orders.ExpediaHotelItinerary;
import ru.yandex.travel.hotels.common.orders.OrderDetails;
import ru.yandex.travel.orders.admin.proto.TAdminActionToken;
import ru.yandex.travel.orders.entities.ExpediaOrderItem;
import ru.yandex.travel.orders.entities.HotelOrder;
import ru.yandex.travel.orders.entities.Invoice;
import ru.yandex.travel.orders.entities.Order;
import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.orders.entities.TrustInvoice;
import ru.yandex.travel.orders.services.TokenEncrypter;
import ru.yandex.travel.orders.services.TokenEncrypterProperties;

public class AdminActionTokenTest {
    private TokenEncrypter tokenEncrypter;
    private AdminActionTokenService adminActionTokenService;

    @Before
    public void setUp() {
        TokenEncrypterProperties tokenEncrypterProperties = new TokenEncrypterProperties();
        tokenEncrypterProperties.setSecretKey("random");

        tokenEncrypter = new TokenEncrypter(tokenEncrypterProperties);
        adminActionTokenService = new AdminActionTokenService(tokenEncrypter);
    }

    private static Order orderFactory() {
        HotelOrder order = new HotelOrder();
        order.setId(UUID.randomUUID());
        order.setVersion(1);
        order.setServicedAt(LocalDateTime.now());

        ExpediaOrderItem item = new ExpediaOrderItem();
        item.setId(UUID.randomUUID());
        item.setVersion(1);
        ExpediaHotelItinerary itinerary = new ExpediaHotelItinerary();
        OrderDetails orderDetails = OrderDetails.builder().checkinDate(LocalDate.now()).build();
        itinerary.setOrderDetails(orderDetails);
        item.setItinerary(itinerary);
        order.addOrderItem(item);

        TrustInvoice trustInvoice = new TrustInvoice();
        trustInvoice.setId(UUID.randomUUID());
        trustInvoice.setVersion(1);
        order.addInvoice(trustInvoice);

        return order;
    }

    private static Order changeOrder(Order targetOrder) {
        HotelOrder order = new HotelOrder();
        order.setId(targetOrder.getId());
        order.setVersion(targetOrder.getVersion() + 1);

        OrderItem item = targetOrder.getOrderItems().get(0);
        item.setVersion(item.getVersion() + 1);
        order.addOrderItem(item);

        Invoice trustInvoice = targetOrder.getInvoices().get(0);
        trustInvoice.setVersion(targetOrder.getVersion() + 1);
        order.addInvoice(trustInvoice);

        return order;
    }

    @Test
    public void checkOrderTokenSuccess() {
        Order order = orderFactory();
        TAdminActionToken token = adminActionTokenService.buildOrderToken(order);

        String tokenView = tokenEncrypter.toAdminActionToken(token);

        adminActionTokenService.checkOrderToken(order, tokenView);
    }

    @Test
    public void checkOrderTokenFailed() {
        Order order = orderFactory();
        TAdminActionToken token = adminActionTokenService.buildOrderToken(order);

        String tokenView = tokenEncrypter.toAdminActionToken(token);
        Order changedOrder = changeOrder(order);

        Assertions.assertThatThrownBy(() -> adminActionTokenService.checkOrderToken(changedOrder, tokenView))
                .isInstanceOf(ErrorException.class)
                .hasMessageContaining("Action admin token is invalid");
    }
}
