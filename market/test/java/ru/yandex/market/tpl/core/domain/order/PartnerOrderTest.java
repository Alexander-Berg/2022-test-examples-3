package ru.yandex.market.tpl.core.domain.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;
import org.apache.commons.collections.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.order.CallRequirement;
import ru.yandex.market.tpl.api.model.order.ChequeUrlDto;
import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentStatus;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDeliveryDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderDetailsDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderItemDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerOrderType;
import ru.yandex.market.tpl.api.model.order.partner.PartnerPartialReturnOrderDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderDto;
import ru.yandex.market.tpl.api.model.order.partner.PartnerReportOrderParamsDto;
import ru.yandex.market.tpl.api.model.task.ActionTimestampType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.api.model.tracking.DeliveryDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDeliveryStatus;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.common.util.DateTimeUtil;
import ru.yandex.market.tpl.common.util.datetime.LocalTimeInterval;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnCommandService;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.commands.ClientReturnCommand;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.address.AddressGenerator;
import ru.yandex.market.tpl.core.domain.order.batch.OrderBatch;
import ru.yandex.market.tpl.core.domain.order.place.OrderPlaceDto;
import ru.yandex.market.tpl.core.domain.partial_return_order.PartialReturnOrderGenerateService;
import ru.yandex.market.tpl.core.domain.partial_return_order.repository.PartialReturnOrderRepository;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.domain.usershift.location.GeoPointGenerator;
import ru.yandex.market.tpl.core.service.order.PartnerReportOrderService;
import ru.yandex.market.tpl.core.service.partnerka.PartnerkaCommand;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.core.service.usershift.ActionTimestampSaveService;
import ru.yandex.market.tpl.core.test.ClockUtil;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CARD;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.CASH;
import static ru.yandex.market.tpl.api.model.order.OrderPaymentType.PREPAID;
import static ru.yandex.market.tpl.api.model.order.OrderType.LOCKER;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.DO_NOT_CALL_DELIVERY_PREFIX;

@RequiredArgsConstructor
class PartnerOrderTest extends TplAbstractTest {

    private static final String TRACKING_ID = "tracking123";
    private static final String INCORPORATION = "shop name";
    private static final String OTHER_INCORPORATION = "other shop name";
    private static final String SENDER_YANDEX_ID = "1234";
    private static final String OTHER_SENDER_YANDEX_ID = "4321";

    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository repository;
    private final PartnerReportOrderService partnerReportOrderService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final PartialReturnOrderGenerateService partialReturnOrderGenerateService;
    private final TransactionTemplate transactionTemplate;
    private final ActionTimestampSaveService actionTimestampSaveService;
    private final PartialReturnOrderRepository partialReturnOrderRepository;
    private final TestDataFactory testDataFactory;
    private final ClientReturnGenerator clientReturnGenerator;
    private final ClientReturnCommandService clientReturnCommandService;
    private final AddressGenerator addressGenerator;
    private final OrderManager orderManager;

    private User user;
    private UserShift userShift;
    private Order orderPrepaid;
    private Order orderUnpaid;
    private Shift shift;
    private PickupPoint pickupPoint;

    private final Clock clock;
    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    private TrackingService trackingService;

    @BeforeEach
    void init() {
        TrackingDto mock = getMockCancelledWithNoContactTrackingDto(TRACKING_ID);

        when(trackingService.getTrackingDto(TRACKING_ID)).thenReturn(mock);
        ClockUtil.initFixed(clock);
        user = userHelper.findOrCreateUser(356L);

        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        orderPrepaid = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.PAID)
                .paymentType(PREPAID)
                .transferActsIds(List.of(123L))
                .build());

        orderUnpaid = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentStatus(OrderPaymentStatus.UNPAID)
                .paymentType(CASH)
                .build());

        pickupPoint = testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L);
    }

    @Test
    void mapping_PartnerOrderDeliveryDto() {
        PartnerOrderDeliveryDto orderPrepaidDto =
                partnerReportOrderService.findOrder(orderPrepaid.getExternalOrderId());
        assertEquals(orderPrepaidDto, orderPrepaid);

        PartnerOrderDeliveryDto orderUnpaidDto = partnerReportOrderService.findOrder(orderUnpaid.getExternalOrderId());
        assertEquals(orderUnpaidDto, orderUnpaid);
    }

    private void assertEquals(PartnerOrderDeliveryDto partnerOrder, Order order) {
        assertThat(partnerOrder.getOrder().getOrderId()).isEqualTo(order.getExternalOrderId());
        assertThat(partnerOrder.getOrder().isLinkedTransferActs()).isEqualTo(CollectionUtils.isNotEmpty(order.getTransferActsIds()));
    }

    @Test
    void shouldNotAllowReassignOrderInTransmittedToRecipient() {
        Order order = orderUnpaid;
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        PartnerOrderDeliveryDto dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .contains(OrderDeliveryTaskDto.ActionType.REASSIGN);

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        commandService.switchActiveUserShift(user, userShift.getId());
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        userHelper.finishPickupAtStartOfTheDay(userShift);
        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .contains(OrderDeliveryTaskDto.ActionType.REASSIGN);

        transactionTemplate.execute(ts -> {
            userShift = repository.findByIdOrThrow(userShift.getId());
            userHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
            return null;
        });
        assertThat(partnerReportOrderService.findOrder(order.getExternalOrderId()).getOrder().getOrderFlowStatus())
                .isEqualTo(OrderFlowStatus.TRANSMITTED_TO_RECIPIENT);
        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.REASSIGN);

    }

    @Test
    @Transactional
    void shouldNotAllowChangePaymentTypeForPreparedOrder() {
        Order order = orderPrepaid;
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskPrepaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        PartnerOrderDeliveryDto dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);

        transactionTemplate.execute(
                ts -> {
                    userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
                    commandService.switchActiveUserShift(user, userShift.getId());
                    commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
                    commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
                    userHelper.finishPickupAtStartOfTheDay(userShift);

                    return null;
                }
        );

        assertThat(partnerReportOrderService.findOrder(order.getExternalOrderId()).getOrder().
                getCheques())
                .isEmpty();
        userHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);
        changeOrderChequeUrl(order);
        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertCheque(dto);
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
    }

    @Test
    void shouldNotAllowChangePaymentTypeForUnpaidOrder() {
        Order order = orderUnpaid;
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        PartnerOrderDeliveryDto dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);

        transactionTemplate.execute(ts -> {
            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            userHelper.finishPickupAtStartOfTheDay(userShift);

            var rp = userShift.getCurrentRoutePoint();
            OrderDeliveryTask task = rp.streamTasks(OrderDeliveryTask.class).findFirst().orElseThrow();
            commandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                    userShift.getId(), rp.getId(), task.getId(),
                    new OrderDeliveryFailReason(OrderDeliveryTaskFailReasonType.NO_CONTACT, "Недозвон")
            ));
            return null;
        });


        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
        userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
    }

    @Test
    void shouldAllowChangePaymentTypeForPaidOrder() {
        Order order = orderUnpaid;
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));

        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        PartnerOrderDeliveryDto dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);

        transactionTemplate.execute(ts -> {
            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            userHelper.finishPickupAtStartOfTheDay(userShift);
            userHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);

            var deliveryDto = partnerReportOrderService.findOrder(order.getExternalOrderId());
            assertThat(deliveryDto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                    .contains(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
            userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);

            return null;
        });

        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .contains(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);

    }


    @Test
    void shouldNotAllowChangePaymentTypeAfterTwoDays() {
        Order order = orderUnpaid;
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));

        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        PartnerOrderDeliveryDto dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);

        transactionTemplate.execute(
                ts -> {
                    userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
                    commandService.switchActiveUserShift(user, userShift.getId());
                    commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
                    commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
                    userHelper.finishPickupAtStartOfTheDay(userShift);
                    userHelper.finishDelivery(userShift.getCurrentRoutePoint(), false);

                    var deliveryDto = partnerReportOrderService.findOrder(order.getExternalOrderId());
                    assertThat(deliveryDto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                            .contains(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
                    userHelper.finishReturnAtEndOfTheDayWithOrderWithoutCashReturning(userShift, true);

                    return null;
                }
        );

        ClockUtil.initFixed(clock, DateTimeUtil.toLocalDateTime(clock.instant().plus(2, ChronoUnit.DAYS)));
        dto = partnerReportOrderService.findOrder(order.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.CHANGE_PAYMENT_TYPE);
    }

    @Test
    void shouldReturnDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);

        List<Order> multiOrders = transactionTemplate.execute(ts -> createMultiOrders());

        var dto1 = partnerReportOrderService.findOrder(multiOrders.get(0).getExternalOrderId());
        var dto2 = partnerReportOrderService.findOrder(multiOrders.get(1).getExternalOrderId());

        assertThat(List.of(dto1, dto2)).extracting(e -> e.getOrder().getCallRequirement())
                .containsOnly(CallRequirement.DO_NOT_CALL);
    }

    @Test
    void shouldNotReturnDoNotCall() {
        when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(false);

        List<Order> multiOrders = transactionTemplate.execute(ts -> createMultiOrders());

        var dto1 = partnerReportOrderService.findOrder(multiOrders.get(0).getExternalOrderId());
        var dto2 = partnerReportOrderService.findOrder(multiOrders.get(1).getExternalOrderId());

        for (PartnerOrderDeliveryDto deliveryDto : List.of(dto1, dto2)) {
            assertThat(deliveryDto.getOrder().getCallRequirement()).isEqualTo(null);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    void testFindAllWithBatchFilters() {
        Order orderWithBatch1 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(CASH)
                        .withBatch(true)
                        .places(List.of(
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "1")).build(),
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "2")).build()
                        ))
                        .build()
        );
        Order orderWithBatch2 = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder()
                        .paymentStatus(OrderPaymentStatus.UNPAID)
                        .paymentType(CASH)
                        .withBatch(true)
                        .places(List.of(
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "3")).build(),
                                OrderPlaceDto.builder().barcode(new OrderPlaceBarcode("123", "4")).build()
                        ))
                        .build()
        );

        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, orderWithBatch1.getId()));
        builder.routePoint(helper.taskUnpaid("addr 2", 12, orderWithBatch2.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        OrderPlace orderPlaceInBatch = StreamEx.of(orderWithBatch1.getPlaces())
                .filter(place -> place.getCurrentBatch().isPresent())
                .findFirst()
                .orElseThrow();
        OrderPlace orderPlaceOutsideBatch = StreamEx.of(orderWithBatch1.getPlaces())
                .filter(place -> !place.getCurrentBatch().isPresent())
                .findFirst()
                .orElseThrow();
        String batch1Barcode = orderPlaceInBatch.getCurrentBatch().get().getBarcode();
        String batch2Barcode = StreamEx.of(orderWithBatch2.getPlaces())
                .map(OrderPlace::getCurrentBatch)
                .flatMap(Optional::stream)
                .map(OrderBatch::getBarcode)
                .findFirst()
                .orElseThrow();

        PartnerReportOrderParamsDto filterByBatchBarcode = new PartnerReportOrderParamsDto();
        filterByBatchBarcode.setBatchBarcode(batch1Barcode);
        List<PartnerReportOrderDto> dtosByBatchBarcode = partnerReportOrderService.findAll(filterByBatchBarcode);
        assertThat(dtosByBatchBarcode)
                .extracting(PartnerReportOrderDto::getOrderId)
                .containsExactly(orderWithBatch1.getExternalOrderId());

        PartnerReportOrderParamsDto filterByBatch2Barcode = new PartnerReportOrderParamsDto();
        filterByBatch2Barcode.setBatchBarcode(batch2Barcode);
        List<PartnerReportOrderDto> dtosByBatch2Barcode = partnerReportOrderService.findAll(filterByBatch2Barcode);
        assertThat(dtosByBatch2Barcode)
                .extracting(PartnerReportOrderDto::getOrderId)
                .containsExactly(orderWithBatch2.getExternalOrderId());

        PartnerReportOrderParamsDto filterByPlaceBarcode = new PartnerReportOrderParamsDto();
        filterByPlaceBarcode.setPlaceBarcode(orderPlaceOutsideBatch.getBarcode().getBarcode());
        List<PartnerReportOrderDto> dtosByPlaceBarcode = partnerReportOrderService.findAll(filterByPlaceBarcode);
        assertThat(dtosByPlaceBarcode)
                .extracting(PartnerReportOrderDto::getOrderId)
                .containsExactly(orderWithBatch1.getExternalOrderId());

        PartnerReportOrderParamsDto filterByPlaceAndBatchBarcode = new PartnerReportOrderParamsDto();
        filterByPlaceAndBatchBarcode.setPlaceBarcode(orderPlaceOutsideBatch.getBarcode().getBarcode());
        filterByPlaceAndBatchBarcode.setBatchBarcode(batch1Barcode);
        List<PartnerReportOrderDto> dtosByPlaceAndBatchBarcode =
                partnerReportOrderService.findAll(filterByPlaceAndBatchBarcode);
        assertThat(dtosByPlaceAndBatchBarcode)
                .extracting(PartnerReportOrderDto::getOrderId)
                .containsExactly(orderWithBatch1.getExternalOrderId());

        PartnerReportOrderParamsDto filterByPlaceAndDifferentBatchBarcode = new PartnerReportOrderParamsDto();
        filterByPlaceAndDifferentBatchBarcode.setPlaceBarcode(orderPlaceOutsideBatch.getBarcode().getBarcode());
        filterByPlaceAndDifferentBatchBarcode.setBatchBarcode(batch2Barcode);
        List<PartnerReportOrderDto> dtosByPlaceAndDifferentBatchBarcode =
                partnerReportOrderService.findAll(filterByPlaceAndDifferentBatchBarcode);
        assertThat(dtosByPlaceAndDifferentBatchBarcode).isEmpty();

        PartnerReportOrderParamsDto filterByHasBatch = new PartnerReportOrderParamsDto();
        filterByHasBatch.setHasBatch(true);
        List<PartnerReportOrderDto> dtosByHasBatch = partnerReportOrderService.findAll(filterByHasBatch);
        assertThat(dtosByHasBatch)
                .extracting(PartnerReportOrderDto::getOrderId)
                .containsExactlyInAnyOrder(orderWithBatch1.getExternalOrderId(), orderWithBatch2.getExternalOrderId());

    }

    @Test
    public void shouldReturnPartialReturnInfo() {
        var order =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());
        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
        partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder, 3);

        var result =
                partnerReportOrderService.findOrder(order.getExternalOrderId());

        PartnerOrderDetailsDto resultOrder = result.getOrder();
        assertPartnerOrderDetailsDtoWithPartialReturn(order, resultOrder);
        assertThat(resultOrder.isPartiallyReturned()).isTrue();
    }

    @Test
    public void shouldReturnPartialReturnInfo2() {
        var order =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());
        var partialReturnOrder =
                partialReturnOrderGenerateService.generatePartialReturnWithAllReturnItemsInstances(order);
        partialReturnOrderGenerateService.generatePartialReturnBoxes(partialReturnOrder, 3);

        var result =
                partnerReportOrderService.findOrder(order.getExternalOrderId());

        PartnerOrderDetailsDto resultOrder = result.getOrder();
        assertPartnerOrderDetailsDtoWithPartialReturn(order, resultOrder);
        assertThat(resultOrder.isPartiallyReturned()).isFalse();
    }

    private void assertPartnerOrderDetailsDtoWithPartialReturn(Order order, PartnerOrderDetailsDto resultOrder) {
        assertThat(resultOrder.isPartialReturnAllowed()).isTrue();
        assertThat(resultOrder.getPartialReturn()).isNotNull();

        var boxBarcodes =
                resultOrder.getPartialReturn().getBoxes().stream().map(PartnerPartialReturnOrderDto.PartnerPartialReturnBoxDto::getBarcode).collect(Collectors.toList());
        assertThat(boxBarcodes).isNotEmpty();
        var itemNamesExpected = order.getItems().stream().map(OrderItem::getName).collect(Collectors.toList());
        var itemCountExpected = order.getItems().stream().map(OrderItem::getCount).collect(Collectors.toList());
        var itemPicturesExpected = order.getItems().stream().map(OrderItem::getPictures).collect(Collectors.toList());
        var expectedInstances =
                order.getItems().stream().map(OrderItem::getInstances).flatMap(Collection::stream).collect(Collectors.toList());
        var expectedInstanceUits =
                expectedInstances.stream().map(OrderItemInstance::getUit).collect(Collectors.toList());
        var expectedInstanceStatuses =
                expectedInstances.stream().map(OrderItemInstance::getPurchaseStatus).collect(Collectors.toList());
        var expectedInstanceReturnReasons =
                expectedInstances.stream().map(OrderItemInstance::getReturnReason).collect(Collectors.toList());
        var expectedOrderPrices = order.getItems().stream().map(OrderItem::getPrice).collect(Collectors.toList());

        var itemNamesActual =
                resultOrder.getItems().stream().map(PartnerOrderItemDto::getName).collect(Collectors.toList());
        var itemCountActual =
                resultOrder.getItems().stream().map(PartnerOrderItemDto::getCount).collect(Collectors.toList());
        var itemPicturesActual =
                resultOrder.getItems().stream().map(PartnerOrderItemDto::getPictures).collect(Collectors.toList());
        var actualInstances =
                resultOrder.getItems().stream()
                        .map(PartnerOrderItemDto::getInstances).flatMap(Collection::stream).collect(Collectors.toList());
        var actualInstanceUits =
                actualInstances.stream().map(PartnerOrderItemDto.PartnerOrderItemInstanceDto::getUit).collect(Collectors.toList());
        var actualInstanceStatuses =
                actualInstances.stream().map(PartnerOrderItemDto.PartnerOrderItemInstanceDto::getStatus).collect(Collectors.toList());
        var actualInstanceReturnReasons =
                actualInstances.stream().map(PartnerOrderItemDto.PartnerOrderItemInstanceDto::getReturnReason).collect(Collectors.toList());
        var actualOrderPrices =
                resultOrder.getItems().stream().map(PartnerOrderItemDto::getPrice).collect(Collectors.toList());

        assertThat(itemNamesExpected).containsAll(itemNamesActual);
        assertThat(itemCountExpected).containsAll(itemCountActual);
        assertThat(itemPicturesExpected).containsAll(itemPicturesActual);
        assertThat(expectedInstanceUits).containsAll(actualInstanceUits);
        assertThat(expectedInstanceStatuses).isNotEmpty();
        assertThat(expectedInstanceReturnReasons).isNotEmpty();
        assertThat(expectedInstanceStatuses.size()).isEqualTo(actualInstanceStatuses.size());
        assertThat(expectedInstanceReturnReasons.size()).isEqualTo(actualInstanceReturnReasons.size());
        assertThat(resultOrder.isFashion()).isTrue();
        assertThat(expectedOrderPrices.size()).isEqualTo(actualOrderPrices.size());

        // сделал не через .containExactlyInAnyOrder т.к. такое сравнение некорректно обрабатывает знаки после запятой
        var samePricesForExpectedAndActual = expectedOrderPrices.stream().map(expectedPrice ->
                actualOrderPrices.stream()
                        .filter(actualPrice -> actualPrice.compareTo(expectedPrice) == 0)
                        .collect(Collectors.toList())
        ).flatMap(Collection::stream).collect(Collectors.toList());
        assertThat(samePricesForExpectedAndActual.size()).isEqualTo(expectedOrderPrices.size());
        assertThat(samePricesForExpectedAndActual.size()).isEqualTo(actualOrderPrices.size());
    }

    @Test
    public void testFindAllWithPartialReturn() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        var order =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());
        partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);

        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        var params = new PartnerReportOrderParamsDto();
        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent().get(0);
        var resultWithSort = partnerReportOrderService.findAll(params).get(0);

        assertThat(resultWithPageable).isNotNull();
        assertThat(resultWithPageable.isPartialReturnAllowed()).isTrue();
        assertThat(resultWithPageable.isFashion()).isTrue();
        assertThat(resultWithPageable.isPartiallyReturned()).isTrue();
        assertThat(resultWithPageable.getExtraditionOrderToClientDuration()).isNull();

        assertThat(resultWithSort).isNotNull();
        assertThat(resultWithSort.isPartialReturnAllowed()).isTrue();
        assertThat(resultWithSort.isFashion()).isTrue();
        assertThat(resultWithSort.isPartiallyReturned()).isTrue();
        assertThat(resultWithSort.getExtraditionOrderToClientDuration()).isNull();
    }

    @Test
    public void testFindOrderWithNotNullStartOrderExtraditionTimestamp() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);

        transactionTemplate.execute(ts -> {
            var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                    .paymentStatus(OrderPaymentStatus.UNPAID)
                    .paymentType(CARD)
                    .build()
            );
            var builder = UserShiftCommand.Create.builder()
                    .userId(user.getId())
                    .shiftId(shift.getId())
                    .routePoint(helper.taskOrderPickup(clock.instant()));
            builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
            var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();
            userHelper.checkinAndFinishPickup(userShift);
            userHelper.finishAllDeliveryTasks(userShift);
            var task = userShift.streamOrderDeliveryTasks().findFirst().orElseThrow();
            actionTimestampSaveService.saveActionTimestamp(
                    ActionTimestampType.START_ORDER_EXTRADITION_TO_CLIENT,
                    task.getId(),
                    null
            );
            assertThat(task.getFinishedAt()).isNotNull();
            assertThat(task.getStartExtraditionAt()).isNotNull();
            return null;
        });

        var params = new PartnerReportOrderParamsDto();
        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent().get(0);
        assertThat(resultWithPageable.getExtraditionOrderToClientDuration()).isNotNull();
    }

    @Test
    public void testFindAllWithoutPartialReturnButWithPossibilityToReturnAndFashion() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        var order =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(true)
                                                .build()
                                )
                                .build());

        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        var params = new PartnerReportOrderParamsDto();
        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent().get(0);
        var resultWithSort = partnerReportOrderService.findAll(params).get(0);

        assertThat(resultWithPageable).isNotNull();
        assertThat(resultWithPageable.isPartialReturnAllowed()).isTrue();
        assertThat(resultWithPageable.isFashion()).isTrue();
        assertThat(resultWithPageable.isPartiallyReturned()).isFalse();
        assertThat(resultWithPageable.getExtraditionOrderToClientDuration()).isNull();

        assertThat(resultWithSort).isNotNull();
        assertThat(resultWithSort.isPartialReturnAllowed()).isTrue();
        assertThat(resultWithSort.isFashion()).isTrue();
        assertThat(resultWithSort.isPartiallyReturned()).isFalse();
        assertThat(resultWithSort.getExtraditionOrderToClientDuration()).isNull();
    }

    @Test
    public void testFindAllWithPartialReturnAllowedFilter() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        var order =
                orderGenerateService.createOrder(
                        OrderGenerateService.OrderGenerateParam.builder()
                                .items(
                                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                                .isFashion(false)
                                                .build()
                                )
                                .build());
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskUnpaid("addr ", 12, order.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        var params = new PartnerReportOrderParamsDto();

        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        var resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isNotEmpty();
        assertThat(resultWithSort).isNotEmpty();

        params = new PartnerReportOrderParamsDto();
        params.setPartialReturnAllowed(true);
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isEmpty();
        assertThat(resultWithSort).isEmpty();

        params.setPartialReturnAllowed(false);
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isNotEmpty();
        assertThat(resultWithSort).isNotEmpty();
    }

    @Test
    void findAllOrdersWithBoxBarcode() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().isFashion(true).build())
                .build()
        );
        var partial = partialReturnOrderGenerateService.generatePartialReturnWithOnlyOneReturnItemInstance(order);
        partialReturnOrderGenerateService.generatePartialReturnBoxes(partial, 2);
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()))
                .routePoint(helper.taskUnpaid("addr ", 12, order.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE).build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        //с несуществующий штрих-кодом ничего не отдаем
        var params = new PartnerReportOrderParamsDto();
        params.setBoxBarcode("NOTHING_FIND");
        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageable.size()).isEqualTo(0);

        var boxes = transactionTemplate.execute(ts ->
                partialReturnOrderRepository.findByOrderId(order.getId())
                        .orElseThrow()
                        .getBoxes().stream().collect(Collectors.toList())
        );
        var barcode = boxes.get(0).getBarcode();

        //с существующим первым баркодом отдаем одну нужную запись
        params = new PartnerReportOrderParamsDto();
        params.setBoxBarcode(barcode);
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageable.size()).isEqualTo(1);
        assertThat(resultWithPageable.get(0).getBoxes().size()).isEqualTo(2);

        //с существующим вторым баркодом отдаем одну нужную запись
        barcode = boxes.get(1).getBarcode();
        params = new PartnerReportOrderParamsDto();
        params.setBoxBarcode(barcode);
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageable.size()).isEqualTo(1);

        //если фильтр не прислали, то отдаем все записи
        params = new PartnerReportOrderParamsDto();
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageable.size()).isEqualTo(1);
    }

    @Test
    void canFindOrderByYandexSenderIdAndIncorporation() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);
        //заказ с определенным отправителем
        var order =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().orderSender(
                                OrderGenerateService.OrderGenerateParam.OrderSender.builder()
                                        .yandexId(SENDER_YANDEX_ID)
                                        .incorporation(INCORPORATION)
                                        .ogrn("1234").legalForm("form").url("url.com")
                                        .build()
                        ).build()
                );
        var otherOrder =
                orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder().orderSender(
                                OrderGenerateService.OrderGenerateParam.OrderSender.builder()
                                        .yandexId(OTHER_SENDER_YANDEX_ID)
                                        .incorporation(OTHER_INCORPORATION)
                                        .ogrn("12345").legalForm("form2").url("url2.com")
                                        .build()
                        ).build()
                );
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskUnpaid("addr ", 12, order.getId()))
                .routePoint(helper.taskUnpaid("addr ", 12, otherOrder.getId()))
                .mergeStrategy(SimpleStrategies.NO_MERGE).build();

        commandService.createUserShift(createCommand);

        //поиск заказа по айдишнику отправителя
        var paramsYandexId = new PartnerReportOrderParamsDto();
        paramsYandexId.setSenderYandexId(SENDER_YANDEX_ID);
        var resultWithPageableYandexId =
                partnerReportOrderService.findAll(paramsYandexId, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPageableYandexId.size()).isEqualTo(1);
        PartnerReportOrderDto partnerReportOrderDtoSenderId = resultWithPageableYandexId.get(0);

        assertThat(partnerReportOrderDtoSenderId.getOrderId()).isEqualTo(order.getExternalOrderId());
        assertThat(partnerReportOrderDtoSenderId.getSender().getYandexId()).isEqualTo(SENDER_YANDEX_ID);
        assertThat(partnerReportOrderDtoSenderId.getSender().getIncorporation()).isEqualTo(INCORPORATION);

        //поиск заказа по названию магазина
        var paramsIncorporation = new PartnerReportOrderParamsDto();
        paramsIncorporation.setSenderIncorporation(INCORPORATION);
        var resultWithPAgeableIncorporation =
                partnerReportOrderService.findAll(paramsYandexId, PageRequest.of(0, 2)).getContent();
        assertThat(resultWithPAgeableIncorporation.size()).isEqualTo(1);
        PartnerReportOrderDto partnerReportOrderDtoIncorporation = resultWithPAgeableIncorporation.get(0);

        assertThat(partnerReportOrderDtoIncorporation.getOrderId()).isEqualTo(order.getExternalOrderId());
        assertThat(partnerReportOrderDtoIncorporation.getSender().getYandexId()).isEqualTo(SENDER_YANDEX_ID);
        assertThat(partnerReportOrderDtoIncorporation.getSender().getIncorporation()).isEqualTo(INCORPORATION);
    }

    @Test
    public void testFindAllClientReturns() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(
                ConfigurationProperties.READ_ORDERS_FROM_MATERIALIZED_VIEW_ENABLED
        )).thenReturn(true);

        userShift = userHelper.createEmptyShift(user, LocalDate.now(clock));

        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var barcode = "barcode";
        clientReturnCommandService.attachBarcode(ClientReturnCommand.AttachBarcode.builder()
                .barcode(barcode)
                .clientReturnId(clientReturn.getId())
                .source(Source.COURIER)
                .build());

        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        var params = new PartnerReportOrderParamsDto();

        var resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        var resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isNotEmpty();
        assertThat(resultWithSort).isNotEmpty();

        params = new PartnerReportOrderParamsDto();
        params.setOrderTypes(Set.of(PartnerOrderType.CLIENT_RETURN));
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isEmpty();
        assertThat(resultWithSort).isEmpty();

        params.setOrderTypes(Set.of(PartnerOrderType.CLIENT_RETURN_AT_CLIENT_ADDRESS));
        resultWithPageable = partnerReportOrderService.findAll(params, PageRequest.of(0, 2)).getContent();
        resultWithSort = partnerReportOrderService.findAll(params);
        assertThat(resultWithPageable).isNotEmpty();
        assertThat(resultWithSort).isNotEmpty();
        assertThat(resultWithPageable.get(0).getTotalPrice()).isNotNull();
        assertThat(resultWithPageable.get(0).getBoxes()).hasSize(1);
        assertThat(resultWithPageable.get(0).getBoxes().get(0)).isEqualTo(barcode);
    }

    @Test
    void shouldNotAllowReopenForRedelivery() {
        // given
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));
        builder.routePoint(helper.taskPrepaid("addr ", 12, orderPrepaid.getId()));
        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();
        transactionTemplate.executeWithoutResult(ts -> {
            userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

            commandService.switchActiveUserShift(user, userShift.getId());
            commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
            commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
            userHelper.finishPickupAtStartOfTheDay(userShift);
        });

        // when
        orderManager.updateDsOrderData(buildUpdateOrderCommand(orderPrepaid));

        // then
        var dto = partnerReportOrderService.findOrder(orderPrepaid.getExternalOrderId());
        assertThat(dto.getActions().stream().map(OrderDeliveryTaskDto.Action::getType))
                .doesNotContain(OrderDeliveryTaskDto.ActionType.REOPEN);
    }

    private void assertCheque(PartnerOrderDeliveryDto dto) {
        assertThat(dto.getOrder().
                getCheques().stream().map(ChequeUrlDto::getUrl).collect(Collectors.toList()))
                .isNotEmpty().containsOnly("fakeUrl");
    }

    private void changeOrderChequeUrl(Order order) {
        order.getCheques().get(0).setUrl("fakeUrl");
    }

    private TrackingDto getMockCancelledWithNoContactTrackingDto(String id) {
        DeliveryDto delivery = new DeliveryDto();
        delivery.setStatus(TrackingDeliveryStatus.CANCELLED_AFTER_NO_CONTACT);

        TrackingDto result = new TrackingDto();
        result.setId(id);
        result.setDelivery(delivery);

        return result;
    }

    private List<Order> createMultiOrders() {
        var builder = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.taskOrderPickup(clock.instant()));

        var createCommand = builder.mergeStrategy(SimpleStrategies.NO_MERGE).build();

        userShift = repository.findById(commandService.createUserShift(createCommand)).orElseThrow();

        AddressGenerator.AddressGenerateParam addressGenerateParam = AddressGenerator.AddressGenerateParam.builder()
                .geoPoint(GeoPointGenerator.generateLonLat())
                .street("Колотушкина")
                .house("1")
                .build();

        Order multiOrder1 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
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
                .recipientNotes("Консьержка.")
                .build());

        Order multiOrder2 = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .externalOrderId("4321231")
                .deliveryDate(LocalDate.now(clock))
                .deliveryServiceId(239L)
                .deliveryInterval(LocalTimeInterval.valueOf("10:00-13:00"))
                .addressGenerateParam(addressGenerateParam)
                .items(OrderGenerateService.OrderGenerateParam.Items.builder()
                        .itemsCount(2)
                        .itemsPrice(BigDecimal.valueOf(3000.0))
                        .build()
                )
                .flowStatus(OrderFlowStatus.SORTING_CENTER_PREPARED)
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes("Консьержка. " + DO_NOT_CALL_DELIVERY_PREFIX)
                .build());

        userShiftReassignManager.assign(userShift, multiOrder1);
        userShiftReassignManager.assign(userShift, multiOrder2);

        userHelper.checkinAndFinishPickup(userShift);

        return List.of(multiOrder1, multiOrder2);
    }

    private PartnerkaCommand.UpdateDsOrderData buildUpdateOrderCommand(Order order) {
        return PartnerkaCommand.UpdateDsOrderData.builder()
                .newAddress(addressGenerator.generate(AddressGenerator.AddressGenerateParam.builder().build()))
                .oldAddress(order.getDelivery().getDeliveryAddress())
                .orderType(LOCKER)
                .orderPaymentStatus(OrderPaymentStatus.PAID)
                .orderPaymentType(OrderPaymentType.PREPAID)
                .existingOrder(order)
                .intervalFrom(LocalTime.from(order.getDelivery().getDeliveryIntervalFrom().atOffset(ZoneOffset.UTC)))
                .intervalTo(LocalTime.from(order.getDelivery().getDeliveryIntervalTo().atOffset(ZoneOffset.UTC)))
                .deliveryDate(order.getDelivery().getDeliveryDateAtDefaultTimeZone().plusDays(3))
                .pickupPoint(pickupPoint)
                .build();
    }

}
