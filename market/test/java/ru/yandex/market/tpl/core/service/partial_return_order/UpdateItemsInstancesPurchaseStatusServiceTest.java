package ru.yandex.market.tpl.core.service.partial_return_order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusResponseDto;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderItemInstance;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrder;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.PartialReturnStateProcessingLrm;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.PartialReturnStateProcessingLrmStatus;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.repository.PartialReturnStateProcessingLrmRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.service.partial_return.UpdateItemsInstancesPurchaseStatusService;
import ru.yandex.market.tpl.core.test.TestTplApiRequestFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class UpdateItemsInstancesPurchaseStatusServiceTest extends TplAbstractTest {
    private static final long UID = 12345L;
    private final OrderGenerateService orderGenerateService;
    private final UpdateItemsInstancesPurchaseStatusService updateItemsInstancesPurchaseStatusService;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final Clock clock;

    private final TestUserHelper testUserHelper;
    private final PartialReturnStateProcessingLrmRepository processingLrmRepository;


    private Order order1;
    private Order order2;
    private User user;



    @BeforeEach
    void init() {
        order1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .itemsCount(2)
                                        .itemsItemCount(2)
                                        .itemsPrice(BigDecimal.valueOf(120))
                                        .build()
                        )
                        .build()
        );

        order2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .itemsCount(1)
                                        .itemsItemCount(3)
                                        .itemsPrice(BigDecimal.valueOf(150))
                                        .build()
                        )
                        .deliveryPrice(BigDecimal.ONE)
                        .build()
        );

        user = testUserHelper.findOrCreateUser(UID);
    }

    @Test
    void updateItemsInstancesPurchaseStatusHappyTest() {
        buildUserShiftWithOrders(List.of(order1, order2));

        UpdateItemsInstancesPurchaseStatusRequestDto request = buildValidRequest();

        UpdateItemsInstancesPurchaseStatusResponseDto response =
                updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user);

        UpdateItemsInstancesPurchaseStatusResponseDto.TotalCostByOrder totalCostByOrder1 =
                response.getOrders().stream()
                        .filter(o -> o.getExternalOrderId().equals(order1.getExternalOrderId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(totalCostByOrder1.getTotalCost()).isEqualTo(new BigDecimal("240.00"));
        assertThat(totalCostByOrder1.isOnlyDeliveryServiceCost()).isFalse();

        UpdateItemsInstancesPurchaseStatusResponseDto.TotalCostByOrder totalCostByOrder2 =
                response.getOrders().stream()
                        .filter(o -> o.getExternalOrderId().equals(order2.getExternalOrderId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(totalCostByOrder2.getTotalCost()).isEqualTo(new BigDecimal("1.00"));
        assertThat(totalCostByOrder2.isOnlyDeliveryServiceCost()).isTrue();

        assertThat(response.getTotalCost()).isEqualTo(new BigDecimal("241.00"));

        assertThat(partialReturnOrderRepository.findByOrder(order1).isPresent()).isTrue();
        assertThat(partialReturnOrderRepository.findByOrder(order2).isPresent()).isTrue();
    }


    @Test
    void updateItemsInstancesPurchaseStatus_validationUitsSetsError() {
        //given
        buildUserShiftWithOrders(List.of(order1, order2));

        List<OrderItemInstance> instO1 = order1.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        List<OrderItemInstance> instO2 = order2.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        UpdateItemsInstancesPurchaseStatusRequestDto request = UpdateItemsInstancesPurchaseStatusRequestDto.builder()
                .orders(List.of(
                        //Skip instO1.get(1) in request
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(order1.getExternalOrderId(),
                                List.of(instO1.get(0).getUit()),
                                List.of(instO1.get(2).getUit()),
                                List.of(instO1.get(3).getUit()))
                        ,
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(order2.getExternalOrderId(),
                                null,
                                List.of(instO2.get(2).getUit()),
                                List.of(instO2.get(0).getUit(), instO2.get(1).getUit()))

                ))
                .build();

        //then
        Assertions.assertThrows(TplInvalidActionException.class, () ->
                updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user));
    }

    @Test
    void updateItemsInstancesPurchaseStatus_validationUserAccessError() {
        //given
        buildUserShiftWithOrders(List.of(order1));

        UpdateItemsInstancesPurchaseStatusRequestDto request = buildValidRequest();
        //then
        Assertions.assertThrows(TplInvalidActionException.class, () ->
                updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user));
    }

    @Test
    void updateItemsInstancesPurchaseStatus_validationObtainedByExternalSystemError() {
        //given
        buildUserShiftWithOrders(List.of(order1, order2));

        UpdateItemsInstancesPurchaseStatusRequestDto request = buildValidRequest();

        addObtainedOrderByLrm(order1);

        //then
        Assertions.assertThrows(TplInvalidActionException.class, () ->
                updateItemsInstancesPurchaseStatusService.updateItemsInstancesPurchaseStatus(request, user));
    }

    private void addObtainedOrderByLrm(Order obtainedOrder) {
        PartialReturnOrder savedPartialReturnOrder = partialReturnOrderRepository.save(PartialReturnOrder
                .builder()
                .order(obtainedOrder)
                .build());

        processingLrmRepository.save(
                PartialReturnStateProcessingLrm.builder()
                        .partialReturnOrder(savedPartialReturnOrder)
                        .status(PartialReturnStateProcessingLrmStatus.CREATED)
                        .build()
        );
    }

    private void buildUserShiftWithOrders(List<Order> orders) {
        testUserHelper.createOpenedShift(user, orders, LocalDate.now(clock));
    }

    private UpdateItemsInstancesPurchaseStatusRequestDto buildValidRequest() {
        List<OrderItemInstance> instO1 = order1.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        List<OrderItemInstance> instO2 = order2.getItems().stream()
                .filter(i -> !i.isService())
                .flatMap(OrderItem::streamInstances)
                .collect(Collectors.toList());

        return UpdateItemsInstancesPurchaseStatusRequestDto.builder()
                .orders(List.of(

                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(order1.getExternalOrderId(),
                                List.of(instO1.get(0).getUit(), instO1.get(1).getUit()),
                                List.of(instO1.get(2).getUit()),
                                List.of(instO1.get(3).getUit()))
                        ,
                        TestTplApiRequestFactory.buildUpdateOrderItemRequest(order2.getExternalOrderId(),
                                null,
                                List.of(instO2.get(2).getUit()),
                                List.of(instO2.get(0).getUit(), instO2.get(1).getUit()))

                ))
                .build();
    }
}
