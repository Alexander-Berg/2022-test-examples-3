package ru.yandex.market.tpl.core.domain.partial_return_order;

import java.time.Clock;
import java.time.LocalDate;

import javax.persistence.EntityManager;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.task.OrderReturnTaskDto;
import ru.yandex.market.tpl.api.model.task.TaskDto;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderReturnTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftBatchService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.query.usershift.UserShiftQueryService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class PartialReturnOrderMapTaskOrderReturnWithMultiOrderTest extends TplAbstractTest {
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final UserShiftBatchService userShiftBatchService;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final EntityManager entityManager;
    private final UserShiftQueryService userShiftQueryService;

    private User user;
    private Shift shift;
    private UserShift userShift;
    private Order multiOrder1;
    private Order multiOrder2;
    private OrderDeliveryTask orderDeliveryTask;
    private OrderReturnTask orderReturnTask;

    @BeforeEach
    public void init() {
        transactionTemplate.execute(ts -> {
                    user = testUserHelper.findOrCreateUser(1L);
                    shift = testUserHelper.findOrCreateOpenShiftForSc(
                            LocalDate.now(clock),
                            sortingCenterService.findSortCenterForDs(239).getId()
                    );
                    userShift = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(),
                            user));

                    AddressGenerator.AddressGenerateParam addressGenerateParam =
                            AddressGenerator.AddressGenerateParam.builder()
                            .geoPoint(GeoPointGenerator.generateLonLat())
                            .build();

                    multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .externalOrderId("451345234")
                            .buyerYandexUid(1L)
                            .deliveryDate(LocalDate.now(clock))
                            .deliveryServiceId(239L)
                            .paymentType(OrderPaymentType.CASH)
                            .paymentStatus(OrderPaymentStatus.PAID)
                            .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                            .addressGenerateParam(addressGenerateParam)
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .items(
                                    OrderGenerateService.OrderGenerateParam.Items.builder()
                                            .isFashion(true)
                                            .build()
                            )
                            .build());

                    multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                            .externalOrderId("4323451234")
                            .buyerYandexUid(1L)
                            .deliveryDate(LocalDate.now(clock))
                            .deliveryServiceId(239L)
                            .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                            .addressGenerateParam(addressGenerateParam)
                            .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                            .items(
                                    OrderGenerateService.OrderGenerateParam.Items.builder()
                                            .isFashion(true)
                                            .build()
                            )
                            .build());

                    userShiftReassignManager.assign(userShift, multiOrder1);
                    userShiftReassignManager.assign(userShift, multiOrder2);
                    testUserHelper.checkinAndFinishPickup(userShift);
                    orderDeliveryTask = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
                    orderReturnTask = userShift.streamReturnRoutePoints().findFirst().get().getOrderReturnTask();
                    return null;
                }
        );
    }

    @Test
    void mapTaskOrderReturn() {
        testUserHelper.finishAllDeliveryTasks(userShift);
        var partialReturnOrder1 =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(multiOrder1);
        partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder1, 2);

        var partialReturnOrder2 =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(multiOrder2);
        partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder2, 2);

        TaskDto task = transactionTemplate.execute(ts -> {
            return userShiftQueryService.getTaskInfo(user, orderReturnTask.getRoutePoint().getId(), orderReturnTask.getId());
        });

        OrderReturnTaskDto returnTask = (OrderReturnTaskDto) task;
        returnTask.getOrders();
        assertThat(returnTask.getOrders()).hasSize(4);
        assertThat(returnTask.getDestinations()).hasSize(2);
        assertThat(returnTask.getDestinations().get(0).getOutsideOrders()).hasSize(2);
        assertThat(returnTask.getDestinations().get(1).getOutsideOrders()).hasSize(2);
        returnTask.getDestinations().forEach(
                d -> assertThat(d.getDelivery()).isNotNull()
        );
    }
}
