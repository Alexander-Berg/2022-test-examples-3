package ru.yandex.market.tpl.core.domain.shift.task;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.specialrequest.SpecialRequestType;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.logistic_request.LogisticRequest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.DeliverySubtask;
import ru.yandex.market.tpl.core.domain.usershift.DeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerOrderDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLink;
import ru.yandex.market.tpl.core.task.persistence.LogisticRequestLinkRepository;
import ru.yandex.market.tpl.core.task.projection.FlowTask;
import ru.yandex.market.tpl.core.task.projection.TaskFlowType;
import ru.yandex.market.tpl.core.task.service.TaskService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
class TaskOrdinalNumberServiceTest {
    public static final String EXTERNAL_ORDER_ID_1 = "EXTERNAL_ORDER_ID_1";
    public static final String EXTERNAL_ORDER_ID_2 = "EXTERNAL_ORDER_ID_2";
    public static final String EXTERNAL_ORDER_ID_3 = "EXTERNAL_ORDER_ID_3";

    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PickupPointRepository pickupPointRepository;
    private final LockerOrderDataHelper lockerOrderDataHelper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final SpecialRequestGenerateService specialRequestGenerateService;
    private final TaskService taskService;
    private final LogisticRequestLinkRepository logisticRequestLinkRepository;

    private UserShift userShift;
    private Order multiOrder1;
    private Order multiOrder2;
    private Order order1;
    private Order order2;
    private Order lockerOrder1;
    private Order lockerOrder2;
    private Order lockerOrder3;
    private LogisticRequest logisticRequest1;
    private LogisticRequest logisticRequest2;


    @BeforeEach
    void init() {
        var currentDate = LocalDate.now(clock);
        User user = testUserHelper.findOrCreateUser(1L);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(currentDate,
                sortingCenterService.findSortCenterForDs(239).getId());
        userShift = userShiftRepository
                .findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(currentDate)
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321234")
                .deliveryDate(currentDate)
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        order1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("5345323")
                .deliveryDate(currentDate)
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("14:00-15:00"))
                .addressGenerateParam(addressGenerateParam)
                .recipientPhone("89295372775")
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        order2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("43533432")
                .deliveryDate(currentDate)
                .deliveryServiceId(239L)
                .recipientPhone("89295372774")
                .deliveryInterval(LocalTimeInterval.valueOf("16:00-17:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(GeoPointGenerator.generateLonLat())
                        .build())
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .build());

        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        lockerOrder1 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_1, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 2);
        lockerOrder2 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_2, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);
        lockerOrder3 = lockerOrderDataHelper.getPickupOrder(
                shift, EXTERNAL_ORDER_ID_3, pickupPoint, geoPoint, OrderFlowStatus.SORTING_CENTER_PREPARED, 1);

        userShiftReassignManager.assign(userShift, lockerOrder1);
        userShiftReassignManager.assign(userShift, lockerOrder2);
        userShiftReassignManager.assign(userShift, lockerOrder3);
        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);
        userShiftReassignManager.assign(userShift, order1);
        userShiftReassignManager.assign(userShift, order2);

        logisticRequest1 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .type(SpecialRequestType.LOCKER_INVENTORY)
                        .pickupPointId(pickupPoint.getId())
                        .arriveIntervalFrom(currentDate.atTime(13, 0))
                        .arriveIntervalTo(currentDate.atTime(14, 0))
                        .build()
        );
        logisticRequest2 = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .type(SpecialRequestType.PVZ_OTHER_DELIVERY)
                        .pickupPointId(pickupPoint.getId())
                        .arriveIntervalFrom(currentDate.atTime(13, 0))
                        .arriveIntervalTo(currentDate.atTime(14, 0))
                        .build()
        );
        testDataFactory.addFlowTask(userShift.getId(), TaskFlowType.LOCKER_INVENTORY, List.of(logisticRequest1));
        testDataFactory.addFlowTask(userShift.getId(), TaskFlowType.TEST_FLOW, List.of(logisticRequest2));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testOrdinalNumbersIsCorrect(boolean addFlowTasks) {

        configurationServiceAdapter.mergeValue(ConfigurationProperties.SET_FLOW_TASK_ORDINAL_ENABLED, addFlowTasks);
        testUserHelper.checkinAndFinishPickup(userShift);

        var ordinalNumberByOrderId = userShift.streamDeliveryTasks()
                .flatMap(DeliveryTask::streamDeliveryOrderSubtasks)
                .collect(Collectors.toMap(DeliverySubtask::getOrderId,
                        subtask -> subtask.getDeliveryTask().getOrdinalNumber()));
        var ordinalNumberByLogisticRequestId = userShift.streamFlowTasks()
                .mapToEntry(FlowTask::getOrdinalNumber, task -> logisticRequestLinkRepository.findLinksForTask(task.getId()))
                .filterKeys(Objects::nonNull)
                .flatMapValues(Collection::stream)
                .mapValues(LogisticRequestLink::getLogisticRequestId)
                .invert()
                .toMap();

        assertThat(ordinalNumberByOrderId.get(lockerOrder1.getId())).isEqualTo(4);
        assertThat(ordinalNumberByOrderId.get(lockerOrder2.getId())).isEqualTo(4);
        assertThat(ordinalNumberByOrderId.get(lockerOrder3.getId())).isEqualTo(4);

        assertThat(ordinalNumberByOrderId.get(order2.getId())).isEqualTo(3);
        assertThat(ordinalNumberByOrderId.get(order1.getId())).isEqualTo(2);

        assertThat(ordinalNumberByOrderId.get(multiOrder1.getId())).isEqualTo(1);
        assertThat(ordinalNumberByOrderId.get(multiOrder2.getId())).isEqualTo(1);

        if (addFlowTasks) {
            assertThat(ordinalNumberByLogisticRequestId.get(logisticRequest1.getId())).isEqualTo(5);
            assertThat(ordinalNumberByLogisticRequestId.get(logisticRequest2.getId())).isNull();
        } else {
            assertThat(ordinalNumberByLogisticRequestId.get(logisticRequest1.getId())).isNull();
            assertThat(ordinalNumberByLogisticRequestId.get(logisticRequest2.getId())).isNull();
        }
    }

}
