package ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.common.db.queue.log.QueueLog;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogEvent;
import ru.yandex.market.tpl.common.db.queue.log.QueueLogRepository;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.lrm.client.api.ReturnsApi;
import ru.yandex.market.tpl.common.lrm.client.model.CreateReturnRequest;
import ru.yandex.market.tpl.common.lrm.client.model.CreateReturnResponse;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnBoxRequest;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnCourier;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnItem;
import ru.yandex.market.tpl.common.lrm.client.model.ReturnSource;
import ru.yandex.market.tpl.common.lrm.client.model.UpdateReturnRequest;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.order.OrderItem;
import ru.yandex.market.tpl.core.domain.order.OrderManager;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderCommandService;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.commands.LinkBoxesPartialReturnOrderCommandHandler;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.commands.PartialReturnStateProcessingLrmCommand;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.dbqueue.ProcessPartialReturnOrderPayload;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.dbqueue.ProcessPartialReturnOrderProcessingService;
import ru.yandex.market.tpl.core.domain.partial_return_order.state_processing_lrm.repository.PartialReturnStateProcessingLrmRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.partial_return.UpdateLrmPartialReturnService;
import ru.yandex.market.tpl.core.service.vehicle.VehicleGenerateService;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;

@RequiredArgsConstructor
public class StateProcessingLrmTest extends TplAbstractTest {
    private final PartialReturnOrderCommandService partialReturnOrderCommandService;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final OrderGenerateService orderGenerateService;
    private final OrderRepository orderRepository;
    private final PartialReturnStateProcessingLrmRepository processingLrmRepository;
    private final QueueLogRepository queueLogRepository;
    private final ProcessPartialReturnOrderProcessingService processingService;
    private final UpdateLrmPartialReturnService updateLrmPartialReturnService;
    private final TransactionTemplate transactionTemplate;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final VehicleGenerateService vehicleGenerateService;
    private final UserRepository userRepository;

    private final PartialReturnStateProcessingLrmCommandService processingLrmCommandService;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final TestUserHelper testUserHelper;

    private UserShift userShift;
    private final ReturnsApi returnsApi;

    @BeforeEach
    public void init() {
        User user = testUserHelper.findOrCreateUser(123);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now());
        var vehicle = vehicleGenerateService.generateVehicle();
        vehicleGenerateService.assignVehicleToUser(VehicleGenerateService.VehicleInstanceGenerateParam.builder()
                .users(List.of(user))
                .registrationNumber("A000AA")
                .registrationNumberRegion("111")
                .vehicle(vehicle)
                .build());

        Mockito.when(returnsApi.createReturn(any(), isNull())).thenReturn(new CreateReturnResponse().id(1L));
    }

    /**
     * Даже пустой {@link AfterEach} нужен
     * для срабатывания {@link ru.yandex.market.tpl.core.test.TplAbstractTest#clearAfterTest(Object)}
     */
    @AfterEach
    void afterEach() {
        Mockito.clearInvocations(returnsApi);
    }

    @Test
    public void processCreatePartialReturn() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        assertThat(partialReturnOrderRepository.findAll()).hasSize(1);


        PartialReturnStateProcessingLrm partialReturnStateProcessingLrm = processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );

        dbQueueTestUtil.assertQueueHasSize(QueueType.PROCESS_PARTIAL_RETURN_ORDER, 1);

        dbQueueTestUtil.executeAllQueueItems(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        var createReturnRequestCaptor = ArgumentCaptor.forClass(CreateReturnRequest.class);

        Mockito.verify(returnsApi).createReturn(createReturnRequestCaptor.capture(), isNull());

        partialReturnStateProcessingLrm =
                processingLrmRepository.findByIdOrThrow(partialReturnStateProcessingLrm.getId());

        assertThat(partialReturnStateProcessingLrm.getStatus()).isEqualTo(PartialReturnStateProcessingLrmStatus.CREATED);

        var sentRequest = createReturnRequestCaptor.getValue();
        var user = userRepository.findByIdWithVehicles(userShift.getUser().getId()).orElseThrow();

        var returnItems = transactionTemplate.execute(ts -> {
            var instance = orderRepository.findByIdOrThrow(order.getId())
                    .getItems().stream()
                    .flatMap(OrderItem::streamReturnedInstances).findFirst().orElseThrow();
            var cisAndUits = new HashMap<String, String>();
            cisAndUits.put(OrderManager.CIS_INSTANCE_KEY, instance.getCis());
            cisAndUits.put(OrderManager.UIT_INSTANCE_KEY, instance.getUit());
            var supplierId = instance.getOrderItem().getVendorArticle().getVendorId();
            return List.of(
                    new ReturnItem()
                            .supplierId(supplierId)
                            .instances(cisAndUits)
                            .vendorCode(instance.getOrderItem().getArticle())
                    );
        });

        var expectedRequest = new CreateReturnRequest()
                .source(ReturnSource.COURIER)
                .orderExternalId(order.getExternalOrderId())
                .partnerFromId(userShift.getShift().getSortingCenter().getId())
                .courier(
                        new ReturnCourier()
                                .carNumber(user.getVehicleNumber())
                                .name(user.getFullName())
                                .uid(String.valueOf(user.getUid()))
                )
                .full(false)
                .items(returnItems);

        assertThat(returnItems).containsExactlyInAnyOrder(sentRequest.getItems().toArray(new ReturnItem[0]));
        // исключаю тут проверку, т.к. проверил соответствие айтемов выше
        assertThat(expectedRequest).isEqualToIgnoringGivenFields(sentRequest, "items");

        // 3, т.к. в очереди должно быть 3 евента new, execute, successful
        dbQueueTestUtil.assertQueueHasSize(QueueType.PROCESS_PARTIAL_RETURN_ORDER, 3);

        assertQueueLogValid(
                partialReturnOrder.getId(),
                Set.of(QueueLogEvent.EXECUTE, QueueLogEvent.SUCCESSFUL),
                Set.of(QueueLogEvent.FAILED)
        );

        partialReturnOrder = partialReturnOrderRepository.findByIdOrThrow(partialReturnOrder.getId());
        // 1 т.к. указали в beforeEach что returnsApi будет возвращать 1
        assertThat(partialReturnOrder.getExternalReturnId()).isEqualTo(1);

    }

    @Test
    public void processCommitPartialReturn() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        PartialReturnStateProcessingLrm partialReturnStateProcessingLrm = processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        processingService.processPayload(new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );

        processingService.processPayload(new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );

        var commitReturnRequestCaptor = ArgumentCaptor.forClass(Long.class);

        Mockito.verify(returnsApi).commitReturn(commitReturnRequestCaptor.capture());

        var sentExternalId = commitReturnRequestCaptor.getValue();
        var expectedExternalId = partialReturnOrderRepository.findByIdOrThrow(partialReturnOrder.getId())
                .getExternalReturnId();

        assertThat(expectedExternalId).isEqualTo(sentExternalId);

        partialReturnStateProcessingLrm =
                processingLrmRepository.findByIdOrThrow(partialReturnStateProcessingLrm.getId());

        assertThat(partialReturnStateProcessingLrm.getStatus()).isEqualTo(PartialReturnStateProcessingLrmStatus.COMMITTED);
    }

    @Test
    public void processUpdatePartialReturn() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        partialReturnOrderCommandService.handleCommand(
                LinkBoxesPartialReturnOrderCommandHandler.builder()
                        .partialReturnId(partialReturnOrder.getId())
                        .newBoxesBarcodes(List.of("barcode1", "barcode2", "Hello there"))
                        .build()
        );

        PartialReturnStateProcessingLrm partialReturnStateProcessingLrm = processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        processingService.processPayload(new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );

        partialReturnOrder = partialReturnOrderRepository.findByIdOrThrow(partialReturnOrder.getId());


        var commitReturnRequestCaptor = ArgumentCaptor.forClass(UpdateReturnRequest.class);

        Mockito.verify(returnsApi).updateReturn(
                any(),
                commitReturnRequestCaptor.capture()
        );

        var partialReturnOrderId = partialReturnOrder.getId();
        var sentRequest = commitReturnRequestCaptor.getValue();
        var expectedReturnItems = transactionTemplate.execute(
                ts -> updateLrmPartialReturnService.getReturnItems(orderRepository.findByIdOrThrow(order.getId()))
        );
        var expectedReturnBoxes = transactionTemplate.execute(
                ts -> partialReturnOrderRepository.findByIdOrThrow(partialReturnOrderId).streamBoxes().map(
                        box -> new ReturnBoxRequest().externalId(box.getBarcode())
                ).collect(Collectors.toList())
        );

        assertThat(expectedReturnItems).containsExactlyInAnyOrder(sentRequest.getItems().toArray(new ReturnItem[0]));
        assertThat(expectedReturnBoxes).containsExactlyInAnyOrder(sentRequest.getBoxes().toArray(new ReturnBoxRequest[0]));

        partialReturnStateProcessingLrm =
                processingLrmRepository.findByIdOrThrow(partialReturnStateProcessingLrm.getId());

        assertThat(partialReturnStateProcessingLrm.getStatus()).isEqualTo(PartialReturnStateProcessingLrmStatus.UPDATED);
    }

    @Test
    public void processUpdatePartialReturnWithoutExternalReturnId() {
        Mockito.when(returnsApi.createReturn(any(), isNull())).thenReturn(new CreateReturnResponse().id(null));

        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        Long returnOrderId = partialReturnOrder.getId();
        partialReturnOrderCommandService.handleCommand(
                LinkBoxesPartialReturnOrderCommandHandler.builder()
                        .partialReturnId(returnOrderId)
                        .newBoxesBarcodes(List.of("barcode1", "barcode2", "Hello there"))
                        .build()
        );

        PartialReturnStateProcessingLrm partialReturnStateProcessingLrm = processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(returnOrderId)
                        .userShiftId(userShift.getId())
                        .build()
        );

        dbQueueTestUtil.executeSingleQueueItem(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        var exception = assertThrows(
                TplInvalidActionException.class,
                () -> processingService.processPayload(new ProcessPartialReturnOrderPayload(
                                null,
                                returnOrderId,
                                userShift.getId(),
                                PartialReturnStateProcessingLrm.ActionType.UPDATE
                        )
                )
        );

        assertThat(exception.getMessage())
                .isEqualTo("externalReturnId for partial returnOrder " + returnOrderId + " is null");
    }

    @Test
    public void testProcessingServiceDelay() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        assertThat(partialReturnOrderRepository.findAll()).hasSize(1);

        PartialReturnStateProcessingLrm partialReturnStateProcessingLrm = processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );

        dbQueueTestUtil.assertQueueHasSize(QueueType.PROCESS_PARTIAL_RETURN_ORDER, 1);

        dbQueueTestUtil.executeAllQueueItems(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        partialReturnStateProcessingLrm =
                processingLrmRepository.findByIdOrThrow(partialReturnStateProcessingLrm.getId());

        assertThat(partialReturnStateProcessingLrm.getStatus()).isEqualTo(PartialReturnStateProcessingLrmStatus.CREATED);

        var delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );
        // Должна быть задержка, т.к. не можем закоммить без евента update
        assertThat(delayO.isPresent()).isTrue();

        delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );

        // Задержки нет, т.к. спокойно можем обновить сущность после её создания
        assertThat(delayO.isEmpty()).isTrue();

        processingService.processPayload(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );
        delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );

        // Задержки нет, т.к. можем коммитить после апдейта
        assertThat(delayO.isEmpty()).isTrue();

    }

    @Test
    public void testProcessingServiceReenqueueDelay() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        assertThat(partialReturnOrderRepository.findAll()).hasSize(1);

        var delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );

        // Должна быть задержка, т.к. не можем апдейтить без создания сущности
        assertThat(delayO.isPresent()).isTrue();


        delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );
        // Должна быть задержка, т.к. не можем закоммить без евента update
        assertThat(delayO.isPresent()).isTrue();


        processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );
        dbQueueTestUtil.executeAllQueueItems(QueueType.PROCESS_PARTIAL_RETURN_ORDER);

        delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );
        // Должна быть задержка, т.к. не можем закоммить без евента update
        assertThat(delayO.isPresent()).isTrue();

        processingService.processPayload(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.UPDATE
                )
        );
        delayO = processingService.reenqueueDelay(
                new ProcessPartialReturnOrderPayload(
                        null,
                        partialReturnOrder.getId(),
                        userShift.getId(),
                        PartialReturnStateProcessingLrm.ActionType.COMMIT
                )
        );

        // Задержки нет, т.к. можем коммитить после апдейта
        assertThat(delayO.isEmpty()).isTrue();

    }

    @Test
    public void mapNullCisOrUit() {
        var order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .items(
                                OrderGenerateService.OrderGenerateParam.Items.builder()
                                        .isFashion(true)
                                        .build()
                        )
                        .build()
        );

        order.getItems().forEach(item -> item.getInstances().forEach(orderItemInstance -> {
            orderItemInstance.setCis(null);
            orderItemInstance.setUit(null);
        }));
        orderRepository.save(order);

        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        assertThat(partialReturnOrderRepository.findAll()).hasSize(1);
        processingLrmCommandService.create(
                PartialReturnStateProcessingLrmCommand.Create.builder()
                        .partialReturnOrderId(partialReturnOrder.getId())
                        .userShiftId(userShift.getId())
                        .build()
        );
        assertDoesNotThrow(() -> transactionTemplate.execute(
                ts -> updateLrmPartialReturnService.getReturnItems(orderRepository.findByIdOrThrow(order.getId()))
        ));
    }

    private void assertQueueLogValid(long partialReturnOrder, Set<QueueLogEvent> requiredEvents,
                                     Set<QueueLogEvent> forbiddenEvents) {
        var logEvents = queueLogRepository.findAll().stream()
                .filter(log -> log.getQueueName().equals(QueueType.PROCESS_PARTIAL_RETURN_ORDER.name()))
                .filter(log -> log.getEntityId().equals(Long.toString(partialReturnOrder)))
                .map(QueueLog::getEvent)
                .collect(Collectors.toSet());
        requiredEvents.forEach(event -> assertThat(logEvents.contains(event)).isTrue());
        forbiddenEvents.forEach(event -> assertThat(logEvents.contains(event)).isFalse());
    }

}
