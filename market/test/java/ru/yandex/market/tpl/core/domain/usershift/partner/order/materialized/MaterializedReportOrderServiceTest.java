package ru.yandex.market.tpl.core.domain.usershift.partner.order.materialized;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.partner.order.BasePartnerReportOrder;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static ru.yandex.market.tpl.core.util.TplCoreTestUtils.OBJECT_GENERATOR;

@RequiredArgsConstructor
class MaterializedReportOrderServiceTest extends TplAbstractTest {

    private static final long UID = OBJECT_GENERATOR.nextLong();

    private final TransactionTemplate transactionTemplate;
    private final OrderDeliveryTaskRepository deliveryTaskRepository;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final Clock clock;
    private final MaterializedReportOrderService reportOrderService;

    private long usId;
    private User user;

    @BeforeEach
    void setUp() {
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(UID, date);
        Shift shift = testUserHelper.findOrCreateOpenShift(date);
        usId = testDataFactory.createEmptyShift(shift.getId(), user);
    }

    @ParameterizedTest
    @MethodSource("reportCases")
    @Disabled //will be flaky cause of stat_task_mview_rules
    void findOrders(OrderDeliveryTaskFailReasonType reasonType, Source source,
                    PartnerReportOrderParamsDto.OrderFilter orderFilter,
                    Predicate<BasePartnerReportOrder> checkPredicate) {
        //given
        OrderDeliveryTask task = createOrderDeliveryTask();
        task.failTask(new OrderDeliveryFailReason(
                new OrderDeliveryFailReasonDto(reasonType, "", source), source),
                Instant.now(clock));
        deliveryTaskRepository.save(task);

        var paramsDto = new PartnerReportOrderParamsDto();
        paramsDto.setOrderFilter(orderFilter);

        //when
        var result = reportOrderService.findOrders(paramsDto, null,
                Pageable.unpaged()).getContent();

        //then
        Assertions.assertThat(result).hasSize(1);
        Assertions.assertThat(checkPredicate.test(result.get(0))).isTrue();
    }

    public static Stream<Arguments> reportCases() {
        return Stream.of(
                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED, Source.COURIER,
                        PartnerReportOrderParamsDto.OrderFilter.cancelledCourier,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isCancelledCourier),

                Arguments.of(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED, Source.COURIER,
                        PartnerReportOrderParamsDto.OrderFilter.rescheduledOtherReason,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isRescheduledOtherReason),

                Arguments.of(OrderDeliveryTaskFailReasonType.DELIVERY_DATE_UPDATED, Source.OPERATOR,
                        PartnerReportOrderParamsDto.OrderFilter.rescheduledOperator,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isRescheduledOperator),

                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_WAS_LOST, Source.OPERATOR,
                        PartnerReportOrderParamsDto.OrderFilter.cancelledOperator,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isCancelledOperator),

                Arguments.of(OrderDeliveryTaskFailReasonType.TASK_CANCELLED, Source.SYSTEM,
                        PartnerReportOrderParamsDto.OrderFilter.cancelledSystem,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isCancelledSystem),

                Arguments.of(OrderDeliveryTaskFailReasonType.OTHER, Source.COURIER,
                        PartnerReportOrderParamsDto.OrderFilter.cancelledOtherReason,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isCancelledOtherReason),

                Arguments.of(OrderDeliveryTaskFailReasonType.ORDER_NOT_ACCEPTED, Source.DELIVERY,
                        PartnerReportOrderParamsDto.OrderFilter.orderOther,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isOrderOther),

                Arguments.of(OrderDeliveryTaskFailReasonType.POSTPONED, Source.COURIER,
                        PartnerReportOrderParamsDto.OrderFilter.orderPostponed,
                        (Predicate<BasePartnerReportOrder>) BasePartnerReportOrder::isOrderPostponed)

        );
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
}
