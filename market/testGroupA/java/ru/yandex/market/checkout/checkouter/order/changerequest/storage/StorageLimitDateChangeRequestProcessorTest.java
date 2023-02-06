package ru.yandex.market.checkout.checkouter.order.changerequest.storage;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryServiceType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderEditRequest;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.exception.ChangeRequestNotAppliedException;
import ru.yandex.market.checkout.checkouter.trace.OrderEditContextHolder;
import ru.yandex.market.checkout.helpers.NotifyTracksHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.providers.DeliveryTrackProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.checkout.util.tracker.MockTrackerHelper;

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StorageLimitDateChangeRequestProcessorTest extends AbstractWebTestBase {

    @Autowired
    private WireMockServer pvzMock;
    @Autowired
    private WireMockServer postamatMock;
    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;
    @Autowired
    private NotifyTracksHelper notifyTracksHelper;
    @Autowired
    private WireMockServer trackerMock;
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private Clock clock;

    private final OrderEditContextHolder.OrderEditContextAttributesHolder editContext =
            new OrderEditContextHolder.OrderEditContextAttributesHolder();

    @AfterEach
    public void clearOrderEditContext() {
        editContext.clear();
        pvzMock.resetAll();
        postamatMock.resetAll();
    }

    @Test
    public void shouldChangeStorageLimitDateForPickupOutlet() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        pvzMock.stubFor(
                patch(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        var track = new Track("0000174309", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID);
        track.setShipmentId(order.getDelivery().getParcels().get(0).getId());
        track.setDeliveryServiceType(DeliveryServiceType.CARRIER);

        orderDeliveryHelper.addTrack(order.getId(), order.getDelivery().getParcels().get(0).getId(), track,
                ClientInfo.SYSTEM);
        MockTrackerHelper.mockGetDeliveryServices(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID, trackerMock);
        MockTrackerHelper.mockPutTrack(trackerMock, MockTrackerHelper.TRACKER_ID);
        tmsTaskHelper.runRegisterDeliveryTrackTaskV2();

        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 30, 123L));
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 101,
                123L));
        notifyTracksHelper.notifyTracks(DeliveryTrackProvider.getDeliveryTrack(MockTrackerHelper.TRACKER_ID, 50, 123L));

        order = orderService.getOrder(order.getId());

        var delivery = order.getDelivery();
        delivery.setOutletStoragePeriod(5);
        delivery.setOutletStorageLimitDate(LocalDate.ofInstant(
                delivery.getDeliveryDates().getFromDate().toInstant(), clock.getZone())
                .plusDays(delivery.getOutletStoragePeriod()));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        var oldDate = order.getDelivery().getOutletStorageLimitDate();
        var oldPeriod = order.getDelivery().getOutletStoragePeriod();
        var newDate = oldDate.plusDays(3L);
        var newPeriod = oldPeriod + 3;

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(newDate);
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);
        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(Color.BLUE),
                orderEditRequest);

        assertNotNull(changeRequests);
        var cr = changeRequests.stream().filter(ch ->
                ch.getType() == ChangeRequestType.STORAGE_LIMIT_DATE
                        && ch.getStatus() == ChangeRequestStatus.APPLIED)
                .findAny().orElse(null);
        assertNotNull(cr);
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getNewDate().isEqual(newDate));
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getOldDate().isEqual(oldDate));
        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(newDate));
        assertEquals(newPeriod, order.getDelivery().getOutletStoragePeriod());
    }

    @Test
    public void shouldChangeStorageLimitDateForPostTermOutlet() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPostTerm(DeliveryProvider.MOCK_POST_TERM_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        postamatMock.stubFor(
                patch(urlPathEqualTo("/boxbot/api/pincode/logistics/orders/" + order.getId() + "/reschedule" +
                        "-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order, new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());

        var delivery = order.getDelivery();
        delivery.setOutletStoragePeriod(5);
        delivery.setOutletStorageLimitDate(LocalDate.ofInstant(
                delivery.getDeliveryDates().getFromDate().toInstant(), clock.getZone())
                .plusDays(delivery.getOutletStoragePeriod()));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        var oldDate = order.getDelivery().getOutletStorageLimitDate();
        var oldPeriod = order.getDelivery().getOutletStoragePeriod();
        var newDate = oldDate.plusDays(3L);
        var newPeriod = oldPeriod + 3;

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(newDate);
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);
        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(Color.BLUE),
                orderEditRequest);

        assertNotNull(changeRequests);
        var cr = changeRequests.stream().filter(ch ->
                ch.getType() == ChangeRequestType.STORAGE_LIMIT_DATE
                        && ch.getStatus() == ChangeRequestStatus.APPLIED)
                .findAny().orElse(null);
        assertNotNull(cr);
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getNewDate().isEqual(newDate));
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getOldDate().isEqual(oldDate));
        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(newDate));
        assertEquals(newPeriod, order.getDelivery().getOutletStoragePeriod());
    }

    @Test
    public void shouldNotChangeStorageLimitDateForProcessingOrder() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        pvzMock.stubFor(
                patch(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order, new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(LocalDate.now().plusDays(3L));
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);
        var orderId = order.getId();
        var uid = order.getBuyer().getUid();
        assertThrows(Exception.class, () -> client.editOrder(
                orderId,
                ClientRole.USER,
                uid,
                List.of(Color.BLUE),
                orderEditRequest));
    }

    @Test
    public void shouldNotChangeStorageLimitDateIfNewDateIsEarlierThenCurrent() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        pvzMock.stubFor(
                patch(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order, new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());

        var delivery = order.getDelivery();
        delivery.setOutletStoragePeriod(5);
        delivery.setOutletStorageLimitDate(LocalDate.ofInstant(
                delivery.getDeliveryDates().getFromDate().toInstant(), clock.getZone())
                .plusDays(delivery.getOutletStoragePeriod()));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        var oldDate = order.getDelivery().getOutletStorageLimitDate();
        var oldPeriod = order.getDelivery().getOutletStoragePeriod();
        var newDate = oldDate.minusDays(3L);

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(newDate);
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);

        var orderId = order.getId();
        var uid = order.getBuyer().getUid();
        assertThrows(ChangeRequestNotAppliedException.class, () -> client.editOrder(
                orderId,
                ClientRole.USER,
                uid,
                List.of(Color.BLUE),
                orderEditRequest));

        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(oldDate));
        assertEquals(oldPeriod, order.getDelivery().getOutletStoragePeriod());
    }

    @Test
    public void shouldNotChangeStorageLimitDateIfNewDateIsEqualToCurrent() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        pvzMock.stubFor(
                patch(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order, new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());

        var delivery = order.getDelivery();
        delivery.setOutletStoragePeriod(5);
        delivery.setOutletStorageLimitDate(LocalDate.ofInstant(
                delivery.getDeliveryDates().getFromDate().toInstant(), clock.getZone())
                .plusDays(delivery.getOutletStoragePeriod()));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        var oldDate = order.getDelivery().getOutletStorageLimitDate();
        var oldPeriod = order.getDelivery().getOutletStoragePeriod();

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(oldDate);
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);
        var orderId = order.getId();
        var uid = order.getBuyer().getUid();
        assertThrows(ChangeRequestNotAppliedException.class, () -> client.editOrder(
                orderId,
                ClientRole.USER,
                uid,
                List.of(Color.BLUE),
                orderEditRequest));

        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(oldDate));
        assertEquals(oldPeriod, order.getDelivery().getOutletStoragePeriod());
    }

    @Test
    public void shouldNotChangeStorageLimitDateIfOrderAlreadyHasStorageLimitDateChangeRequest() throws Exception {
        var order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(DeliveryProvider.MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        pvzMock.stubFor(
                patch(urlPathEqualTo("/logistics/pickup-point/orders/" + order.getId() + "/reschedule-expiration"))
                        .willReturn(ok())
        );

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PROCESSING);

        orderDeliveryHelper.addTrack(order, new Track("trackCode", DeliveryProvider.MOCK_DELIVERY_SERVICE_ID),
                ClientInfo.SYSTEM);
        order = orderService.getOrder(order.getId());

        var delivery = order.getDelivery();
        delivery.setOutletStoragePeriod(5);
        delivery.setOutletStorageLimitDate(LocalDate.ofInstant(
                delivery.getDeliveryDates().getFromDate().toInstant(), clock.getZone())
                .plusDays(delivery.getOutletStoragePeriod()));
        order = orderUpdateService.updateOrderDelivery(order.getId(), delivery, ClientInfo.SYSTEM);

        order = orderStatusHelper.proceedOrderToStatus(order, OrderStatus.PICKUP);

        var oldDate = order.getDelivery().getOutletStorageLimitDate();
        var oldPeriod = order.getDelivery().getOutletStoragePeriod();
        var newDate = oldDate.plusDays(3L);
        var newPeriod = oldPeriod + 3;

        var storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(newDate);
        var orderEditRequest = new OrderEditRequest();
        orderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);
        var changeRequests = client.editOrder(
                order.getId(),
                ClientRole.USER,
                order.getBuyer().getUid(),
                List.of(Color.BLUE),
                orderEditRequest);

        assertNotNull(changeRequests);
        var cr = changeRequests.stream().filter(ch ->
                ch.getType() == ChangeRequestType.STORAGE_LIMIT_DATE
                        && ch.getStatus() == ChangeRequestStatus.APPLIED)
                .findAny().orElse(null);
        assertNotNull(cr);
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getNewDate().isEqual(newDate));
        assertTrue(((StorageLimitDateChangeRequestPayload) cr.getPayload()).getOldDate().isEqual(oldDate));
        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(newDate));
        assertEquals(newPeriod, order.getDelivery().getOutletStoragePeriod());

        //Повторно продлеваем дату хранения
        oldDate = order.getDelivery().getOutletStorageLimitDate();
        oldPeriod = order.getDelivery().getOutletStoragePeriod();
        newDate = oldDate.plusDays(3L);

        storageLimitDatesRequest = new StorageLimitDateRequest();
        storageLimitDatesRequest.setNewDate(newDate);
        var newOrderEditRequest = new OrderEditRequest();
        newOrderEditRequest.setStorageLimitDateRequest(storageLimitDatesRequest);

        var orderId = order.getId();
        var uid = order.getBuyer().getUid();
        assertThrows(ChangeRequestNotAppliedException.class, () -> client.editOrder(
                orderId,
                ClientRole.USER,
                uid,
                List.of(Color.BLUE),
                newOrderEditRequest));

        order = orderService.getOrder(order.getId());
        assertTrue(order.getDelivery().getOutletStorageLimitDate().isEqual(oldDate));
        assertEquals(oldPeriod, order.getDelivery().getOutletStoragePeriod());
    }
}
