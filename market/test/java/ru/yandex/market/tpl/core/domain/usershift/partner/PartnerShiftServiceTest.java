package ru.yandex.market.tpl.core.domain.usershift.partner;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDetailsDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerRoutingListDataDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerShiftDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftParamsDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.company.CompanyCachingService;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.IS_PARTNER_COMPANY_ROLE_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

/**
 * @author kukabara
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class PartnerShiftServiceTest {

    public static final long UID_1 = 1234;
    public static final long UID_2 = 2345;
    private final TestUserHelper userHelper;
    private final TestDataFactory testDataFactory;
    private final UserShiftReassignManager userShiftReassignManager;
    private final UserShiftCommandDataHelper helper;
    private final ConfigurationServiceAdapter configurationServiceAdapter;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final CompanyCachingService companyCachingService;

    private final PartnerShiftService partnerShiftService;

    private List<Order> orders;
    private User user1;
    private UserShift userShift1;
    private UserShift multiOrderUserShift;
    private CompanyPermissionsProjection company1;

    @BeforeEach
    void init() {
        LocalDate today = LocalDate.now(clock);
        user1 = userHelper.findOrCreateUser(UID_1, today);
        Company company1 = userHelper.findOrCreateCompany(DEFAULT_COMPANY_NAME);
        this.company1 = companyCachingService.getProjectionForCompany(company1.getCampaignId());
        User user2 = userHelper.findOrCreateUser(UID_2, today);

        Shift shift = userHelper.findOrCreateOpenShift(today);

        orders = Stream.generate(() ->
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(today).build())).limit(5)
                .collect(Collectors.toList());

        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L));
        Order pickupOrder = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryDate(today)
                        .pickupPoint(pickupPoint)
                        .build());

        userShift1 = repository.findById(commandService.createUserShift(
                UserShiftCommand.Create.builder()
                        .userId(user1.getId())
                        .shiftId(shift.getId())
                        .routePoint(helper.taskPrepaid("addr1", 12, orders.get(0).getId()))
                        .routePoint(helper.taskPrepaid("addr2", 13, orders.get(1).getId()))
                        .routePoint(helper.taskPrepaid("addr3", 14, orders.get(2).getId()))
                        .routePoint(helper.taskPrepaid("addr4", 15, orders.get(3).getId()))
                        .mergeStrategy(SimpleStrategies.NO_MERGE)
                        .build())).orElseThrow();

        userHelper.addLockerDeliveryTaskToShift(user1, userShift1, pickupOrder);

        // 2 отмены + 1 доставка + 1 перенос
        userHelper.checkin(userShift1);
        userHelper.finishPickupAtStartOfTheDay(userShift1);
        userHelper.finishDelivery(Objects.requireNonNull(userShift1.getCurrentRoutePoint()), true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift1.getCurrentRoutePoint()), true);
        userHelper.finishDelivery(Objects.requireNonNull(userShift1.getCurrentRoutePoint()), false);
        userHelper.rescheduleNextDay(Objects.requireNonNull(userShift1.getCurrentRoutePoint()));

        UserShift userShift2 = repository.findById(commandService.createUserShift(
                UserShiftCommand.Create.builder()
                        .userId(user2.getId())
                        .shiftId(shift.getId())
                        .routePoint(helper.taskPrepaid("addr1", 12, orders.get(4).getId()))
                        .mergeStrategy(SimpleStrategies.NO_MERGE)
                        .build())).orElseThrow();
        userHelper.checkin(userShift2);
        userHelper.finishPickupAtStartOfTheDay(userShift2);
        userHelper.finishDelivery(Objects.requireNonNull(userShift2.getCurrentRoutePoint()), true);
    }

    @AfterEach
    void after() {
        configurationServiceAdapter.deleteValue(IS_PARTNER_COMPANY_ROLE_ENABLED);
    }

    @Test
    void findShifts() {
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        Page<PartnerShiftDto> shifts = partnerShiftService.findShifts(params, PageRequest.of(0, 10), null);
        assertThat(shifts.getContent()).hasSize(1);
    }

    @Test
    void findUserShifts() {
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        Page<PartnerUserShiftDto> userShifts = partnerShiftService.findUserShifts(params, PageRequest.of(0, 10), null);
        assertThat(userShifts.getContent()).extracting(PartnerUserShiftDto::getCourierUid)
                .contains(UID_1, UID_2);

        PartnerUserShiftDto first = userShifts.getContent().stream()
                .filter(us -> us.getCourierUid().equals(user1.getUid()))
                .findFirst().orElseThrow();
        assertThat(first.getCountOrders()).isEqualTo(5);
        assertThat(first.getCountMultiOrders()).isEqualTo(5);
        assertThat(first.getCountOrdersDelivered()).isEqualTo(1);
        //CANCELLED, CLIENT_REFUSED by COURIER
        assertThat(first.getCountOrdersCancelled()).isEqualTo(2);
        assertThat(first.getCountOrdersCancelledCourier()).isEqualTo(2);
        assertThat(first.getCountOrdersRescheduled()).isEqualTo(1);
    }

    @Test
    void findUserShiftsWithReassignments() {
        long uid3 = 1234432L;
        var user3 = userHelper.findOrCreateUser(uid3);
        userShiftReassignManager.reassignOrdersV2(Set.of(orders.get(1).getId()), Set.of(), Set.of(), user3.getId(),
                "ABSENTEEISM");
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        Page<PartnerUserShiftDto> userShifts = partnerShiftService.findUserShifts(params, PageRequest.of(0, 10), null);
        assertThat(userShifts.getContent()).extracting(PartnerUserShiftDto::getCourierUid)
                .contains(UID_1, uid3);

        PartnerUserShiftDto second = userShifts.getContent().stream()
                .filter(us -> us.getCourierUid().equals(user3.getUid()))
                .findFirst().orElseThrow();
        assertThat(second.getHaveReassignIn()).isTrue();

    }

    @Test
    void findRoutingListData() {
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(List.of(userShift1.getId()),
                        CompanyPermissionsProjection.builder().build());
        assertThat(routingListData).isNotEmpty();
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
    }

    @Test
    void findRoutingListDataWithSuperCompanyAndRoleEnabled() {
        configurationServiceAdapter.insertValue(IS_PARTNER_COMPANY_ROLE_ENABLED, true);
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(List.of(userShift1.getId()),
                        CompanyPermissionsProjection.builder().isSuperCompany(true).build());
        assertThat(routingListData).isNotEmpty();
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
    }

    @Test
    void findRoutingListDataWithNotSuperCompanyAndRoleEnabled() {
        Company company = userHelper.findOrCreateCompany("company", "login");
        User user = userHelper.findOrCreateUser(9876, LocalDate.now(clock), company.getName());
        UserShift userShift2 = userHelper.createEmptyShift(user, LocalDate.now(clock));
        configurationServiceAdapter.insertValue(IS_PARTNER_COMPANY_ROLE_ENABLED, true);
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(List.of(userShift1.getId(), userShift2.getId()),
                        CompanyPermissionsProjection.builder().isSuperCompany(true).build());
        assertThat(routingListData).hasSize(1);
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
    }

    @Test
    void findRoutingListDataByUid() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.HIDE_RECIPIENT_INFO_FOR_PICKUP_ORDERS_ENABLED
                , true);
        long sortingCenterId = userShift1.getShift().getSortingCenter().getId();
        long uid = user1.getUid();
        LocalDate date = LocalDate.now(clock);
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(sortingCenterId, date, List.of(uid));

        assertThat(routingListData).isNotEmpty();
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
        Optional<PartnerOrderDetailsDto> pickupOrderO = StreamEx.of(orders)
                .filter(o -> o.getOrderType() == PartnerOrderType.LOCKER)
                .findFirst();

        assertThat(pickupOrderO).isPresent();
        PartnerOrderDetailsDto actualPickupOrder = pickupOrderO.get();
        assertThat(actualPickupOrder.getRecipient().getPhone()).isBlank();
    }

    @Test
    void findRoutingListDataByUidNoShift() {
        long sortingCenterId = userShift1.getShift().getSortingCenter().getId();
        LocalDate date = LocalDate.now(clock);
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(sortingCenterId, date, List.of(1L));
        assertThat(routingListData).isEmpty();
    }

    @Test
    void findShiftsWithRoleEnabled() {
        configurationServiceAdapter.insertValue(IS_PARTNER_COMPANY_ROLE_ENABLED, true);
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        Page<PartnerShiftDto> shifts = partnerShiftService.findShifts(
                params, PageRequest.of(0, 10), company1);
        assertThat(shifts.getContent()).hasSize(1);
    }

    @Test
    void findUserShiftsWithRoleEnabled() {
        configurationServiceAdapter.insertValue(ConfigurationProperties.IS_PARTNER_COMPANY_ROLE_ENABLED, true);
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        Page<PartnerUserShiftDto> userShifts = partnerShiftService.findUserShifts(params,
                PageRequest.of(0, 10), company1);
        assertThat(userShifts.getContent()).hasSize(2);
    }

    @Test
    void findUserShiftsWithRoleEnabledUsersFromAnotherCompany() {
        configurationServiceAdapter.insertValue(IS_PARTNER_COMPANY_ROLE_ENABLED, true);
        PartnerUserShiftParamsDto params = new PartnerUserShiftParamsDto();
        params.setShiftDateFrom(LocalDate.now(clock));
        params.setShiftDateTo(LocalDate.now(clock));
        long companyIdWithoutUsers = -3L;
        Page<PartnerUserShiftDto> userShifts = partnerShiftService.findUserShifts(params,
                PageRequest.of(0, 10), CompanyPermissionsProjection.builder().id(companyIdWithoutUsers).build());
        assertThat(userShifts.getContent()).hasSize(0);
    }

    @Test
    void findRoutingListDataWithDoNotCall() {
        createMultiOrder();
        configurationServiceAdapter.insertValue(DO_NOT_CALL_ENABLED, true);

        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(List.of(multiOrderUserShift.getId()), null);

        assertThat(routingListData).hasSize(1);
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
        assertThat(orders).extracting(PartnerOrderDetailsDto::getCallRequirement)
                .containsOnly(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void findRoutingListDataWithDoNotCallNegative() {
        createMultiOrder();
        configurationServiceAdapter.insertValue(DO_NOT_CALL_ENABLED, false);

        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(List.of(multiOrderUserShift.getId()), null);

        assertThat(routingListData).hasSize(1);
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
        for (PartnerOrderDetailsDto order : orders) {
            assertThat(order.getCallRequirement()).isNull();
        }
    }

    @Test
    void findRoutingListDataByUidWithDoNotCall() {
        createMultiOrder();
        long sortingCenterId = multiOrderUserShift.getShift().getSortingCenter().getId();
        LocalDate date = LocalDate.now(clock);
        List<PartnerRoutingListDataDto> routingListData =
                partnerShiftService.findRoutingListData(sortingCenterId, date, List.of(4567L));

        assertThat(routingListData).hasSize(1);
        List<PartnerOrderDetailsDto> orders = routingListData.get(0).getOrders();
        assertThat(orders).isNotEmpty();
        assertThat(orders.get(0).getOrderType()).isNotNull();
        for (PartnerOrderDetailsDto order : orders) {
            assertThat(order.getCallRequirement()).isNull();
        }
    }

    private List<Order> createMultiOrder() {
        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        Order multiOrder1 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("451234")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(5000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .build());

        Order multiOrder2 = testDataFactory.generateOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .recipientNotes(DO_NOT_CALL_DELIVERY_PREFIX)
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .build());


        assertThat(multiOrder1.getId()).isNotNull();
        assertThat(multiOrder2.getId()).isNotNull();
        testDataFactory.flushAndClear();

        User user = userHelper.findOrCreateUser(4567L, LocalDate.now(clock));
        multiOrderUserShift = userHelper.createEmptyShift(user, LocalDate.now(clock));
        userShiftReassignManager.assign(multiOrderUserShift, multiOrder1);
        userShiftReassignManager.assign(multiOrderUserShift, multiOrder2);
        userHelper.checkinAndFinishPickup(multiOrderUserShift);

        return List.of(multiOrder1, multiOrder2);
    }
}
