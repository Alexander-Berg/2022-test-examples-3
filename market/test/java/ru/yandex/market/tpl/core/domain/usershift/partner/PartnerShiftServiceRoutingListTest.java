package ru.yandex.market.tpl.core.domain.usershift.partner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerRecipientDto;
import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListDataDto;
import ru.yandex.market.tpl.api.model.shift.routingList.RoutingListRow;
import ru.yandex.market.tpl.api.model.user.UserMode;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.MultiOrderMapper;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.NewDeliveryRoutePointData;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.value.RoutePointAddress;
import ru.yandex.market.tpl.core.external.routing.api.MultiOrder;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.DO_NOT_CALL;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CoreTest
@Slf4j
public class PartnerShiftServiceRoutingListTest {
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftCommandDataHelper helper;
    private final MultiOrderMapper multiOrderMapper;

    private final OrderGenerateService orderGenerateService;
    private final UserService userService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final PickupPointRepository pickupPointRepository;
    private final SortingCenterRepository sortingCenterRepository;
    private final Clock clock;

    private final PartnerShiftService partnerShiftService;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;

    private User user;
    private UserShift userShift;
    private Order singleOrder;
    private String multiOrderId;


    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(1L);
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

        Order multiOrderPart1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());
        Order multiOrderPart2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build());

        singleOrder = orderGenerateService.createOrder();

        userShift = repository.findById(commandService.createUserShift(
                UserShiftCommand.Create.builder()
                        .userId(user.getId())
                        .shiftId(shift.getId())
                        .routePoint(helper.taskPrepaid("addr2", 13, singleOrder.getId()))
                        .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                        .build())).orElseThrow();

        Instant deliveryTime = multiOrderPart1.getDelivery().getDeliveryIntervalFrom();

        RoutePointAddress myAddress = new RoutePointAddress("my_address", geoPoint);
        commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShift.getId(),
                        NewDeliveryRoutePointData.builder()
                                .address(myAddress)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(multiOrderPart1, false, false)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        commandService.addDeliveryTask(user,
                new UserShiftCommand.AddDeliveryTask(userShift.getId(),
                        NewDeliveryRoutePointData.builder()
                                .address(myAddress)
                                .expectedArrivalTime(deliveryTime)
                                .expectedDeliveryTime(deliveryTime)
                                .name("my_name")
                                .withOrderReferenceFromOrder(multiOrderPart2, false, false)
                                .build(),
                        SimpleStrategies.BY_DATE_INTERVAL_MERGE
                )
        );

        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        Order lockerOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pickupPoint)
                        .build());
        userHelper.addLockerDeliveryTaskToShift(user, userShift, lockerOrder);

        PickupPoint pvzPickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 2L, 1L));
        Order pvzOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .pickupPoint(pvzPickupPoint)
                        .build());

        userHelper.addLockerDeliveryTaskToShift(user, userShift, pvzOrder);

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);


        List<MultiOrder> multiOrders = multiOrderMapper.mapForRoutingRequest(List.of(multiOrderPart1, multiOrderPart2));
        MultiOrder multiOrder = multiOrders.iterator().next();
        multiOrder.getMultiOrderId();

        multiOrderId = userShift.streamDeliveryTasks()
                .select(OrderDeliveryTask.class)
                .filter(t -> t.getMultiOrderId().equals(multiOrder.getMultiOrderId()))
                .findFirst()
                .map(OrderDeliveryTask::getParentId)
                .orElseThrow();

    }

    @Test
    void findRoutingListDataByUidV2() {
        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<RoutingListRow> rows = routingListData.get(0).getOrders();
        assertThat(rows).isNotEmpty().hasSize(5);

        assertThat(rows)
                .extracting(RoutingListRow::getOrderNumber)
                .containsExactlyInAnyOrder(
                        PartnerOrderType.LOCKER.getDescription(),
                        PartnerOrderType.PVZ.getDescription(),
                        singleOrder.getExternalOrderId(),
                        multiOrderId,
                        multiOrderId
                );
    }

    @Test
    void findRoutingListDataByUidV3() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        final String scToken = "scToken";

        //setting not null token to shift sc
        Long scId = userShift.getShift().getSortingCenter().getId();
        SortingCenter sortingCenter = sortingCenterRepository.findByIdOrThrow(scId);
        sortingCenter.setToken(scToken);
        sortingCenterRepository.save(sortingCenter);

        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV3(scToken, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<RoutingListRow> rows = routingListData.get(0).getOrders();
        assertThat(rows).isNotEmpty().hasSize(5);

        assertThat(rows)
                .extracting(RoutingListRow::getOrderNumber)
                .containsExactlyInAnyOrder(
                        PartnerOrderType.LOCKER.getDescription(),
                        PartnerOrderType.PVZ.getDescription(),
                        singleOrder.getExternalOrderId(),
                        multiOrderId,
                        multiOrderId
                );

        List<RoutingListRow> multiOrder =
                rows.stream().filter(e -> e.getMultiOrderId().equals(multiOrderId)).collect(Collectors.toList());
        assertThat(multiOrder).extracting(RoutingListRow::getCallRequirement)
                .containsOnly(DO_NOT_CALL);
    }

    @Test
    void findRoutingListDataByUidWithHideClientPhoneNumberAndUserStrictMode() {
        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "true"));
        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.USER_MODE.getName(), UserMode.STRICT_MODE.name()));

        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<RoutingListRow> rows = routingListData.get(0).getOrders();
        assertThat(rows).isNotEmpty();

        assertThat(rows).extracting(RoutingListRow::getRecipient).extracting(PartnerRecipientDto::getPhone)
                .allMatch(Strings::isBlank);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.USER_MODE.getName(), UserMode.DEFAULT_MODE.name()));
        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }


    @Test
    void findRoutingListDataByUidWithHideClientPhoneNumber() {
        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "true"));

        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<RoutingListRow> rows = routingListData.get(0).getOrders();
        assertThat(rows).isNotEmpty();


        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

    @Test
    void findRoutingListDataByUidWithoutHideClientPhoneNumber() {
        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<RoutingListRow> rows = routingListData.get(0).getOrders();
        assertThat(rows).isNotEmpty();


        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

    @Test
    void findRoutingListDataByUidWithDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();

        List<RoutingListRow> rows = routingListData.get(0).getOrders().stream()
                .filter(e -> e.isPartOfMultiOrder() && e.getMultiOrderId().equals(multiOrderId))
                .collect(Collectors.toList());

        assertThat(rows).isNotEmpty();
        assertThat(rows).extracting(RoutingListRow::getCallRequirement)
                .containsOnly(DO_NOT_CALL);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

    @Test
    void findRoutingListDataByUidWithDoNotCallNegative() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));

        long sortingCenterId = userShift.getShift().getSortingCenter().getId();
        long uid = user.getUid();
        LocalDate date = LocalDate.now(clock);
        List<RoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListDataV2(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();

        List<RoutingListRow> rows = routingListData.get(0).getOrders().stream()
                .filter(e -> e.isPartOfMultiOrder() && e.getMultiOrderId().equals(multiOrderId))
                .collect(Collectors.toList());

        assertThat(rows).isNotEmpty();
        for (RoutingListRow row : rows) {
            assertThat(row.getCallRequirement()).isNull();
        }

        userService.upsertUserProperty(user.getId(),
                Map.of(UserProperties.HIDE_CLIENT_PHONE_NUMBER_FOR_COURIER.getName(), "false"));
    }

}
