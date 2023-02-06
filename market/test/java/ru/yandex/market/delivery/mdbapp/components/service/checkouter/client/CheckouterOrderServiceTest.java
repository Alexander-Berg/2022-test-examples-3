package ru.yandex.market.delivery.mdbapp.components.service.checkouter.client;

import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.delivery.mdbapp.integration.converter.ChangeRequestStatusConverter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class CheckouterOrderServiceTest {

    @Rule
    public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

    private final CheckouterAPI checkouterClientMock = mock(CheckouterAPI.class);
    private final ChangeRequestStatusConverter changeRequestStatusConverter = mock(ChangeRequestStatusConverter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CheckouterOrderService orderService;
    private static final Long ORDER_ID = 12L;

    @Before
    public void setUp() {
        orderService = new CheckouterOrderService(checkouterClientMock, changeRequestStatusConverter);
    }

    @Test
    public void testOrderFound() {
        Mockito.when(checkouterClientMock.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
            .thenReturn(new Order());

        Order order = orderService.getOrder(ORDER_ID);
        softly.assertThat(order).as("Proper method should return empty order")
            .isNotNull();
    }

    @Test(expected = OrderNotFoundException.class)
    public void testOrderNotFound() {
        Mockito.when(checkouterClientMock.getOrder(ORDER_ID, ClientRole.SYSTEM, null))
            .thenReturn(null);

        orderService.getOrder(ORDER_ID);
    }

    @Test
    public void getRouteFromParcel() throws JsonProcessingException {
        Parcel parcel = new Parcel();
        parcel.setRoute(getRoute());
        softly.assertThat(orderService.getRoute(parcel, 1L)).isEqualTo(parcel.getRoute());
        verifyNoMoreInteractions(checkouterClientMock);
    }

    @Test
    public void getRouteFromOrder() throws JsonProcessingException {
        Order order = getCheckouterOrder();
        when(checkouterClientMock.getOrder(1L, ClientRole.SYSTEM, null))
            .thenReturn(order);
        Parcel parcel = new Parcel();
        parcel.setRoute(null);
        softly.assertThat(orderService.getRoute(parcel, 1L))
            .isEqualTo(order.getDelivery().getParcels().get(0).getRoute());
        verify(checkouterClientMock).getOrder(1L, ClientRole.SYSTEM, null);
        verifyNoMoreInteractions(checkouterClientMock);
    }

    @Nonnull
    private Order getCheckouterOrder() throws JsonProcessingException {
        Order order = new Order();
        order.setId(1L);
        Parcel parcel = new Parcel();
        parcel.setRoute(getRoute());
        Delivery delivery = new Delivery();
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }

    @Nonnull
    private JsonNode getRoute() throws JsonProcessingException {
        return objectMapper.readTree(
            "\"route\": {\n" +
                "\"points\": [],\n" +
                "\"paths\": [],\n" +
                "\"tariff_id\": 4181,\n" +
                "\"cost_for_shop\": 249,\n" +
                "\"date_from\": {\n" +
                "  \"day\": 16,\n" +
                "  \"month\": 6,\n" +
                "  \"year\": 2020\n" +
                "},\n" +
                "\"date_to\": {\n" +
                "  \"day\": 17,\n" +
                "  \"month\": 6,\n" +
                "  \"year\": 2020\n" +
                "}\n" +
                "}"
        );
    }
}
