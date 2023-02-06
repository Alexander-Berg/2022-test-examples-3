package ru.yandex.market.tpl.core.query.usershift.routepoint;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.LeavingAtReceptionStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderPlaceBarcode;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.routepoint.projection.RoutePointClientDeliveryProjection;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class RoutePointClientDeliveryQueryServiceTest extends TplAbstractTest {
    private final RoutePointClientDeliveryQueryService routePointClientDeliveryQueryService;

    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TransactionTemplate transactionTemplate;
    private final OrderRepository orderRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnRepository clientReturnRepository;

    private static final String EXTERNAL_ORDER_ID = "451234";
    private static final OrderPaymentStatus PAYMENT_STATUS = OrderPaymentStatus.PAID;
    private static final OrderPaymentType PAYMENT_TYPE = OrderPaymentType.PREPAID;
    private static final String FIO = "Фамилия Имя Отчество";
    private static final String PHONE = "+7 929 537 .. ..";
    private static final String NOTES = "Заметка";
    private static final String CITY = "Москва";
    private static final String STREET = "Красных зорь";
    private static final String HOUSE = "49";
    private static final String APARTMENT = "5";
    private static final Integer FLOOR = 7;
    private static final String ENTRY_PHONE = "45в";
    private static final String ENTRANCE = "2";

    private User user;
    private Shift shift;
    private UserShift userShift;

    @BeforeEach
    void init() {
        transactionTemplate.execute(ts -> {
            user = testUserHelper.findOrCreateUser(1L);
            shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                    sortingCenterService.findSortCenterForDs(239).getId());
            userShift = userShiftRepository
                    .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));

            return null;
        });
    }

    @Test
    void testFetchRoutePointClientReturn() {
        var clientReturn = prepareClientReturn();
        var routePoint = prepareRoutePoint(clientReturn);

        RoutePointClientDeliveryProjection routePointProjection =
                routePointClientDeliveryQueryService.fetchRoutePointClientDelivery(routePoint);

        assertThat(routePointProjection.getTasks()).hasSize(1);
        RoutePointClientDeliveryProjection.ClientReturn clientReturnProjection
                = routePointProjection.getTasks().get(0).getClientReturn();
        assertThat(clientReturnProjection).isNotNull();
        assertThat(clientReturnProjection.getExternalId()).isEqualTo(clientReturn.getExternalReturnId());
        assertClientReturnRecipientDetails(clientReturnProjection, clientReturn);

    }

    @Test
    void testFetchRoutePointOrder() {
        var order = prepareUsualOrder();
        var routePoint = prepareRoutePoint(order);

        RoutePointClientDeliveryProjection routePointProjection =
                routePointClientDeliveryQueryService.fetchRoutePointClientDelivery(routePoint);

        assertThat(routePointProjection.getId()).isEqualTo(routePoint.getId());
        assertThat(routePointProjection.getStatus()).isEqualTo(routePoint.getStatus());
        assertThat(routePointProjection.getAddress()).isEqualTo(routePoint.getRoutePointAddress());
        assertThat(routePointProjection.getType()).isEqualTo(routePoint.getType());
        assertThat(routePointProjection.getExpectedDate()).isEqualTo(routePoint.getExpectedDateTime());

        List<RoutePointClientDeliveryProjection.Task> tasksProjection = routePointProjection.getTasks();
        assertThat(tasksProjection).hasSize(1);
        RoutePointClientDeliveryProjection.Task taskProjection = routePointProjection.getTasks().get(0);
        assertThat(taskProjection.getLeavingAtReceptionStatus()).isEqualTo(LeavingAtReceptionStatus.UNAVAILABLE);
        assertThat(taskProjection.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(taskProjection.getOrdinalNumber()).isEqualTo(1);
        assertThat(taskProjection.getOrder()).isNotNull();

        RoutePointClientDeliveryProjection.Order orderProjection = taskProjection.getOrder();
        assertThat(orderProjection.getExternalOrderId()).isEqualTo(EXTERNAL_ORDER_ID);
        assertThat(orderProjection.getPaymentStatus()).isEqualTo(PAYMENT_STATUS);
        assertThat(orderProjection.isPickup()).isFalse();
        assertThat(orderProjection.getPlaces()).hasSize(2);
        assertOrderItems(orderProjection, order);
        assertOrderDelivery(orderProjection, order);
        assertThat(orderProjection.getHistoryEvents()).hasSizeGreaterThan(0);
        assertThat(orderProjection.getProperties()).hasSizeGreaterThan(0);
    }

    private ClientReturn prepareClientReturn() {
        return clientReturnGenerator.createClientReturn(
                ClientReturnGenerator.ClientReturnGenerateParam.builder()
                        .build()
        );
    }

    private void assertOrderDelivery(RoutePointClientDeliveryProjection.Order orderProjection, Order trueOrder) {
        var projectionDelivery = orderProjection.getOrderDelivery();
        var trueDelivery = trueOrder.getDelivery();

        assertThat(projectionDelivery.getCallRequirement()).isEqualTo(trueDelivery.getCallRequirement());
        //todo раскоментить, как подолью транк
        //assertThat(projectionDelivery.getRecipientName()).isEqualTo(FIO);
        assertThat(projectionDelivery.getRecipientAddress()).isEqualTo(trueDelivery.getDeliveryAddress().getAddress());
        assertThat(projectionDelivery.getRecipientPhone()).isEqualTo(PHONE);
        assertThat(projectionDelivery.getRecipientNotes()).isEqualTo(NOTES);
        assertThat(projectionDelivery.getCity()).isEqualTo(CITY);
        assertThat(projectionDelivery.getStreet()).isEqualTo(STREET);
        assertThat(projectionDelivery.getHouse()).isEqualTo(HOUSE);
        assertThat(projectionDelivery.getApartment()).isEqualTo(APARTMENT);
        assertThat(projectionDelivery.getFloor()).isEqualTo(String.valueOf(FLOOR));
        assertThat(projectionDelivery.getEntryPhone()).isEqualTo(ENTRY_PHONE);
        assertThat(projectionDelivery.getBuilding()).isEqualTo(trueDelivery.getDeliveryAddress().getBuilding());
        assertThat(projectionDelivery.getHousing()).isEqualTo(trueDelivery.getDeliveryAddress().getHousing());
        assertThat(projectionDelivery.getEntrance()).isEqualTo(ENTRANCE);
    }

    private void assertClientReturnRecipientDetails(RoutePointClientDeliveryProjection.ClientReturn clientReturnProjection,
                                                    ClientReturn clientReturn) {
        var projectionRecipientDetails = clientReturnProjection.getRecipientDetails();
        var trueClientData = clientReturn.getClient().getClientData();
        var trueLogisticPointFrom = clientReturn.getLogisticRequestPointFrom();

        //todo раскоментить, как подолью транк
        //assertThat(projectionDelivery.getRecipientName()).isEqualTo(FIO);
        assertThat(projectionRecipientDetails.getAddress()).isEqualTo(trueLogisticPointFrom.getAddress());
        assertThat(projectionRecipientDetails.getRecipientPhone()).isEqualTo(trueClientData.getPhone());
        assertThat(projectionRecipientDetails.getRecipientNotes()).isNull();
        assertThat(projectionRecipientDetails.getCity()).isEqualTo(trueLogisticPointFrom.getCity());
        assertThat(projectionRecipientDetails.getStreet()).isEqualTo(trueLogisticPointFrom.getStreet());
        assertThat(projectionRecipientDetails.getHouse()).isEqualTo(trueLogisticPointFrom.getHouse());
        assertThat(projectionRecipientDetails.getApartment()).isEqualTo(trueLogisticPointFrom.getApartment());
        assertThat(projectionRecipientDetails.getFloor()).isEqualTo(trueLogisticPointFrom.getFloor());
        assertThat(projectionRecipientDetails.getEntryPhone()).isEqualTo(trueLogisticPointFrom.getEntryPhone());
        assertThat(projectionRecipientDetails.getBuilding()).isEqualTo(trueLogisticPointFrom.getBuilding());
        assertThat(projectionRecipientDetails.getHousing()).isEqualTo(trueLogisticPointFrom.getHousing());
        assertThat(projectionRecipientDetails.getEntrance()).isEqualTo(trueLogisticPointFrom.getEntrance());
    }


    private void assertOrderItems(RoutePointClientDeliveryProjection.Order orderProjection, Order trueOrder) {
        transactionTemplate.execute(ts -> {
            var o = orderRepository.findByIdOrThrow(trueOrder.getId());
            assertThat(o.getItems().size()).isEqualTo(orderProjection.getItems().size());
            Map<Long, OrderItem> byId = o.getItems().stream().collect(Collectors.toMap(OrderItem::getId, oi -> oi));

            orderProjection.getItems().forEach(
                    itemProjection -> {
                        var itemTrue = byId.get(itemProjection.getId());
                        assertThat(itemTrue).isNotNull();
                        assertThat(itemTrue.getSumPrice()).isEqualTo(itemProjection.getSumPrice());
                        assertThat(itemTrue.getCargoTypeCodes()).isEqualTo(itemProjection.getCargoTypeCodes());
                    }
            );
            return null;
        });
    }

    private RoutePoint prepareRoutePoint(Order o) {
        return transactionTemplate.execute(ts -> {
            var order = orderRepository.findByIdOrThrow(o.getId());
            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            userShiftReassignManager.assign(userShift, order);

            testUserHelper.checkinAndFinishPickup(userShift);
            assertThat(userShift.streamDeliveryRoutePoints().collect(Collectors.toList())).hasSize(1);
            return userShift.streamDeliveryRoutePoints().findFirst().get();
        });
    }

    private RoutePoint prepareRoutePoint(ClientReturn cr) {
        return transactionTemplate.execute(ts -> {
            var clientReturn = clientReturnRepository.findByIdOrThrow(cr.getId());
            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            userShiftReassignManager.assign(userShift, clientReturn);

            testUserHelper.checkinAndFinishPickup(userShift);
            assertThat(userShift.streamDeliveryRoutePoints().collect(Collectors.toList())).hasSize(1);
            return userShift.streamDeliveryRoutePoints().findFirst().get();
        });
    }

    private Order prepareUsualOrder() {
        return orderGenerateService.createOrder(
                usualOrderCommand().build()
        );
    }

    private OrderGenerateService.OrderGenerateParam.OrderGenerateParamBuilder usualOrderCommand() {
        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        return OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId(EXTERNAL_ORDER_ID)
                .paymentStatus(PAYMENT_STATUS)
                .paymentType(PAYMENT_TYPE)
                .places(List.of(
                        OrderPlaceDto.builder()
                                .barcode(new OrderPlaceBarcode("145", "barcode1"))
                                .build(),
                        OrderPlaceDto.builder()
                                .barcode(new OrderPlaceBarcode("145", "barcode2"))
                                .build()
                ))
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .isFashion(true)
                        .itemsItemCount(3)
                        .itemsCount(2)
                        .build())
                .buyerYandexUid(1L)
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .recipientFio(FIO)
                .recipientPhone(PHONE)
                .recipientNotes(NOTES)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .city(CITY)
                        .street(STREET)
                        .house(HOUSE)
                        .apartment(APARTMENT)
                        .floor(FLOOR)
                        .entryPhone(ENTRY_PHONE)
                        .entrance(ENTRANCE)
                        .build()
                )
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }
}
