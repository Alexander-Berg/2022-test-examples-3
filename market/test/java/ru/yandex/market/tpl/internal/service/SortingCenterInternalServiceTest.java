package ru.yandex.market.tpl.internal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.company.CompanyForScDto;
import ru.yandex.market.tpl.api.model.order.partner.ScDamagedOrder;
import ru.yandex.market.tpl.api.model.order.partner.ScDamagedOrderList;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.user.sc.CourierForScDto;
import ru.yandex.market.tpl.api.model.user.sc.CourierForScListDto;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.company.Company;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.tvm.service.ServiceTicketRequest;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserUtil;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.tracking.TrackingRepository;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.internal.controller.TplIntTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.CallRequirement.CALL_REQUIRED;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.CLIENT_ASK_NOT_TO_CALL;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.FAILED;
import static ru.yandex.market.tpl.api.model.task.call.CallToRecipientTaskStatus.NOT_CALLED;
import static ru.yandex.market.tpl.core.domain.company.Company.DEFAULT_COMPANY_NAME;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@TplIntTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortingCenterInternalServiceTest {

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService userShiftCommandService;
    private final TestUserHelper testHelper;
    private final SortingCenterInternalService sortingCenterService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final UserShiftRepository userShiftRepository;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final TrackingRepository trackingRepository;
    private final TrackingService trackingService;
    private final SortingCenterRepository sortingCenterRepository;

    private SortingCenter sortingCenter;
    private User courier1;
    private User courier2;

    @BeforeEach
    void setUp() {
        courier1 = testHelper.findOrCreateUser(1L);
        courier2 = testHelper.findOrCreateUser(2L);
        Set<User> couriers = new LinkedHashSet<>(2);
        couriers.add(courier1);
        couriers.add(courier2);
        Company company = testHelper.addCouriersToCompany(DEFAULT_COMPANY_NAME, couriers);
        sortingCenter = testHelper.sortingCenter(1L, Set.of(company));
    }

    @Test
    void getCouriersForSortingCentersWithSameToken() {
        var newCourier1 = testHelper.findOrCreateUser(3L);
        var newCourier2 = testHelper.findOrCreateUser(4L);
        Set<User> couriersForNewCompany = new LinkedHashSet<>(2);
        couriersForNewCompany.add(newCourier1);
        couriersForNewCompany.add(newCourier2);
        Company newCompany = testHelper.addCouriersToCompany(DEFAULT_COMPANY_NAME, couriersForNewCompany);

        var sortingCenter2 = testHelper.sortingCenter(2L, Set.of(newCompany));
        sortingCenter2.setToken(sortingCenter.getToken());
        sortingCenterRepository.save(sortingCenter2);

        //when
        CourierForScListDto actual = sortingCenterService.getCouriersForSortingCenter(sortingCenter.getId(),
                sortingCenter.getToken(),
                null);

        //then
        var expected = List.of(
                new CourierForScDto(
                        1L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null),
                new CourierForScDto(
                        2L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null),
                new CourierForScDto(
                        3L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null),
                new CourierForScDto(
                        4L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null)
        );
        assertThat(actual.getCouriers()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void getCouriersForSortingCenter() {
        //when
        CourierForScListDto actual = sortingCenterService.getCouriersForSortingCenter(sortingCenter.getId(),
                sortingCenter.getToken(),
                null);

        //then
        var expected = List.of(
                new CourierForScDto(
                        1L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null),
                new CourierForScDto(
                        2L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null)
        );
        assertThat(actual.getCouriers()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void getCouriersForSortingCenterUseOnlyId() {
        //when
        CourierForScListDto actual = sortingCenterService.getCouriersForSortingCenter(sortingCenter.getId(),
                null,
                null);

        //then
        var expected = List.of(
                new CourierForScDto(
                        1L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null),
                new CourierForScDto(
                        2L, UserUtil.NAME, new CompanyForScDto(DEFAULT_COMPANY_NAME), null, null)
        );
        assertThat(actual.getCouriers()).containsExactlyInAnyOrderElementsOf(expected);
    }

    @Test
    void getCouriersForSortingCenterWithDate() {
        var now = LocalDate.now(clock);
        transactionTemplate.execute(ts -> {
            var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .deliveryDate(now)
                    .deliveryServiceId(sortingCenter.getDeliveryServices().iterator().next().getId())
                    .build());

            testHelper.createOpenedShift(courier1, order, now, sortingCenter.getId());
            return null;
        });

        CourierForScListDto actual = sortingCenterService.getCouriersForSortingCenter(sortingCenter.getId(),
                sortingCenter.getToken(),
                now);
        assertThat(actual.getCouriers()).extracting(CourierForScDto::getUserShiftDate).containsOnly(now);
        assertThat(actual.getCouriers()).extracting(CourierForScDto::getUserShiftStart).isNotEmpty();
    }

    @Test
    void getDamagedOrdersByExternalIdsNotDamaged() {
        createNotDamagedOrder("1");
        assertThat(sortingCenterService.getDamagedOrders(List.of("1")))
                .isEqualTo(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", false))));
    }

    @Test
    void getDamagedOrdersByDateIntervalNotDamaged() {
        var order = createNotDamagedOrder("1");
        assertThat(sortingCenterService.getDamagedOrders(
                order.getTask().getCreatedAt().minusSeconds(1),
                order.getTask().getUpdatedAt().plusSeconds(1)
        ))
                .isEqualTo(new ScDamagedOrderList(Collections.emptyList()));
    }

    @Test
    void getDamagedOrdersByExternalIdsDamaged() {
        markOrderDamaged(createNotDamagedOrder("1"));
        assertThat(sortingCenterService.getDamagedOrders(List.of("1")))
                .isEqualTo(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", true))));
    }

    @Test
    void getDamagedOrdersByDateIntervalDamaged() {
        var order = markOrderDamaged(createNotDamagedOrder("1"));
        assertThat(sortingCenterService.getDamagedOrders(
                order.getTask().getCreatedAt().minusSeconds(1),
                order.getTask().getUpdatedAt().plusSeconds(1)
        ))
                .isEqualTo(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", true))));
    }

    @Test
    void getDamagedOrdersByExternalIdsReverted() {
        revertOrderDamaged(
                markOrderDamaged(createNotDamagedOrder("1"))
        );
        assertThat(sortingCenterService.getDamagedOrders(List.of("1")))
                .isEqualTo(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", false))));
    }

    @Test
    void testHasDoNotCallStatus() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        OrderWithTask order = revertOrderDamaged(
                markOrderDamaged(createNotDamagedOrder("1", DO_NOT_CALL_DELIVERY_PREFIX))
        );
        ScDamagedOrderList damagedOrders = sortingCenterService.getDamagedOrders(List.of("1"));
        assertThat(damagedOrders)
                .isEqualTo(new ScDamagedOrderList(List.of(new ScDamagedOrder("1", false))));

        OrderDeliveryTask task = transactionTemplate.execute(ts -> {
            return userShiftRepository.findTasksByOrderId(order.getOrder().getId())
                    .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class));
        });

        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(CLIENT_ASK_NOT_TO_CALL);
    }

    @Test
    void revertedDoNotCallStatus() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.DO_NOT_CALL_ENABLED, true);

        OrderWithTask order = markOrderDamaged(createNotDamagedOrder("1", DO_NOT_CALL_DELIVERY_PREFIX));

        var tracking = trackingRepository.findByOrderExternalOrderId(order.getOrder().getExternalOrderId());
        ServiceTicketRequest serviceTicket = new ServiceTicketRequest();
        trackingService.updateCallRequirement(tracking.get().getId(), CALL_REQUIRED, serviceTicket);

        OrderDeliveryTask task = transactionTemplate.execute(ts -> {
            return userShiftRepository.findTasksByOrderId(order.getOrder().getId())
                    .findFirst().orElseThrow(() -> new TplEntityNotFoundException(OrderDeliveryTask.class));
        });

        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(NOT_CALLED);
        reopenOrder(order);
        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(NOT_CALLED);
        failOrder(order);
        assertThat(task.getCallToRecipientTask().getStatus()).isEqualTo(FAILED);
    }

    @Test
    void getDamagedOrdersByDateIntervalReverted() {
        var order = revertOrderDamaged(
                markOrderDamaged(createNotDamagedOrder("1"))
        );
        assertThat(sortingCenterService.getDamagedOrders(
                order.getTask().getCreatedAt().minusSeconds(1),
                order.getTask().getUpdatedAt().plusSeconds(1)
        ))
                .isEqualTo(new ScDamagedOrderList(Collections.emptyList()));
    }

    private OrderWithTask createNotDamagedOrder(@SuppressWarnings("SameParameterValue") String externalId) {
        return createNotDamagedOrder(externalId, "");
    }

    private OrderWithTask createNotDamagedOrder(@SuppressWarnings("SameParameterValue") String externalId,
                                                String recipientNotes) {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .externalOrderId(externalId)
                        .recipientNotes(recipientNotes)
                        .build()
        );
        var userShift = testHelper.createShiftWithDeliveryTask(
                testHelper.findOrCreateUser(1L),
                UserShiftStatus.SHIFT_OPEN,
                order
        );
        return new OrderWithTask(
                order,
                userShift.streamOrderDeliveryTasks()
                        .filter(t -> Objects.equals(order.getId(), t.getOrderId()))
                        .findFirst().orElseThrow()
        );
    }

    private OrderWithTask markOrderDamaged(OrderWithTask order) {
        testHelper.checkinAndFinishPickup(order.getTask().getRoutePoint().getUserShift());
        testHelper.finishDelivery(order.getTask().getRoutePoint(), OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED);
        return order;
    }

    private OrderWithTask revertOrderDamaged(OrderWithTask order) {
        reopenOrder(order);
        failOrder(order);
        return order;
    }

    private void reopenOrder(OrderWithTask order) {
        userShiftCommandService.reopenDeliveryTask(
                order.getTask().getRoutePoint().getUserShift().getUser(),
                new UserShiftCommand.ReopenOrderDeliveryTask(
                        order.getTask().getRoutePoint().getUserShift().getId(),
                        order.getTask().getRoutePoint().getId(),
                        order.getTask().getId(),
                        Source.COURIER
                ));
    }

    private void failOrder(OrderWithTask order) {
        userShiftCommandService.failDeliveryTask(
                order.getTask().getRoutePoint().getUserShift().getUser(),
                new UserShiftCommand.FailOrderDeliveryTask(
                        order.getTask().getRoutePoint().getUserShift().getId(),
                        order.getTask().getRoutePoint().getId(),
                        order.getTask().getId(),
                        new OrderDeliveryFailReason(
                                OrderDeliveryTaskFailReasonType.CLIENT_REFUSED,
                                null
                        )
                )
        );
    }

    @Value
    private static class OrderWithTask {

        Order order;
        OrderDeliveryTask task;

    }


}
