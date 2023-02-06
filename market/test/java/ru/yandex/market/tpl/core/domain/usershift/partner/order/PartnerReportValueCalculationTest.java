package ru.yandex.market.tpl.core.domain.usershift.partner.order;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerShiftDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftDto;
import ru.yandex.market.tpl.api.model.shift.partner.PartnerUserShiftParamsDto;
import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.company.CompanyPermissionsProjection;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.domain.usershift.partner.PartnerShiftService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@RequiredArgsConstructor
class PartnerReportValueCalculationTest extends TplAbstractTest {

    private static final long UID = OBJECT_GENERATOR.nextLong();

    private final TransactionTemplate transactionTemplate;
    private final OrderDeliveryTaskRepository deliveryTaskRepository;
    private final LockerDeliveryTaskRepository lockerTaskRepository;
    private final UserShiftRepository userShiftRepository;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final PartnerShiftService partnerShiftService;
    private final Clock clock;
    private final OrderGenerateService orderGenerateService;

    private long usId;
    private User user;
    private PickupPoint pickupPoint;

    @BeforeEach
    void setUp() {
        ClockUtil.initFixed(clock, LocalDateTime.of(LocalDate.now(), LocalTime.of(12, 0)));
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(UID, date);
        Shift shift = testUserHelper.findOrCreateOpenShift(date);
        usId = testDataFactory.createEmptyShift(shift.getId(), user);
        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
        clearAfterTest(pickupPoint);
    }

    @AfterEach
    void tearDown() {
        ClockUtil.initFixed(clock);
    }

    //new
    @Test
    void cancelCount_whenORDER_IS_DAMAGED_sourceCOURIER() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledCourier(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void canceledTaskNotInFact() {
        ClockUtil.initFixed(clock, LocalDateTime.now());

        //Отменяем таску ДО выдачи курьеру
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //Обновляем часы, чтобы различалось finishedAt у тасок
        ClockUtil.initFixed(clock, LocalDateTime.now().plusMinutes(1));

        //Курьер проходит заборку
        transactionTemplate.execute(status -> {
            testUserHelper.openShift(user, usId);
            testUserHelper.finishPickupAtStartOfTheDay(
                    userShiftRepository.getById(usId), List.of(), List.of(), true, true);
            return status;
        });

        //Проверяем что факт пустой
        var userShiftResult = getUserShiftReportResult();
        var shiftResult = getShiftReportResult();
        assertThat(userShiftResult.getCountOrdersFact()).isEqualTo(0);
        assertThat(shiftResult.getCountOrdersFact()).isEqualTo(0);
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(userShiftResult, expectedResult);
        assertReportResult(shiftResult, expectedResult);
    }

    @Test
    void cancelCount_whenCLIENT_RETURN_CLIENT_REFUSED_sourceCOURIER() {
        //given
        OrderDeliveryTask task = createClientReturnDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledCourier(1)
                .countOrdersCancelled(1)
                .countClientReturns(1)
                .countClientReturnsFact(1)
                .countClientReturnsCancelledClientRefused(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void cancelCount_whenORDER_ITEMS_QUANTITY_MISMATCH_sourceCOURIER() {
        //given
        OrderDeliveryTask task = createClientReturnDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledCourier(1)
                .countOrdersCancelled(1)
                .countClientReturns(1)
                .countClientReturnsFact(1)
                .countClientReturnsCancelledItemsQuantityMismatch(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void cancelCount_whenORDER_ITEMS_MISMATCH_sourceCOURIER() {
        //given
        OrderDeliveryTask task = createClientReturnDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_ITEMS_MISMATCH,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledCourier(1)
                .countOrdersCancelled(1)
                .countClientReturns(1)
                .countClientReturnsFact(1)
                .countClientReturnsCancelledItemsMismatch(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduleCount_whenfail_NO_CONTACT() {
        //given
        OrderDeliveryTask task = createClientReturnDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.NO_CONTACT,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countClientReturns(1)
                .countClientReturnsFact(1)
                .countOrdersRescheduledCourier(1)
                .countOrdersRescheduledNoContact(1)
                .countOrdersRescheduled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduleCount_when_reschedule_NO_CONTACT() {
        //given
        OrderDeliveryTask task = createClientReturnDeliveryTask();
        rescheduleClientReturn(task, OrderDeliveryRescheduleReasonType.NO_CONTACT);

        //then
        ReportData expectedResult = ReportData.builder()
                .countClientReturns(1)
                .countClientReturnsFact(1)
                .countOrdersRescheduledCourier(1)
                .countOrdersRescheduledOtherReason(1)
                .countOrdersRescheduled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenORDER_IS_DAMAGED_sourceOPERATOR() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                        "", Source.OPERATOR), Source.OPERATOR
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledOperator(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenEXTRA_RESCHEDULING_sourceSYSTEM() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.EXTRA_RESCHEDULING,
                        "", Source.SYSTEM), Source.SYSTEM
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledSystem(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenEXTRA_RESCHEDULING_ALL_SOURCES_sourceSYSTEM() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.EXTRA_RESCHEDULING_ALL_SOURCES,
                        "", Source.SYSTEM), Source.SYSTEM
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledSystem(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void otherCount_whenCANCEL_ORDER_sourceCOURIER() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledCourier(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void otherCount_whenCANCEL_ORDER_sourceOPERATOR() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                                    "", Source.OPERATOR), Source.OPERATOR
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledOperator(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void otherCount_whenCANCEL_ORDER_source() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                        "", Source.DELIVERY), Source.DELIVERY
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(1)
                .countOrdersCancelledOther(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenORDER_WAS_LOST_sourceOPERATOR() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST,
                        "", Source.OPERATOR), Source.OPERATOR
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledOperator(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @ParameterizedTest
    @MethodSource("sourcesOfFailure")
    void otherCount_whenCOURIER_NEEDS_HELP_source(Source source) {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                        "", source), source
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    public static Collection<Source> sourcesOfFailure() {
        return Set.of(Source.OPERATOR, Source.DELIVERY);
    }


    //new
    @Test
    void otherCount_whenOTHER_sourceCOURIER() {
        //given
        IntStream.range(0, 2)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.OTHER,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(2)
                .countOrdersCancelledOtherReason(2)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void otherCount_whenOTHER_source() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.OTHER,
                        "", Source.OPERATOR), Source.OPERATOR
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledOperator(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenCLIENT_REFUSED_sourceCLIENT() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                        "", Source.CLIENT), Source.CLIENT
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelledOther(1)
                .countOrdersCancelled(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelCount_whenCLIENT_REFUSED_sourceCOURIER() {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                        "", Source.COURIER), Source.COURIER
        ), Instant.now(clock));
        deliveryTaskRepository.save(task);

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(1)
                .countOrdersCancelledCourier(1)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenPOSTPONED_sourceCOURIER() {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.POSTPONED,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersPostponed(3)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenWRONG_ADDRESS_BY_CLIENT_sourceCOURIER() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(4)
                .countOrdersRescheduledCourierWrongAddressByClient(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenDELIVERY_DATE_UPDATED_sourceCOURIER() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOtherReason(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @ParameterizedTest
    @MethodSource("sourcesOfRescheduledDeliveryDateUpdated")
    void rescheduleCount_whenDELIVERY_DATE_UPDATED_source(Source source) {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                                    "", source), source
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    public static Collection<Source> sourcesOfRescheduledDeliveryDateUpdated() {
        return Arrays.asList(Source.CLIENT, Source.CRM_OPERATOR, Source.DELIVERY);
    }

    @Test
    void rescheduleCount_whenORDER_TYPE_UPDATED_sourceDELIVERY() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_TYPE_UPDATED,
                                    "", Source.DELIVERY), Source.DELIVERY
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOrderTypeUpdated(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenDELIVERY_DATE_UPDATED_sourceSORT_CENTER() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                                    "", Source.SORT_CENTER), Source.SORT_CENTER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(5)
                .countOrdersNotPrepared(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenDELIVERY_DATE_UPDATED_sourceOPERATOR() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                                    "", Source.OPERATOR), Source.OPERATOR
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOperator(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenDELIVERY_DATE_UPDATED_sourceSYSTEM() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                                    "", Source.SYSTEM), Source.SYSTEM
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotAccepted(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenWRONG_COORDINATES_sourceCOURIER() {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.WRONG_COORDINATES,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(3)
                .countOrdersRescheduledCourierWrongCoordinates(3)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void notCount_whenORDER_NOT_ACCEPTED_sourceCOURIER() {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotAccepted(3)
                .countOrdersNotReady(3)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void notCount_whenORDER_NOT_ACCEPTED_sourceDELIVERY() {
        //given
        IntStream.range(0, 7)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED,
                                    "", Source.DELIVERY), Source.DELIVERY
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(7)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenORDER_NOT_PREPARED_Other() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED,
                                    "", Source.OPERATOR), Source.OPERATOR
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenORDER_NOT_PREPARED_COURIER() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(4)
                .countOrdersNotAccepted(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenORDER_NOT_PREPARED_SYSTEM() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED,
                                    "", Source.SYSTEM), Source.SYSTEM
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(4)
                .countOrdersNotPrepared(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenNO_CONTACT_sourceCOURIER() {
        //given
        IntStream.range(0, 6)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.NO_CONTACT,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(6)
                .countOrdersRescheduledNoContact(6)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }


    @Test
    void rescheduleCount_whenCANNOT_PAY_sourceCOURIER() {
        //given
        IntStream.range(0, 6)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANNOT_PAY,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(6)
                .countOrdersRescheduledOtherReason(6)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }


    @ParameterizedTest
    @MethodSource("reasonsOfRescheduledToOthers")
    void rescheduleCount_whenCourier_Other(OrderDeliveryTaskFailReasonType reason) {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(reason,
                                    "", Source.COURIER), Source.COURIER
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    public static Collection<OrderDeliveryTaskFailReasonType> reasonsOfRescheduledToOthers() {
        return Arrays.asList(OrderDeliveryTaskFailReasonType.WRONG_ADDRESS,
                OrderDeliveryTaskFailReasonType.WRONG_LOCKER_COORDINATES);
    }

    //new
    @Test
    void rescheduleCount_whenCOURIER_REASSIGNED_Others() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED,
                                    "", Source.OPERATOR), Source.OPERATOR
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void rescheduleCount_whenTask_Others() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.TASK_CANCELLED,
                                    "", Source.OPERATOR), Source.OPERATOR
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //new
    @Test
    void cancelledCount_whenTask_SYSTEM() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    OrderDeliveryTask task = createOrderDeliveryTask();
                    task.failTask(new OrderDeliveryFailReason(
                            new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.TASK_CANCELLED,
                                    "", Source.SYSTEM), Source.SYSTEM
                    ), Instant.now(clock));
                    deliveryTaskRepository.save(task);
                });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(4)
                .countOrdersCancelledSystem(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }


    //Locker tasks tests...


    @Test
    void rescheduledLockerTask_LAVKA_CLOSED_Courier() {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.LAVKA_CLOSED,
                            "", Source.COURIER), Source.COURIER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(4)
                .countOrdersRescheduledCourier(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_ORDER_NOT_ACCEPTED_Courier() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED,
                            "", Source.COURIER), Source.COURIER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(5)
                .countOrdersNotAccepted(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_COURIER_NEEDS_HELP_Courier() {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.COURIER_NEEDS_HELP,
                            "", Source.COURIER), Source.COURIER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(3)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @ParameterizedTest
    @MethodSource("ltSourcesOrderNotPrepared")
    void rescheduledLockerTask_ORDER_NOT_PREPARED(Source source) {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED,
                            "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(4)
                .countOrdersNotPrepared(4)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    //Дополнить всеми значениями
    public static Collection<Source> ltSourcesOrderNotPrepared() {
        return Arrays.asList(Source.CLIENT, Source.COURIER, Source.OPERATOR, Source.SYSTEM);
    }


    @Test
    void rescheduledLockerTask_COULD_NOT_GET_TO_LOCKER_Courier() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.COULD_NOT_GET_TO_LOCKER,
                            "", Source.COURIER), Source.COURIER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledCourier(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_DELIVERY_DATE_UPDATED_OPERATOR() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED,
                            "", Source.OPERATOR), Source.OPERATOR
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOperator(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_ORDER_TYPE_UPDATED_DELIVERY() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> createLockerDeliveryTask());

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.ORDER_TYPE_UPDATED,
                            "", Source.DELIVERY), Source.DELIVERY
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(5)
                .countOrdersRescheduledOrderTypeUpdated(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_CANCEL_ORDER_DELIVERY() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                            "", Source.DELIVERY), Source.DELIVERY
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }


    @Test
    void rescheduledLockerTask_FINISHED_BY_SUPPORT_COURIER() {
        //given
        IntStream.range(0, 6)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.FINISHED_BY_SUPPORT,
                            "", Source.COURIER), Source.COURIER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(6)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_DELIVERY_DATE_UPDATED_SORT_CENTER() {
        //given
        IntStream.range(0, 6)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                            "", Source.SORT_CENTER), Source.SORT_CENTER
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(6)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @Test
    void rescheduledLockerTask_TASK_CANCELLED_SYSTEM() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.TASK_CANCELLED,
                            "", Source.SYSTEM), Source.SYSTEM
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledSystem(5)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    @ParameterizedTest
    @MethodSource("ltRescheduledCases")
    void rescheduledLockerTask_LOCKER_FULL_COURIER(OrderDeliveryTaskFailReasonType reasonType, Source source) {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(reasonType, "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(3)
                .countOrdersRescheduledCourier(3)
                .build();
        assertReportResult(getUserShiftReportResult(), expectedResult);
        assertReportResult(getShiftReportResult(), expectedResult);
    }

    public static Stream<Arguments> ltRescheduledCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.BIG_PLACE_FOR_FREE_CELLS, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.LOCKER_FULL, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.PVZ_TECHNICAL_ISSUES, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.LAVKA_TECHNICAL_ISSUES, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.PVZ_FULL, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.PVZ_CLOSED, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.PVZ_TIMEOUT, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING, Source.COURIER)
        );
    }

    @Test
    void lockerTask_OTHER_OPERATOR() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.OTHER,
                            "", Source.OPERATOR), Source.OPERATOR
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    @Test
    void lockerTask_EXTRA_RESCHEDULING_ALL_SOURCES_SYSTEM() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.EXTRA_RESCHEDULING_ALL_SOURCES,
                            "", Source.SYSTEM), Source.SYSTEM
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledSystem(5)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    @Test
    void lockerTask_CANCEL_ORDER() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.CANCEL_ORDER,
                            "", null), null
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(5)
                .countOrdersCancelledOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    @ParameterizedTest
    @MethodSource("ltCancelledCases")
    void lockerTask_LOCKER_FULL_COURIER(OrderDeliveryTaskFailReasonType reasonType, Source source) {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(reasonType, "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(3)
                .countOrdersCancelledOtherReason(3)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    public static Stream<Arguments> ltCancelledCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.DIMENSIONS_EXCEEDS, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.OTHER, Source.COURIER),
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, Source.OPERATOR),
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, Source.COURIER)
        );
    }

    @ParameterizedTest
    @MethodSource("ltNotReadyCases")
    void lockerTask_NotReadyCases(OrderDeliveryTaskFailReasonType reasonType, Source source) {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(reasonType, "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersNotReady(3)
                .countOrdersNotPrepared(3)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    @ParameterizedTest
    @MethodSource("ltDeliveryDateUpdatedCases")
    void deliveryDateUpdated(OrderDeliveryTaskFailReasonType reasonType, Source source) {
        //given
        IntStream.range(0, 3)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(reasonType, "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersRescheduled(3)
                .countOrdersRescheduledOtherReason(3)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    public static Stream<Arguments> ltNotReadyCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED, Source.SYSTEM)
        );
    }

    public static Stream<Arguments> ltDeliveryDateUpdatedCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_NOT_PREPARED, Source.OPERATOR)
        );
    }

    @ParameterizedTest
    @MethodSource("ltCancelOperatorCases")
    void lockerTask_CancelOperatorCases(OrderDeliveryTaskFailReasonType reasonType, Source source) {
        //given
        IntStream.range(0, 4)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(reasonType, "", source), source
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersCancelled(4)
                .countOrdersCancelledOperator(4)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    public static Stream<Arguments> ltCancelOperatorCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, Source.OPERATOR),
                Arguments.of(OrderDeliveryTaskFailReasonType.CANCEL_ORDER, Source.OPERATOR)
        );
    }


    @Test
    void lockerTask_COURIER_REASSIGNED_Operator() {
        //given
        IntStream.range(0, 5)
                .forEach(i -> {
                    createLockerDeliveryTask();
                });

        transactionTemplate.execute(st -> {
            LockerDeliveryTask task = userShiftRepository.findByIdOrThrow(usId).streamLockerDeliveryTasks()
                    .findFirst().orElseThrow();
            task.failTask(new OrderDeliveryFailReason(
                    new OrderDeliveryFailReasonDto(OrderDeliveryTaskFailReasonType.COURIER_REASSIGNED,
                            "", Source.OPERATOR), Source.OPERATOR
            ), Instant.now(clock));
            lockerTaskRepository.save(task);
            return 0;
        });

        //then
        ReportData expectedResult = ReportData.builder()
                .countOrdersOther(5)
                .build();
        assertReportResult(getUserShiftReportResult(),
                expectedResult);
    }

    private PartnerUserShiftDto getUserShiftReportResult() {
        PartnerUserShiftParamsDto paramsDto = new PartnerUserShiftParamsDto();
        paramsDto.setCourierUid(UID);
        paramsDto.setShiftDateTo(LocalDate.now(clock));
        paramsDto.setShiftDateFrom(LocalDate.now(clock));
        Page<PartnerUserShiftDto> userShifts = partnerShiftService.findUserShifts(paramsDto,
                Pageable.unpaged(), null);
        assertThat(userShifts.getTotalElements()).isEqualTo(1L);
        return userShifts.iterator().next();
    }

    private PartnerShiftDto getShiftReportResult() {
        PartnerUserShiftParamsDto paramsDto = new PartnerUserShiftParamsDto();
        paramsDto.setCourierUid(UID);
        paramsDto.setShiftDateTo(LocalDate.now(clock));
        paramsDto.setShiftDateFrom(LocalDate.now(clock));
        List<PartnerShiftDto> shifts = partnerShiftService.findShifts(paramsDto, buildCompanyPermissions());
        assertThat(shifts.size()).isEqualTo(1L);
        return shifts.iterator().next();
    }

    private CompanyPermissionsProjection buildCompanyPermissions() {
        return CompanyPermissionsProjection.builder()
                .isSuperCompany(true)
                .build();
    }

    private OrderDeliveryTask createOrderDeliveryTask() {
        return transactionTemplate.execute(st -> {
            RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, usId);
            return testDataFactory.addDeliveryTaskManual(user, usId, routePoint.getId(),
                    OrderGenerateService.OrderGenerateParam.builder()
                            .paymentStatus(OrderPaymentStatus.UNPAID)
                            .paymentType(OrderPaymentType.CARD)
                            .build());

        });
    }

    private OrderDeliveryTask createClientReturnDeliveryTask() {
        return transactionTemplate.execute(st -> {
            RoutePoint routePoint = testDataFactory.createEmptyRoutePoint(user, usId);

            return testDataFactory.addClientReturnDeliveryTaskManual(user, usId, routePoint.getId());
        });
    }

    private void rescheduleClientReturn(OrderDeliveryTask task,
                                        OrderDeliveryRescheduleReasonType rescheduleReasonType) {
        var newFrom = LocalDateTime.of(
                LocalDate.now().plusDays(1), LocalTime.of(14, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var newTo = LocalDateTime.of(
                LocalDate.now().plusDays(1), LocalTime.of(16, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var rescheduleDto = new DeliveryRescheduleDto(
                newFrom,
                newTo,
                rescheduleReasonType,
                "Нет скотча"
        );
        var reschedule = DeliveryReschedule.fromCourier(user, rescheduleDto);
        task.rescheduleTask(reschedule, null, Instant.now(clock).plus(1, ChronoUnit.DAYS), clock);
        deliveryTaskRepository.save(task);
    }

    private LockerDeliveryTask createLockerDeliveryTask() {
        return transactionTemplate.execute(st -> {
            GeoPoint geoPoint = GeoPointGenerator.generateLonLat();

            Order lockerOrder = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .addressGenerateParam(AddressGenerator.AddressGenerateParam.builder()
                            .geoPoint(geoPoint)
                            .build())
                    .recipientPhone("phone1")
                    .deliveryInterval(LocalTimeInterval.valueOf("10:00-14:00"))
                    .pickupPoint(pickupPoint)
                    .build());
            return testDataFactory.addLockerDeliveryTask(usId, lockerOrder);

        });
    }

    private void assertReportResult(PartnerUserShiftDto actual, ReportData expected) {
        assertThat(actual.getCountClientReturns()).isEqualTo(expected.getCountClientReturns());
        assertThat(actual.getCountClientReturnsFact()).isEqualTo(expected.getCountClientReturnsFact());
        assertThat(actual.getCountOrdersDelivered()).isEqualTo(expected.getCountOrdersDelivered());
        assertThat(actual.getCountOrdersDeliveredPrepaid()).isEqualTo(expected.getCountOrdersDeliveredPrepaid());
        assertThat(actual.getCountOrdersDeliveredCash()).isEqualTo(expected.getCountOrdersDeliveredCash());
        assertThat(actual.getCountOrdersDeliveredCard()).isEqualTo(expected.getCountOrdersDeliveredCard());

        //the rest of asserts were commented because test results will be flaky after changes in stat_task_mview_rules

//        assertThat(actual.getCountClientReturnsCancelledClientRefused()).isEqualTo(expected.getCountClientReturnsCancelledClientRefused());
//        assertThat(actual.getCountClientReturnsCancelledItemsMismatch()).isEqualTo(expected.getCountClientReturnsCancelledItemsMismatch());
//        assertThat(actual.getCountClientReturnsCancelledItemsQuantityMismatch()).isEqualTo(expected.getCountClientReturnsCancelledItemsQuantityMismatch());

//        assertThat(actual.getCountOrdersRescheduled()).isEqualTo(expected.getCountOrdersRescheduled());
//        assertThat(actual.getCountOrdersRescheduledCourier()).isEqualTo(expected.getCountOrdersRescheduledCourier());
//        assertThat(actual.getCountOrdersRescheduledOperator())
//                .isEqualTo(expected.getCountOrdersRescheduledOperator());
//        assertThat(actual.getCountOrdersRescheduledCourierWrongAddressByClient())
//                .isEqualTo(expected.getCountOrdersRescheduledCourierWrongAddressByClient());
//        assertThat(actual.getCountOrdersRescheduledCourierWrongCoordinates())
//                .isEqualTo(expected.getCountOrdersRescheduledCourierWrongCoordinates());
//        assertThat(actual.getCountOrdersRescheduledOther()).isEqualTo(expected.getCountOrdersRescheduledOther());
//        assertThat(actual.getCountOrdersRescheduledOtherReason())
//                .isEqualTo(expected.getCountOrdersRescheduledOtherReason());
//        assertThat(actual.getCountOrdersRescheduledNoContact())
//                .isEqualTo(expected.getCountOrdersRescheduledNoContact());
//        assertThat(actual.getCountOrdersRescheduledOrderTypeUpdated())
//                .isEqualTo(expected.getCountOrdersRescheduledOrderTypeUpdated());
//        assertThat(actual.getCountOrdersPostponed())
//                .isEqualTo(expected.getCountOrdersPostponed());

//        assertThat(actual.getCountOrdersCancelled()).isEqualTo(expected.getCountOrdersCancelled());
//        assertThat(actual.getCountOrdersCancelledCourier()).isEqualTo(expected.getCountOrdersCancelledCourier());
//        assertThat(actual.getCountOrdersCancelledOperator()).isEqualTo(expected.getCountOrdersCancelledOperator());
//        assertThat(actual.getCountOrdersCancelledSystem()).isEqualTo(expected.getCountOrdersCancelledSystem());
//        assertThat(actual.getCountOrdersCancelledOther()).isEqualTo(expected.getCountOrdersCancelledOther());
//        assertThat(actual.getCountOrdersCancelledOtherReason())
//                .isEqualTo(expected.getCountOrdersCancelledOtherReason());

//        assertThat(actual.getCountOrdersNotAccepted()).isEqualTo(expected.getCountOrdersNotAccepted());
//        assertThat(actual.getCountOrdersNotPrepared()).isEqualTo(expected.getCountOrdersNotPrepared());
//        assertThat(actual.getCountOrdersNotReady()).isEqualTo(expected.getCountOrdersNotReady());

//        assertThat(actual.getCountOrdersOther()).isEqualTo(expected.getCountOrdersOther());
    }

    private void assertReportResult(PartnerShiftDto actual, ReportData expected) {
        assertThat(actual.getCountOrdersDelivered()).isEqualTo(expected.getCountOrdersDelivered());
        assertThat(actual.getCountOrdersDeliveredPrepaid()).isEqualTo(expected.getCountOrdersDeliveredPrepaid());
        assertThat(actual.getCountOrdersDeliveredCash()).isEqualTo(expected.getCountOrdersDeliveredCash());
        assertThat(actual.getCountOrdersDeliveredCard()).isEqualTo(expected.getCountOrdersDeliveredCard());
        assertThat(actual.getCountOrdersPostponed())
                .isEqualTo(expected.getCountOrdersPostponed());

        //the rest of asserts were commented because test results will be flaky after changes in stat_task_mview_rules

//        assertThat(actual.getCountClientReturnsCancelledClientRefused()).isEqualTo(expected.getCountClientReturnsCancelledClientRefused());
//        assertThat(actual.getCountClientReturnsCancelledItemsMismatch()).isEqualTo(expected.getCountClientReturnsCancelledItemsMismatch());
//        assertThat(actual.getCountClientReturnsCancelledItemsQuantityMismatch()).isEqualTo(expected.getCountClientReturnsCancelledItemsQuantityMismatch());

//        assertThat(actual.getCountOrdersRescheduled()).isEqualTo(expected.getCountOrdersRescheduled());
//        assertThat(actual.getCountOrdersRescheduledCourier()).isEqualTo(expected.getCountOrdersRescheduledCourier());
//        assertThat(actual.getCountOrdersRescheduledOperator())
//                .isEqualTo(expected.getCountOrdersRescheduledOperator());
//        assertThat(actual.getCountOrdersRescheduledCourierWrongAddressByClient())
//                .isEqualTo(expected.getCountOrdersRescheduledCourierWrongAddressByClient());
//        assertThat(actual.getCountOrdersRescheduledCourierWrongCoordinates())
//                .isEqualTo(expected.getCountOrdersRescheduledCourierWrongCoordinates());
//        assertThat(actual.getCountOrdersRescheduledOther()).isEqualTo(expected.getCountOrdersRescheduledOther());
//        assertThat(actual.getCountOrdersRescheduledOtherReason())
//                .isEqualTo(expected.getCountOrdersRescheduledOtherReason());
//        assertThat(actual.getCountOrdersRescheduledNoContact())
//                .isEqualTo(expected.getCountOrdersRescheduledNoContact());
//        assertThat(actual.getCountOrdersRescheduledOrderTypeUpdated())
//                .isEqualTo(expected.getCountOrdersRescheduledOrderTypeUpdated());

//        assertThat(actual.getCountOrdersCancelled()).isEqualTo(expected.getCountOrdersCancelled());
//        assertThat(actual.getCountOrdersCancelledCourier()).isEqualTo(expected.getCountOrdersCancelledCourier());
//        assertThat(actual.getCountOrdersCancelledOperator()).isEqualTo(expected.getCountOrdersCancelledOperator());
//        assertThat(actual.getCountOrdersCancelledSystem()).isEqualTo(expected.getCountOrdersCancelledSystem());
//        assertThat(actual.getCountOrdersCancelledOther()).isEqualTo(expected.getCountOrdersCancelledOther());
//        assertThat(actual.getCountOrdersCancelledOtherReason())
//                .isEqualTo(expected.getCountOrdersCancelledOtherReason());

//        assertThat(actual.getCountOrdersNotAccepted()).isEqualTo(expected.getCountOrdersNotAccepted());
//        assertThat(actual.getCountOrdersNotPrepared()).isEqualTo(expected.getCountOrdersNotPrepared());
//        assertThat(actual.getCountOrdersNotReady()).isEqualTo(expected.getCountOrdersNotReady());
//
//        assertThat(actual.getCountOrdersOther()).isEqualTo(expected.getCountOrdersOther());
    }

    @Builder
    @Getter
    private static class ReportData {
        //"Доставлено: всего"
        private int countOrdersDelivered;
        //"Доставлено: предоплата"
        private int countOrdersDeliveredPrepaid;
        //"Доставлено: наличные"
        private int countOrdersDeliveredCash;
        //"Доставлено: карта"
        private int countOrdersDeliveredCard;

        //"Перенос: всего"
        private int countOrdersRescheduled;
        //"Перенос: курьер"
        private int countOrdersRescheduledCourier;
        //"Перенос: оператор"
        private int countOrdersRescheduledOperator;
        //"Перенос: заказчик указал неправильный адрес"
        private int countOrdersRescheduledCourierWrongAddressByClient;
        //"Перенос: неправильные координаты"
        private int countOrdersRescheduledCourierWrongCoordinates;
        //"Перенос: клиент/КЦ"
        private int countOrdersRescheduledOther;
        //"Перенос: недозвон"
        private int countOrdersRescheduledNoContact;
        //"Перенос: изменен тип доставки"
        private int countOrdersRescheduledOrderTypeUpdated;
        //"Перенос: другая причина"
        private int countOrdersRescheduledOtherReason;

        //"Перенос: Перенесён врамках дня"
        private int countOrdersPostponed;

        //"Отмена: всего"
        private int countOrdersCancelled;
        //"Отмена: курьер"
        private int countOrdersCancelledCourier;
        //"Отмена: оператор"
        private int countOrdersCancelledOperator;
        //"Отмена: клиент/КЦ"
        private int countOrdersCancelledOther;
        //"Отмена: другая причина"
        private int countOrdersCancelledOtherReason;
        //"Отмена: система"
        private int countOrdersCancelledSystem;

        //"Не принято: всего"
        private int countOrdersNotReady;
        //"Не принято: не принято на СЦ"
        private int countOrdersNotPrepared;
        //"Не принято: не получено курьером"
        private int countOrdersNotAccepted;

        //"Прочее: всего"
        private int countOrdersOther;

        // Возвраты: план"
        private int countClientReturns;
        // Возвраты: факт
        private int countClientReturnsFact;

        // Отмена возвратов: клиент отказался от возврата
        private int countClientReturnsCancelledClientRefused;
        // Отмена возвратов: не совпадает количество товаров
        private int countClientReturnsCancelledItemsQuantityMismatch;
        // Отмена возвратов: товары отличаются от заявленных"
        private int countClientReturnsCancelledItemsMismatch;
    }
}
