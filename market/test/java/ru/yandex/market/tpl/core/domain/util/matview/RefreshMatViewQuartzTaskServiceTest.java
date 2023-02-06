package ru.yandex.market.tpl.core.domain.util.matview;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargo;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommand;
import ru.yandex.market.tpl.core.domain.dropoffcargo.DropoffCargoCommandService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.sc.SortingCenterProperties;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.service.user.SortingCenterPropertyService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;
import ru.yandex.market.tpl.core.test.factory.TestTplDropoffFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@RequiredArgsConstructor
class RefreshMatViewQuartzTaskServiceTest extends TplAbstractTest {

    public static final String[] ALL_MAT_VIEWS = {
            "mv_partner_report_shift_last_week",
            "mv_company_report_shift_last_week",
            "mv_partner_report_user_shift_last_month",
            "mv_partner_report_shift_last_day",
            "mv_partner_report_order_last_day",
            "mv_partner_report_order_last_week",
            "mv_partner_report_user_shift_last_week",
            "mv_partner_report_user_shift_last_day",
            "mv_company_report_shift_last_day",
            "mv_partner_report_order_last_month",
            "mv_partner_report_shift_last_month",
            "mv_company_report_shift_last_month"};
    private static final long SORTING_CENTER_ID = 47819L;

    private final RefreshMatViewQuartzTaskService refreshMatViewQuartzTaskService;
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final TestTplDropoffFactory testTplDropoffFactory;
    private final PickupPointRepository pickupPointRepository;
    private final Clock clock;
    private final SortingCenterPropertyService sortingCenterPropertyService;
    private final SortingCenterService sortingCenterService;
    private final DropoffCargoCommandService dropoffCargoCommandService;

    Shift shift;
    User user;
    PickupPoint pickupPoint;

    @BeforeEach
    void init() {
        pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 100500L, 1L)
        );
        ClockUtil.initFixed(clock, LocalDateTime.now());
        clearAfterTest(pickupPoint);
        user = testUserHelper.findOrCreateUser(1L);
        shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock), SORTING_CENTER_ID);

        sortingCenterPropertyService.deletePropertyFromSortingCenter(shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED);
    }

    @Test
    void refreshMatView_whenDropoffTasks_withCargos() {
        //given
        var userShift = testUserHelper.createEmptyShift(user, shift);

        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();

        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);
        var movementReturn = testTplDropoffFactory.generateReturnMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        //add Return cargos tasks...
        IntStream.range(0, 3)
                .mapToObj(this::createCargoByBarcodeSuffix)
                .map(DropoffCargo::getId)
                .forEach(id -> testTplDropoffFactory.addDropoffTask(userShift, movementReturn, id, pickupPoint));

        //add direct cargos tasks...
        IntStream.range(0, 3)
                .mapToObj(this::createCargoByBarcodeSuffix)
                .map(DropoffCargo::getId)
                .forEach(id -> testTplDropoffFactory.addDropoffTask(userShift, movement, id, pickupPoint));

        //then
        StreamEx.of(ALL_MAT_VIEWS).
                forEach(matViewName -> assertDoesNotThrow(() -> refreshMatViewQuartzTaskService.refreshMatView(matViewName,
                        false, null)));
    }

    @Test
    void refreshMatView_whenDropoffTasks_withOutCargos() {
        //given
        var userShift = testUserHelper.createEmptyShift(user, shift);

        var deliveryServiceId =
                sortingCenterService.findDsForSortingCenter(shift.getSortingCenter().getId()).get(0).getId();

        var movement = testTplDropoffFactory.generateDirectMovement(shift, deliveryServiceId, pickupPoint);
        var movementReturn = testTplDropoffFactory.generateReturnMovement(shift, deliveryServiceId, pickupPoint);

        sortingCenterPropertyService.upsertPropertyToSortingCenter(
                shift.getSortingCenter(),
                SortingCenterProperties.DROPSHIP_LOTS_ENABLED,
                true
        );
        //add Return cargos tasks...
        testTplDropoffFactory.addDropoffTask(userShift, movementReturn, null, pickupPoint);

        //add direct cargos tasks...
        testTplDropoffFactory.addDropoffTask(userShift, movement, null, pickupPoint);

        //then
        StreamEx.of(ALL_MAT_VIEWS).
                forEach(matViewName -> assertDoesNotThrow(() ->
                        refreshMatViewQuartzTaskService.refreshMatView(matViewName, false, null)));
    }

    private DropoffCargo createCargoByBarcodeSuffix(Integer barcodeSuffix) {
        return dropoffCargoCommandService.createOrGet(
                DropoffCargoCommand.Create.builder()
                        .barcode("DROPOFF_CARGO_BARCODE" + barcodeSuffix)
                        .logisticPointIdFrom("fakeIdFrom")
                        .logisticPointIdTo("logisticPointTo")
                        .build());
    }
}
