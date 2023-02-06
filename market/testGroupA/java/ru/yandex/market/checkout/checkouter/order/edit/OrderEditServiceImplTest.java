package ru.yandex.market.checkout.checkouter.order.edit;

import java.time.Clock;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.backbone.validation.order.status.graph.OrderStatusGraph;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.parcel.CancellationRequestStatus;
import ru.yandex.market.checkout.checkouter.delivery.parcel.ParcelService;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.delivery.tracking.TrackCheckpoint;
import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureReader;
import ru.yandex.market.checkout.checkouter.geo.GeoRegionService;
import ru.yandex.market.checkout.checkouter.order.CancellationRequest;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.checkouter.order.changerequest.AvailableOptionType;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestService;
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType;
import ru.yandex.market.checkout.checkouter.order.changerequest.DeliveryServiceCustomerInfo;
import ru.yandex.market.checkout.checkouter.order.changerequest.MethodOfChange;
import ru.yandex.market.checkout.checkouter.order.changerequest.OptionAvailability;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderEditPossibility;
import ru.yandex.market.checkout.checkouter.order.changerequest.OrderOptionAvailability;
import ru.yandex.market.checkout.checkouter.order.changerequest.TrackOrderSource;
import ru.yandex.market.checkout.checkouter.order.changerequest.deliverydatesoptions.DeliveryDatesEditOptionsRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.changerequest.deliverylastmile.DeliveryLastMileEditProcessor;
import ru.yandex.market.checkout.checkouter.order.changerequest.deliveryoption.DeliveryEditOptionsRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.changerequest.paymentmethod.PaymentEditRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.changerequest.paymentmethod.PaymentMethodService;
import ru.yandex.market.checkout.checkouter.order.changerequest.storage.StorageLimitDatesOptionsRequestProcessor;
import ru.yandex.market.checkout.checkouter.order.edit.customerinfo.DeliveryServiceSubtype;
import ru.yandex.market.checkout.checkouter.order.item.MissingItemsRemovalService;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.service.business.PromosService;
import ru.yandex.market.checkout.providers.DeliveryResponseProvider;
import ru.yandex.market.checkout.storage.Storage;
import ru.yandex.market.checkout.test.providers.OrderProvider;
import ru.yandex.market.checkout.test.providers.ParcelProvider;
import ru.yandex.market.checkout.test.providers.TrackCheckpointProvider;
import ru.yandex.market.checkout.test.providers.TrackProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrderEditServiceImplTest extends AbstractWebTestBase {

    private final OrderService orderService = mock(OrderService.class);
    private final DeliveryServiceInfoService deliveryServiceInfoService =
            mock(DeliveryServiceInfoService.class);
    private final ChangeRequestService changeRequestService = mock(ChangeRequestService.class);
    private OrderEditService orderEditService;
    @Autowired
    private OrderStatusGraph orderStatusGraph;
    @Autowired
    private ExecutorService shootingWorkerPool;
    @Autowired
    private GeoRegionService geoRegionService;
    @Autowired
    private Clock clock;
    @Value("${market.checkout.lms.callCourierToDeliveryMinutesThreshold}")
    private long callCourierToDeliveryMinutesThreshold;
    @Autowired
    private CheckouterFeatureReader checkouterFeatureReader;

    @BeforeEach
    void setUp() {

        orderEditService = new OrderEditServiceImpl(mock(Storage.class),
                mock(DeliveryEditOptionsRequestProcessor.class), mock(PaymentEditRequestProcessor.class),
                deliveryServiceInfoService,
                orderStatusGraph, mock(MissingItemsRemovalService.class), mock(PromosService.class),
                mock(DeliveryDatesEditOptionsRequestProcessor.class),
                mock(ParcelService.class), changeRequestService, orderService,
                mock(StorageLimitDatesOptionsRequestProcessor.class),
                shootingWorkerPool,
                mock(QueuedCallService.class),
                checkouterProperties,
                geoRegionService,
                clock,
                callCourierToDeliveryMinutesThreshold,
                checkouterFeatureReader,
                mock(DeliveryLastMileEditProcessor.class),
                mock(PaymentMethodService.class),
                new HashSet<>());

        when(deliveryServiceInfoService.getPossibleOrderOptions(any()))
                .thenReturn(List.of(
                        new PossibleOrderOption(AvailableOptionType.SHOW_RUNNING_COURIER, 48, 49),
                        new PossibleOrderOption(AvailableOptionType.OPEN_PICKUP_TERMINAL, 45, 50)
                ));
    }

    @Test
    void shouldReturnEmptyResultForEmptyIdsList() {
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(), ClientInfo.SYSTEM, Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, empty());
        verify(orderService, never()).getOrders(any(OrderSearchRequest.class), any());
    }

    @Test
    void shouldReturnResultWithoutOptionsForCancelledOrder() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30));
        order.setStatus(OrderStatus.CANCELLED);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), ClientInfo.SYSTEM, Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForOrderWithCancellationRequest() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30));
        order.setCancellationRequest(new CancellationRequest(OrderSubstatus.USER_CHANGED_MIND, "note",
                CancellationRequestStatus.NEW));
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), ClientInfo.SYSTEM, Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForOrderInDeliveryUnderSystem() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30));
        order.setStatus(OrderStatus.DELIVERY);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), ClientInfo.SYSTEM, Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForOrderInDeliveryUnderUser() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30));
        order.setStatus(OrderStatus.DELIVERY);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), new ClientInfo(ClientRole.USER, 123L), Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithOptionsForOrderInPickupTerminal() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30, 45));
        order.setStatus(OrderStatus.DELIVERY);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), ClientInfo.SYSTEM, Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(),
                hasItems(new OptionAvailability(AvailableOptionType.OPEN_PICKUP_TERMINAL)));
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForOrderOnHands() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30, 45, 50));
        order.setStatus(OrderStatus.DELIVERY);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), new ClientInfo(ClientRole.USER, 123L), Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithOptionsForOrderInDelivery() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30, 45));
        order.setStatus(OrderStatus.DELIVERY);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), new ClientInfo(ClientRole.USER, 123L), Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(),
                contains(new OptionAvailability(AvailableOptionType.OPEN_PICKUP_TERMINAL)));
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForDeliveredOrder() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30, 45, 49, 50));
        order.setStatus(OrderStatus.DELIVERED);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), new ClientInfo(ClientRole.USER, 123L), Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldReturnResultWithoutOptionsForOrderInProcessing() {
        final Order order = createOrder(List.of());
        order.setStatus(OrderStatus.PROCESSING);
        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));
        List<OrderOptionAvailability> availabilities = orderEditService.getOrderOptionsAvailabilities(
                Set.of(1L), new ClientInfo(ClientRole.USER, 123L), Set.of(Color.BLUE));
        assertNotNull(availabilities);
        assertThat(availabilities, hasSize(1));
        assertThat(availabilities.get(0).getAvailableOptions(), empty());
        verify(orderService, times(1)).getOrders(any(OrderSearchRequest.class), any());
        validateDeliveryServiceCustomerInfo(availabilities);
    }

    @Test
    void shouldNotReturnDeliveryServiceCustomerInfoBeforeDelivery() {
        final Order order = createOrder(List.of(0, 1, 10, 15, 20, 30, 45, 49, 50));
        order.setStatus(OrderStatus.DELIVERY);

        when(orderService.getOrders(any(OrderSearchRequest.class), any()))
                .thenReturn(new PagedOrders(List.of(order), null));

        when(changeRequestService.getCurrentActiveChangeRequestTypes(any()))
                .thenReturn(Map.of());

        when(deliveryServiceInfoService.getPossibleOrderChanges(any()))
                .thenReturn(List.of(
                        new PossibleOrderChange(ChangeRequestType.DELIVERY_ADDRESS,
                                MethodOfChange.PARTNER_SITE,
                                0,
                                100),
                        new PossibleOrderChange(ChangeRequestType.DELIVERY_ADDRESS,
                                MethodOfChange.PARTNER_PHONE,
                                0,
                                100))
                );

        String phone = "88123456789";
        when(deliveryServiceInfoService.getDeliveryServiceCustomerInfoById(any()))
                .thenReturn(new DeliveryServiceCustomerInfo(
                        "customer",
                        List.of(phone),
                        "track",
                        TrackOrderSource.DS_TRACK_CODE,
                        DeliveryServiceSubtype.MARKET_COURIER)
                );

        List<OrderEditPossibility> orderEditPossibilitiesForDelivery =
                orderEditService.getOrderEditPossibilities(Collections.singleton(order.getId()),
                        new ClientInfo(ClientRole.USER, 123L),
                        Set.of(Color.BLUE), Experiments.empty());

        assertThat(orderEditPossibilitiesForDelivery.get(0).getDeliveryServiceCustomerInfo().getPhones().get(0),
                is(phone));
        order.setStatus(OrderStatus.PROCESSING);

        List<OrderEditPossibility> orderEditPossibilitiesForProcessing =
                orderEditService.getOrderEditPossibilities(Collections.singleton(order.getId()),
                        new ClientInfo(ClientRole.USER, 123L),
                        Set.of(Color.BLUE), Experiments.empty());

        assertNull(orderEditPossibilitiesForProcessing.get(0).getDeliveryServiceCustomerInfo());

    }

    @Nonnull
    private Order createOrder(@Nonnull List<Integer> checkpointStatuses) {
        final Order order = OrderProvider.getBlueOrder();
        order.setStatus(OrderStatus.PROCESSING);
        order.setId(1L);
        final Delivery delivery = DeliveryResponseProvider.buildPickupDeliveryResponse();
        final List<TrackCheckpoint> checkpoints = checkpointStatuses.stream()
                .map(TrackCheckpointProvider::createCheckpoint)
                .collect(Collectors.toList());
        final Track track = TrackProvider.createTrack();
        track.setCheckpoints(checkpoints);
        final Parcel parcel = ParcelProvider.createParcelWithIdAndTracks(10L, List.of(track));
        delivery.setParcels(List.of(parcel));
        order.setDelivery(delivery);
        return order;
    }

    private void validateDeliveryServiceCustomerInfo(@Nonnull List<OrderOptionAvailability> availabilities) {
        availabilities.forEach(a -> {
            final var deliveryServiceCustomerInfo = a.getDeliveryServiceCustomerInfo();
            assertNull(deliveryServiceCustomerInfo);
        });
    }
}
