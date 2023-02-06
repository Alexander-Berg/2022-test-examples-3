package ru.yandex.market.tpl.core.domain.shift;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.routing.AdditionalRoutingParamDto;
import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.common.util.exception.TplInvalidParameterException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.log.RoutingLogDao;
import ru.yandex.market.tpl.core.domain.specialrequest.SpecialRequestGenerateService;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.additional_data.UserShiftAdditionalDataRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants.DEFAULT_DS_ID;
import static ru.yandex.market.tpl.core.domain.order.TplOrderGenerateConstants.NOT_DEFAULT_DS_ID;

/**
 * @author ungomma
 */
@RequiredArgsConstructor
class ShiftManagerTest extends TplAbstractTest {

    private static final LocalDate DATE = LocalDate.of(1986, 4, 29);
    private final TransactionTemplate transactionTemplate;
    private final ShiftManager shiftManager;
    private final TplRoutingShiftManager tplRoutingShiftManager;
    private final UserShiftRepository userShiftRepository;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final EntityManager entityManager;
    private final UserShiftReassignManager userShiftReassignManager;
    private final OrderGenerateService orderGenerateService;
    private final RoutingLogDao routingLogDao;
    private final ShiftManagerValidator shiftManagerValidator;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserShiftAdditionalDataRepository userShiftAdditionalDataRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final SpecialRequestGenerateService specialRequestGenerateService;

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
    }

    @Test
    void shouldAssignCouriers() {
        testUserHelper.findOrCreateUser(278L, DATE);
        var shift = shiftManager.findOrCreate(DATE, SortingCenter.DEFAULT_SC_ID);
        entityManager.clear();
        assertThat(shift).isNotNull();
        List<UserShift> userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).isEmpty();

        shiftManager.assignShiftsForDate(DATE, SortingCenter.DEFAULT_SC_ID);

        userShifts = userShiftRepository.findAllByShiftId(shift.getId());
        assertThat(userShifts).isNotEmpty();
    }

    @DisplayName("Запуск домаршрутизации заказов из csv")
    @Test
    @Transactional
    void shouldRunAdditionalRouteCustomShiftGroup() throws IOException, URISyntaxException {
        User user2 = testUserHelper.findOrCreateUser(279L, DATE);
        var shift = shiftManager.findOrCreate(DATE, SortingCenter.DEFAULT_SC_ID);
        entityManager.clear();
        assertThat(shift).isNotNull();
        shiftManager.assignShiftsForDate(DATE, SortingCenter.DEFAULT_SC_ID);
        Optional<UserShift> userShift = userShiftRepository.findByShiftIdAndUserId(shift.getId(), user2.getId());
        Order order = orderGenerateService.createOrder("123123123");
        userShiftReassignManager.assign(userShift.get(), order);
        var bytes = FileUtils.readFileToByteArray(Paths.get(getClass()
                .getResource("/csv/shifManager.csv").toURI()).toFile());
        User user = testUserHelper.findOrCreateUser(270L, DATE.plusDays(1L));

        String routingRequestId = shiftManager.additionalRouteCustomShiftGroup(
                new AdditionalRoutingParamDto(
                        shift.getSortingCenter().getId(),
                        Set.of(user.getId()),
                        shift.getShiftDate()),
                bytes
        );

        assertThat(routingRequestId).isNotNull();
        assertThat(routingLogDao.findResultByRequestId(routingRequestId)).isNotNull();
    }

    @DisplayName("При домаршрутизации выбрасывается исключение, если не все заказы принадлежат выбранному СЦ")
    @Test
    void shouldThrowExceptionWhenRunAdditionalRouteCustomShiftGroup() {
        Shift shift = shiftManager.findOrCreate(DATE, SortingCenter.DEFAULT_SC_ID);

        var order1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DEFAULT_DS_ID)
                        .build());

        var order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(NOT_DEFAULT_DS_ID)
                        .build());

        TplInvalidParameterException assertThrows = assertThrows(
                TplInvalidParameterException.class,
                () -> shiftManagerValidator.validateAllOrderIdsBelongsToSortingCenter(
                        List.of(order1, order2).stream().map(Order::getExternalOrderId).collect(Collectors.toSet()),
                        shift
                )
        );

        String message = assertThrows.getMessage();
        assertThat(message).contains("Логистические заявки из запроса не принадлежат СЦ");
    }

    @DisplayName("При домаршрутизации выбрасывается исключение, если не все логистические заявки принадлежат выбранному СЦ")
    @Test
    void shouldThrowExceptionWhenRunAdditionalRouteCustomShiftGroupWithWrongSc() {
        Shift shift = shiftManager.findOrCreate(DATE, SortingCenter.DEFAULT_SC_ID);

        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .deliveryServiceId(DEFAULT_DS_ID)
                        .build());

        var specialRequest = specialRequestGenerateService.createSpecialRequest(
                SpecialRequestGenerateService.SpecialRequestGenerateParam.builder()
                        .deliveryServiceId(NOT_DEFAULT_DS_ID)
                        .build()
        );

        TplInvalidParameterException assertThrows = assertThrows(
                TplInvalidParameterException.class,
                () -> shiftManagerValidator.validateAllOrderIdsBelongsToSortingCenter(
                        Set.of(order.getExternalOrderId(), specialRequest.getExternalId()), shift
                )
        );

        String message = assertThrows.getMessage();
        assertThat(message).contains("Логистические заявки из запроса не принадлежат СЦ");
    }

    @Test
    void shouldCreateShiftAndUserShift() {
        var courier = testUserHelper.findOrCreateUser(278L, LocalDate.now());
        var us = shiftManager.createIfNotExistOpenShiftAndUserShiftForCourierAndDate(
                SortingCenter.DEFAULT_SC_ID,
                courier,
                LocalDate.now(),
                true
        );
        assertThat(us).isNotNull();
    }

    @Test
    void shouldThrowExceptionWhenCreateShiftAndUserShift() {
        var courier = testUserHelper.findOrCreateUser(278L, LocalDate.now());
        Exception exception = assertThrows(TplEntityNotFoundException.class, () -> {
            shiftManager.createIfNotExistUserShiftForCourier(
                    SortingCenter.DEFAULT_SC_ID,
                    courier,
                    LocalDate.now(),
                    true
            );
        });
        assertThat(exception.getMessage()).contains("Can't find shift for date");
    }

    @Test
    void shouldCreateUserShift() {
        var courier = testUserHelper.findOrCreateUser(278L, LocalDate.now());
        shiftManager.findOrCreate(LocalDate.now(), SortingCenter.DEFAULT_SC_ID);
        var us = shiftManager.createIfNotExistUserShiftForCourier(
                SortingCenter.DEFAULT_SC_ID,
                courier,
                LocalDate.now(),
                true
        );
        assertThat(us).isNotNull();
    }

    @Test
    void shouldUndoRouting() {
        var user = testUserHelper.findOrCreateUser(1L, DATE);
        var vehicle = vehicleGenerateService.generateVehicle();
        vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .users(List.of(user))
                .registrationNumber("A000AA")
                .registrationNumberRegion("111")
                .vehicle(vehicle)
                .build());


        Shift createdShift = transactionTemplate.execute(tt -> {
            var shift = shiftManager.findOrCreate(DATE, SortingCenter.DEFAULT_SC_ID);
            long userShiftId = testDataFactory.createEmptyShift(shift.getId(), user);
            var userShift = userShiftRepository.findByIdOrThrow(userShiftId);

            userShiftReassignManager.assign(userShift, orderGenerateService.createOrder(getPickupOrder()));
            userShiftReassignManager.assign(userShift, orderGenerateService.createOrder());

            return shift;
        });

        var userShift = userShiftRepository.findByShiftIdAndUserId(createdShift.getId(), user.getId()).orElseThrow();

        userShiftAdditionalDataRepository.findByUserShiftId(userShift.getId()).orElseThrow();

        tplRoutingShiftManager.undoRouting(createdShift.getId());
        createdShift = shiftManager.findOrThrow(DATE, SortingCenter.DEFAULT_SC_ID);
        assertThat(userShiftRepository.findAllByShiftId(createdShift.getId())).isEmpty();

        assertThat(createdShift.getStatus()).isEqualTo(ShiftStatus.CREATED);
        assertThat(createdShift.getProcessingId()).isNull();
        assertThat(userShiftAdditionalDataRepository.findByUserShiftId(createdShift.getId())).isEmpty();
    }

    private OrderGenerateService.OrderGenerateParam getPickupOrder() {
        GeoPoint geoPoint = GeoPointGenerator.generateLonLat();
        PickupPoint pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 10001L, 1L);

        this.clearAfterTest(pickupPoint);

        return OrderGenerateService.OrderGenerateParam.builder()
                .deliveryDate(LocalDate.now())
                .pickupPoint(pickupPoint)
                .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                        .geoPoint(geoPoint)
                        .build())
                .build();
    }
}
