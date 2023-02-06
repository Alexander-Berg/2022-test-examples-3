package ru.yandex.market.tpl.core.domain.usershift.location.precise;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressString;
import ru.yandex.market.tpl.core.domain.order.address.AddressUpdateService;
import ru.yandex.market.tpl.core.domain.order.address.DeliveryAddress;
import ru.yandex.market.tpl.core.domain.partner.DeliveryService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.region.TplRegionService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.DeliveryRegionInfoService;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.tracking.Tracking;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.external.juggler.JugglerPushClient;
import ru.yandex.market.tpl.core.service.order.address.GeoPointDistanceValidator;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class GetOrderDeliveryPreciseLocationProcessingServiceTest {

    private static final long ORDER_ID = 1L;
    private static final long ORDER_ID_MULTI_2 = 12L;
    private static final long ORDER_ID_MULTI_1 = 11L;
    private static final int REGION_ID = 1;
    private static final long ORIG_REGION_ID = 1;
    private static final GetOrderDeliveryPreciseLocationPayload PAYLOAD =
            new GetOrderDeliveryPreciseLocationPayload("req", ORDER_ID, null, false);

    private static final GeoPoint ORIGINAL_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal(55.736306),
            new BigDecimal(37.590497)
    );
    private static final GeoPoint CLOSE_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal(55.736708),
            new BigDecimal(37.589498)
    );
    private static final GeoPoint FAR_LOCATION = GeoPoint.ofLatLon(
            new BigDecimal(55.739681),
            new BigDecimal(37.578434)
    );
    private static final String CITY = "city";
    private static final String STREET = "street";
    private static final String HOUSE = "house";
    private static final String HOUSING = "housing";
    private static final String ENTRANCE = "entrance";
    private static final String FLOOR = "floor";
    private static final String APARTMENT = "apartment";
    private static final String ENTRY_PHONE = "entry phone";
    public static final String BUILDING = "building";
    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private PreciseGeoPointService preciseGeoPointService;
    @MockBean
    private OrderCommandService orderCommandService;
    @MockBean
    private TrackingRepository trackingRepository;
    @MockBean
    private JugglerPushClient jugglerPushClient;
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    private Order order;
    @MockBean
    private SortingCenterService sortingCenterService;
    @MockBean
    private DeliveryRegionInfoService deliveryRegionInfoService;
    @MockBean
    private TplRegionService tplRegionService;
    @MockBean
    private AddressUpdateService addressUpdateService;
    @MockBean
    private UserShiftRepository userShiftRepository;
    @MockBean
    private TransactionTemplate transactionTemplate;
    @MockBean
    private TransactionStatus transactionStatus;
    @MockBean
    private GeoPointDistanceValidator geoPointDistanceValidator;

    @PostConstruct
    void init() {
        when(transactionTemplate.execute(any()))
                .thenAnswer(invocation -> invocation.<TransactionCallback<Boolean>>getArgument(0)
                        .doInTransaction(transactionStatus));
    }

    @Test
    void whenPreciseLocationIsCloseToOriginalItGetsSaved() {
        //given
        GetOrderDeliveryPreciseLocationProcessingService getOrderDeliveryPreciseLocationProcessingService =
                getService();
        given(orderRepository.findById(ORDER_ID))
                .willReturn(Optional.of(order));
        given(orderRepository.findByIdOrThrow(ORDER_ID))
                .willReturn(order);
        given(order.getDelivery().getDeliveryAddress())
                .willReturn(DeliveryAddress.builder()
                        .geoPoint(ORIGINAL_LOCATION)
                        .city(CITY)
                        .street(STREET)
                        .house(HOUSE)
                        .building(BUILDING)
                        .housing(HOUSING)
                        .entrance(ENTRANCE)
                        .floor(FLOOR)
                        .apartment(APARTMENT)
                        .entryPhone(ENTRY_PHONE)
                        .originalRegionId(ORIG_REGION_ID)
                        .build());
        given(preciseGeoPointService.getByAddress(AddressString.builder()
                .city(CITY)
                .street(STREET)
                .house(HOUSE)
                .building(BUILDING)
                .housing(HOUSING)
                .entrance(ENTRANCE)
                .build(), ORIG_REGION_ID)).willReturn(Optional.of(CLOSE_LOCATION));
        given(sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId())).willReturn(new SortingCenter());
        given(tplRegionService.getRegionId(any())).willReturn(REGION_ID);
        given(geoPointDistanceValidator.acceptableDistanceBetweenGeoPoints(CLOSE_LOCATION, ORIGINAL_LOCATION))
                .willReturn(true);

        given(userShiftRepository.findTasksByOrderId(ORDER_ID))
                .willReturn(List.of(
                        getOrderDeliveryTask("m_" + ORDER_ID + "_" + ORDER_ID_MULTI_1 + "_" + ORDER_ID_MULTI_2),
                        getOrderDeliveryTask("m_" + ORDER_ID + "_" + ORDER_ID_MULTI_2 + "_" + ORDER_ID_MULTI_1),
                        getOrderDeliveryTask(null)
                ).stream());

        //when
        getOrderDeliveryPreciseLocationProcessingService.processPayload(PAYLOAD);
        //then
        then(orderCommandService).should(never()).pushFlashMessageNotification(any());
        then(orderCommandService).should().updatePreciseDeliveryLocation(
                List.of(
                        new OrderCommand.UpdatePreciseDeliveryLocation(ORDER_ID, CLOSE_LOCATION, REGION_ID, false,
                                false),
                        new OrderCommand.UpdatePreciseDeliveryLocation(ORDER_ID_MULTI_1, CLOSE_LOCATION, REGION_ID,
                                false, false),
                        new OrderCommand.UpdatePreciseDeliveryLocation(ORDER_ID_MULTI_2, CLOSE_LOCATION, REGION_ID,
                                false, false)
                )
        );
    }

    @Test
    void whenFlashMessageShouldBeSent() {
        //given
        GetOrderDeliveryPreciseLocationProcessingService getOrderDeliveryPreciseLocationProcessingService =
                getService();
        given(orderRepository.findById(ORDER_ID))
                .willReturn(Optional.of(order));
        given(orderRepository.findByIdOrThrow(ORDER_ID))
                .willReturn(order);
        given(order.getDelivery().getDeliveryAddress())
                .willReturn(DeliveryAddress.builder()
                        .geoPoint(ORIGINAL_LOCATION)
                        .city(CITY)
                        .street(STREET)
                        .house(HOUSE)
                        .building(BUILDING)
                        .housing(HOUSING)
                        .entrance(ENTRANCE)
                        .floor(FLOOR)
                        .apartment(APARTMENT)
                        .entryPhone(ENTRY_PHONE)
                        .originalRegionId(ORIG_REGION_ID)
                        .build());
        given(preciseGeoPointService.getByAddress(AddressString.builder()
                .city(CITY)
                .street(STREET)
                .house(HOUSE)
                .building(BUILDING)
                .housing(HOUSING)
                .entrance(ENTRANCE)
                .build(), ORIG_REGION_ID)).willReturn(Optional.of(CLOSE_LOCATION));
        given(geoPointDistanceValidator.acceptableDistanceBetweenGeoPoints(CLOSE_LOCATION, ORIGINAL_LOCATION))
                .willReturn(true);
        given(sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId())).willReturn(new SortingCenter());
        given(tplRegionService.getRegionId(any())).willReturn(REGION_ID);
        var tracking = mock(Tracking.class);
        var userShift = mock(UserShift.class);
        var user = mock(User.class);
        given(tracking.getUserShift()).willReturn(userShift);
        given(userShift.getUser()).willReturn(user);
        given(trackingRepository.findByOrderId(ORDER_ID)).willReturn(Optional.of(tracking));

        //when
        getOrderDeliveryPreciseLocationProcessingService.processPayload(new GetOrderDeliveryPreciseLocationPayload(
                "req", ORDER_ID, "message", false));
        //then
        then(orderCommandService).should().pushFlashMessageNotification(any());
        then(orderCommandService).should().updatePreciseDeliveryLocation(
                List.of(new OrderCommand.UpdatePreciseDeliveryLocation(ORDER_ID, CLOSE_LOCATION, REGION_ID, false,
                        false))
        );
    }

    private OrderDeliveryTask getOrderDeliveryTask(String multiOrderId) {
        OrderDeliveryTask orderDeliveryTask = new OrderDeliveryTask();
        orderDeliveryTask.setOrderId(ORDER_ID);
        orderDeliveryTask.setMultiOrderId(multiOrderId);
        return orderDeliveryTask;
    }

    @Test
    void whenPreciseLocationIsFarFromOriginalItIsNotSaved() {
        checkUpdatePreciseLocation(ORIGINAL_LOCATION, ORIGINAL_LOCATION, false);
    }

    @Test
    void whenMoscowCenterUpdateOriginalLocation() {
        checkUpdatePreciseLocation(GeoPoint.ofLatLon(55.7532, 37.6225),
                FAR_LOCATION, true);
    }

    private void checkUpdatePreciseLocation(GeoPoint originalLocation,
                                            GeoPoint expectedPreciseLocation,
                                            boolean expectedUpdateOriginalLocation
    ) {
        //given
        GetOrderDeliveryPreciseLocationProcessingService getOrderDeliveryPreciseLocationProcessingService =
                getService();
        given(orderRepository.findById(ORDER_ID))
                .willReturn(Optional.of(order));
        given(orderRepository.findByIdOrThrow(ORDER_ID))
                .willReturn(order);
        given(order.getDelivery().getDeliveryAddress())
                .willReturn(DeliveryAddress.builder()
                        .geoPoint(originalLocation)
                        .originalRegionId(ORIG_REGION_ID)
                        .build());
        given(preciseGeoPointService.getByAddress(Mockito.any(AddressString.class), Mockito.anyLong()))
                .willReturn(Optional.of(FAR_LOCATION));
        given(sortingCenterService.findSortCenterForDs(order.getDeliveryServiceId())).willReturn(new SortingCenter());
        given(sortingCenterService.findDsById(order.getDeliveryServiceId())).willReturn(new DeliveryService());
        given(tplRegionService.getRegionId(any())).willReturn(REGION_ID);

        //when
        getOrderDeliveryPreciseLocationProcessingService.processPayload(PAYLOAD);
        //then
        then(orderCommandService).should(never()).pushFlashMessageNotification(any());
        then(orderCommandService).should().updatePreciseDeliveryLocation(
                List.of(new OrderCommand.UpdatePreciseDeliveryLocation(ORDER_ID, expectedPreciseLocation,
                        REGION_ID, expectedUpdateOriginalLocation, false))
        );
    }

    private GetOrderDeliveryPreciseLocationProcessingService getService() {
        return new GetOrderDeliveryPreciseLocationProcessingService(
                orderRepository,
                preciseGeoPointService,
                orderCommandService,
                trackingRepository,
                jugglerPushClient,
                sortingCenterService,
                tplRegionService,
                addressUpdateService,
                userShiftRepository,
                transactionTemplate,
                geoPointDistanceValidator
        );
    }
}
