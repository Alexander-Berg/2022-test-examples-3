package ru.yandex.market.tpl.core.service.axapta;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.tpl.api.model.axapta.AxaptaChequeInfoDto;
import ru.yandex.market.tpl.api.model.axapta.AxaptaChequeParamsDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;
import ru.yandex.market.tpl.core.domain.usershift.Task;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.user.User;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.order.OrderFlowStatus.TRANSPORTATION_RECIPIENT;
import static ru.yandex.market.tpl.core.test.TestDataFactory.DELIVERY_SERVICE_ID;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AxaptaChequeServiceTest {
    private static final int ORDERS_COUNT = 20;

    private final TestUserHelper testUserHelper;
    private final Clock clock;
    private final AxaptaChequeService axaptaChequeService;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftCommandDataHelper helper;

    private User user;
    private Map<String, Order> ordersByExternalOrderId;
    private Map<Long, Order> ordersById;
    private Instant before;
    private UserShift userShift;

    @BeforeEach
    public void init() {
        before = Instant.now();
        ordersByExternalOrderId = generateOrders().stream()
                .collect(Collectors.toMap(Order::getExternalOrderId, Function.identity()));
        ordersById = ordersByExternalOrderId.values().stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        LocalDate date = LocalDate.now(clock);
        user = testUserHelper.findOrCreateUser(824125L, date);
        userShift = testUserHelper.createEmptyShift(user, date);
        ordersByExternalOrderId.values()
                .forEach(order -> testUserHelper.addDeliveryTaskToShift(user, userShift, order));
        testUserHelper.checkinAndFinishPickup(userShift);
        userShift.streamDeliveryRoutePoints()
                .forEach(rp -> testUserHelper.finishDelivery(rp, null, getPaymentType(rp), true));
    }

    @Test
    public void testGetAllCheques() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());

        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();

        assertThat(chequeData).hasSize(ORDERS_COUNT);
        validate(chequeData);
    }

    @Test
    public void testGetAllChequesWithRevertedCheques() {
        revertCheques();
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());

        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();

        assertThat(chequeData).hasSize(ORDERS_COUNT * 2);
        validate(chequeData);
        validateRevertedCheques(chequeData);
    }

    @Test
    public void testFilterByDateFrom() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(Instant.now());
        params.setChequeDateTo(Instant.now());
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT);
    }

    @Test
    public void testFilterByDateTo() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(before);
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(0);
    }

    @Test
    public void testFilterByShiftId() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());
        params.setShiftNumber(1);
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT);

        params.setShiftNumber(777);
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(0);
    }

    @Test
    public void testFilterByOrderId() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());
        params.setOrderId(
                ordersByExternalOrderId.keySet().stream()
                        .findFirst()
                        .orElseThrow()
        );
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(1);

        revertCheques();
        params.setChequeDateTo(Instant.now());
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(2);

        params.setOrderId("wrong_order_id");
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(0);
    }

    @Test
    public void testFilterByPaymentType() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());
        params.setPaymentType(OrderPaymentType.CASH);
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT / 2);

        params.setPaymentType(OrderPaymentType.CARD);
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT / 2);

        params.setPaymentType(OrderPaymentType.PREPAID);
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(0);
    }

    @Test
    public void testFilterByChequeType() {
        AxaptaChequeParamsDto params = new AxaptaChequeParamsDto();
        params.setChequeDateFrom(before);
        params.setChequeDateTo(Instant.now());
        params.setChequeType(OrderChequeType.SELL);
        var chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT);

        params.setChequeType(OrderChequeType.RETURN);
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(0);

        revertCheques();
        params.setChequeDateTo(Instant.now());
        chequeData = axaptaChequeService.findCheques(params, Pageable.unpaged()).getContent();
        assertThat(chequeData).hasSize(ORDERS_COUNT);
    }

    private List<Order> generateOrders() {
        return IntStream.range(0, ORDERS_COUNT)
                .mapToObj(i -> orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .deliveryDate(LocalDate.now(clock))
                                .deliveryServiceId(DELIVERY_SERVICE_ID)
                                .paymentType(i % 2 == 0 ? OrderPaymentType.CARD : OrderPaymentType.CASH)
                                .flowStatus(TRANSPORTATION_RECIPIENT)
                                .build()))
                .collect(Collectors.toList());
    }

    private OrderPaymentType getPaymentType(RoutePoint rp) {
        return rp.streamOrderDeliveryTasks()
                .map(OrderDeliveryTask::getOrderId)
                .filter(Objects::nonNull)
                .map(ordersById::get)
                .map(Order::getPaymentType)
                .findFirst()
                .orElseThrow();
    }

    private void revertCheques() {
        userShift.streamDeliveryRoutePoints()
                .forEach(rp -> commandService.returnCheque(user, new UserShiftCommand.PrintOrReturnCheque(
                        userShift.getId(),
                        rp.getId(),
                        rp.streamOrderDeliveryTasks()
                                .map(Task::getId)
                                .findFirst()
                                .orElseThrow(),
                        helper.getChequeDto(getPaymentType(rp)),
                        Instant.now(clock),
                        false,
                        null,
                        Optional.empty()
                )));
    }

    private void validate(List<AxaptaChequeInfoDto> chequeData) {
        var orderIds = chequeData.stream()
                .map(AxaptaChequeInfoDto::getOrderId)
                .collect(Collectors.toList());
        assertThat(orderIds).containsAll(ordersByExternalOrderId.keySet());
        chequeData.forEach(cheque -> {
            var order = ordersByExternalOrderId.get(cheque.getOrderId());
            assertThat(cheque.getDeliveryServiceId()).isEqualTo(order.getDeliveryServiceId());
            var orderCheque = order.getCheques().stream()
                    .filter(oc -> oc.getId().equals(cheque.getCheque().getChequeId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(cheque.getCheque().getChequeSum()).isEqualTo(orderCheque.getTotal());
            assertThat(cheque.getCheque().getIncomeChequeId()).isEqualTo(orderCheque.getOriginalChequeId());
            assertThat(cheque.getCheque().getKktId()).isEqualTo(orderCheque.getFptrNumber());
            assertThat(cheque.getCheque().getShiftNumber()).isEqualTo(orderCheque.getShiftNumber());
            assertThat(cheque.getCheque().getChequeType()).isEqualTo(orderCheque.getChequeType());
            assertThat(cheque.getCheque().getPaymentType()).isEqualTo(orderCheque.getPaymentType());
            assertThat(cheque.getCourier().getName()).isEqualTo(user.getName());
            assertThat(cheque.getCourier().getUid()).isEqualTo(user.getId());
        });
    }

    private void validateRevertedCheques(List<AxaptaChequeInfoDto> chequeData) {
        var reverted = chequeData.stream()
                .filter(chequeInfo -> chequeInfo.getCheque().getChequeType() == OrderChequeType.RETURN)
                .collect(Collectors.toList());
        assertThat(reverted).hasSize(ORDERS_COUNT);
        reverted.forEach(revertedCheque -> {
            var cheque = revertedCheque.getCheque();
            assertThat(cheque.getIncomeChequeId()).isNotNull();
            var incomeCheque = ordersByExternalOrderId.get(revertedCheque.getOrderId())
                    .getCheques().stream()
                    .filter(orderCheque -> orderCheque.getId().equals(cheque.getIncomeChequeId()))
                    .findFirst()
                    .orElseThrow();
            assertThat(incomeCheque).isNotNull();
            assertThat(cheque.getPaymentType()).isEqualTo(incomeCheque.getPaymentType());
            assertThat(cheque.getChequeSum()).isEqualTo(incomeCheque.getTotal());
        });
    }
}
