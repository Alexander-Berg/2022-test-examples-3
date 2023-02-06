package ru.yandex.market.tpl.core.query.usershift.routepoint;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointClientDeliveryDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
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
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointClientDeliveryCardType.STANDARD_ORDER;

@RequiredArgsConstructor
public class RoutePointQueryServiceTest extends TplAbstractTest {
    private final RoutePointQueryService routePointQueryService;

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
    private final UserShiftCommandService commandService;

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
    void multiClientReturnsShouldNotBeCompressed() {
        var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
        var order = prepareUsualOrder("3324232");
        var routePoint = prepareRoutePoint(List.of(order));
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var clientReturn2 = clientReturnGenerator.generateReturnFromClient();

        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePoint.getId(), clientReturn.getId(), deliveryTime
                )
        );
        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePoint.getId(), clientReturn2.getId(), deliveryTime
                )
        );

        RoutePointClientDeliveryDto routePointInfoPossibleCompressed =
                routePointQueryService.getRoutePointInfoCompressed(routePoint, user, null);

        assertThat(routePointInfoPossibleCompressed.getCards()).hasSize(3);
    }

    @Test
    void testExecuteOrdersInRoutePoint() {
        var order = prepareUsualOrder("33242");
        var order2 = prepareUsualOrder("1231");
        var routePoint = prepareRoutePoint(List.of(order, order2));

        var routePointInfoCompressed = routePointQueryService.getRoutePointInfoCompressed(routePoint, user, null);
        assertThat(routePointInfoCompressed.getCallTasks()).isEmpty();
        assertThat(routePointInfoCompressed.getCards()).hasSize(1);
        var card = routePointInfoCompressed.getCards().get(0);
        assertThat(card.getTaskIds()).hasSize(2);
        var firstTaskId = card.getTaskIds().get(0);
        var secondTaskId = card.getTaskIds().get(1);

        assertThat(card.getType()).isEqualTo(STANDARD_ORDER);

        commandService.finishOrderDeliveryTask(
                user,
                UserShiftCommand.FinishOrderDeliveryTask.builder()
                        .taskId(firstTaskId)
                        .userShiftId(routePoint.getUserShift().getId())
                        .finishedAt(Instant.now())
                        .build()
        );
        routePointInfoCompressed = routePointQueryService.getRoutePointInfoCompressed(routePoint, user, null);
        assertThat(routePointInfoCompressed.getCards()).isNotEmpty();
        assertThat(routePointInfoCompressed.getCallTasks()).isEmpty();

        commandService.finishOrderDeliveryTask(
                user,
                UserShiftCommand.FinishOrderDeliveryTask.builder()
                        .taskId(secondTaskId)
                        .userShiftId(routePoint.getUserShift().getId())
                        .finishedAt(Instant.now())
                        .build()
        );
        routePointInfoCompressed = routePointQueryService.getRoutePointInfoCompressed(routePoint, user, null);
        assertThat(routePointInfoCompressed.getCards()).isEmpty();
    }

    private Order prepareUsualOrder(String externalOrderId) {
        return orderGenerateService.createOrder(
                usualOrderCommand().externalOrderId(externalOrderId).build()
        );
    }

    private RoutePoint prepareRoutePoint(List<Order> orders) {
        return transactionTemplate.execute(ts -> {
            var userShift = userShiftRepository.findByIdOrThrow(this.userShift.getId());
            orders.forEach(order -> userShiftReassignManager.assign(userShift, order));

            testUserHelper.checkinAndFinishPickup(userShift);
            return userShift.streamDeliveryRoutePoints().findFirst().get();
        });
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
                        .geoPoint(GeoPoint.ofLatLon(1, 1))
                        .build()
                )
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED);
    }

}
