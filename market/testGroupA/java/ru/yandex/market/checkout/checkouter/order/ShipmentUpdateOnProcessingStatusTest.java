package ru.yandex.market.checkout.checkouter.order;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.status.actions.UpdateShipmentStatusAction;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.helpers.utils.ResultActionsContainer;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;

//ignored until 'remove items' is refactored and supported in production
@Disabled
public class ShipmentUpdateOnProcessingStatusTest extends AbstractWebTestBase {

    @Autowired
    protected WireMockServer reportMock;
    @Autowired
    private UpdateShipmentStatusAction action;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    private Order order;

    @BeforeEach
    public void setUp() {
        freezeTime();
        action.setClock(getClock());
        Instant supplierShipmentDateTime = Instant.parse("2019-10-14T13:40:00Z");
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withSupplierShipmentDateTime(supplierShipmentDateTime)
                .withDeliveryType(DeliveryType.PICKUP)
                .withPartnerInterface(true)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getReportParameters().setDeliveryMethods(null);

        OrderItem firstItem = parameters.getOrder().getItems().iterator().next();
        firstItem.setWarehouseId(145);
        firstItem.setWeight(1L);
        firstItem.setHeight(1L);
        firstItem.setWidth(1L);
        firstItem.setDepth(1L);

        order = orderCreateHelper.createOrder(parameters);
        orderPayHelper.payForOrder(order);
        order = orderService.getOrder(order.getId());
        assertThat(order.getStatus(), is(OrderStatus.PENDING));
        reportMock.resetRequests();
    }

    @Test
    public void shouldActualizeDeliveryRouteOnOrderStatusUpdate() {
        jumpToFuture(1, DAYS);
        orderStatusHelper.updateOrderStatus(order.getId(), ClientInfo.SYSTEM, OrderStatus.PROCESSING, null,
                new ResultActionsContainer()
                        .andExpect(status().is(400))
                        .andExpect(content().json("{\"status\":400," +
                                "\"code\":\"CURRENT_DELIVERY_OPTION_EXPIRED\"," +
                                "\"message\":\"Order " + order.getId() + " has expired delivery option.\"}")),
                null
        );

        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        Collection<ServeEvent> actualDeliveryCalls = serveEvents.stream()
                .filter(
                        se -> se.getRequest()
                                .queryParameter("place")
                                .containsValue(MarketReportPlace.ACTUAL_DELIVERY.getId())
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("ignore-has-gone")
                                .containsValue("1")
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("client")
                                .containsValue("checkout")
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("co-from")
                                .containsValue("checkouter")
                )
                .filter(
                        se -> se.getRequest()
                                .queryParameter("pp")
                                .containsValue("18")
                )
                .collect(Collectors.toList());

        assertThat(actualDeliveryCalls, hasSize(greaterThanOrEqualTo(1)));

        Collection<ServeEvent> actualDeliveryCallsWithWarehouseId = actualDeliveryCalls.stream().filter(
                        se -> se.getRequest()
                                .queryParameter("offers-list")
                                .values().stream().anyMatch(o -> o.contains("wh:145")))
                .collect(Collectors.toList());

        assertThat(actualDeliveryCallsWithWarehouseId, hasSize(greaterThanOrEqualTo(1)));


        assertThat(
                serveEvents.stream()
                        .filter(
                                se -> se.getRequest()
                                        .queryParameter("place")
                                        .containsValue(MarketReportPlace.OUTLETS.getId())
                        )
                        .collect(Collectors.toList()),
                hasSize(equalTo(0))
        );
    }

    @Test
    public void shipmentActualizationShouldNotPerformedOnOrderStatusUpdate() {
        orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
        Collection<ServeEvent> serveEvents = reportMock.getServeEvents().getServeEvents();
        assertThat(serveEvents, empty());
    }

    @Test
    public void shouldUpdateSupplierShipmentDateTimeOnOrderStatusUpdate() throws IOException {
        Instant newSupplierShipmentDateTime = Instant.parse("2019-10-14T15:40:00Z");
        Parameters parameters = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withDeliveryServiceId(MOCK_DELIVERY_SERVICE_ID)
                .withSupplierShipmentDateTime(newSupplierShipmentDateTime)
                .withDeliveryType(DeliveryType.PICKUP)
                .withPartnerInterface(true)
                .withColor(Color.BLUE)
                .buildParameters();
        parameters.getReportParameters().setDeliveryMethods(null);
        orderCreateHelper.initializeMock(parameters);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);
    }
}
