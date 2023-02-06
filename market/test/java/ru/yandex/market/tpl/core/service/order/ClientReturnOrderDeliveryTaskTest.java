package ru.yandex.market.tpl.core.service.order;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCancelledEvent;
import ru.yandex.market.tpl.api.model.location.LocationDto;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType;
import ru.yandex.market.tpl.api.model.task.DeliveryRescheduleDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnService;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.partner.clientreturn.PartnerClientReturnService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.RoutePointRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.DeliveryReschedule;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.routing.api.RoutingRequestItemType;
import ru.yandex.market.tpl.core.service.sqs.SendClientReturnEventToSqsService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.api.model.routepoint.RoutePointStatus.FINISHED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryRescheduleReasonType.CLIENT_REFUSED_NO_TAPE;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_REFUSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.CLIENT_RETURN_WRONG_ADDRESS_BY_CLIENT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.NO_CONTACT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.NO_PASSPORT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.OTHER;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType.WRONG_ADDRESS_BY_CLIENT;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.CANCELLED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.LOST;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.PREPARED_FOR_CANCEL;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RETURN_CREATED;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.DO_NOT_CALL_ENABLED;

@RequiredArgsConstructor
public class ClientReturnOrderDeliveryTaskTest extends TplAbstractTest {
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final JdbcTemplate jdbcTemplate;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnService clientReturnService;
    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;
    private final RoutePointRepository routePointRepository;
    private final ConfigurationProviderAdapter configurationProviderAdapter;
    private final PartnerClientReturnService partnerClientReturnService;
    private final DbQueueTestUtil dbQueueTestUtil;
    @MockBean
    private final SendClientReturnEventToSqsService sendClientReturnEventToSqsService;

    private static final Pageable PAGE_REQUEST = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "date"));

    private User user;
    private UserShift userShift;


    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(1);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
    }


    @Test
    void createDeliveryTaskWithNullOrderIdAndClientReturnId() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        assertThat(tod).isNotNull();
        assertThat(tod.getClientReturnId()).isEqualTo(clientReturn.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);
    }

    @Test
    void failClientReturnDeliveryTaskWithSqlCheat() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.checkinAndFinishPickup(userShift);

        commandService.failDeliveryTask(user, getFailTaskCommand(routePointId, tod,
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH));

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(PREPARED_FOR_CANCEL);
        testUserHelper.finishFullReturnAtEnd(userShift.getId());
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(CANCELLED);
        var captor = ArgumentCaptor.forClass(TplReturnAtClientAddressCancelledEvent.class);
        Mockito.verify(sendClientReturnEventToSqsService).sendSynchronously(
                Mockito.anyString(),
                Mockito.anyLong(),
                captor.capture(),
                Mockito.anyString()
        );
        assertThat(captor.getValue().getReturnId()).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(captor.getValue().getCancelReason()).isEqualTo(TplReturnAtClientAddressCancelReason.ITEMS_QUANTITY_MISMATCH);
        assertThat(captor.getValue().getSource()).isEqualTo(TplReturnAtClientAddressModificationSource.COURIER);
    }

    @Test
    void failClientReturnWithoutDeliveryTaskFromClientReturnService() {
        var clientReturn = clientReturnGenerator.generate();

        jdbcTemplate.execute("update client_return " +
                "set status = 'READY_FOR_RECEIVED' " +
                "where id =" + clientReturn.getId());

        clientReturnService.cancelLockerClientReturn(clientReturn.getExternalReturnId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(CANCELLED);
    }

    @NotNull
    private UserShiftCommand.FailOrderDeliveryTask getFailTaskCommand(long routePointId, OrderDeliveryTask tod,
                                                                      OrderDeliveryTaskFailReasonType orderItemsQuantityMismatch) {
        return new UserShiftCommand.FailOrderDeliveryTask(userShift.getId(),
                routePointId, tod.getId(),
                new OrderDeliveryFailReason(orderItemsQuantityMismatch,
                        ""));
    }

    @Test
    void reopenClientReturnDeliveryTaskWithSqlCheat() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        testUserHelper.openShift(user, userShift.getId());
        jdbcTemplate.execute("update route_point " +
                "set status = 'IN_PROGRESS' " +
                "where id =" + routePointId);

        commandService.failDeliveryTask(user, getFailTaskCommand(routePointId, tod,
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_QUANTITY_MISMATCH));

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(PREPARED_FOR_CANCEL);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), routePointId, tod.getId(), Source.COURIER
        ));

        updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);
        var events = clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), PAGE_REQUEST);
        var revertedEvent = events.stream()
                .filter(event -> event.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_REOPENED)
                .findAny();
        assertThat(revertedEvent).isPresent();
        assertThat(revertedEvent.get().getSource()).isEqualTo(Source.COURIER);
    }

    @Test
    void reopenClientReturnDeliveryTaskAfterRescheduleWithSqlCheat() {
        Mockito.when(configurationProviderAdapter.isBooleanEnabled(DO_NOT_CALL_ENABLED)).thenReturn(true);
        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = routePoint.getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        assertThat(tod).isNotNull();
        assertThat(tod.getClientReturnId()).isEqualTo(clientReturn.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);

        testUserHelper.openShift(user, userShift.getId());

        var newFrom = LocalDateTime.of(
                clientReturn.getArriveIntervalFrom().toLocalDate().plusDays(1), LocalTime.of(14, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var newTo = LocalDateTime.of(
                clientReturn.getArriveIntervalTo().toLocalDate().plusDays(1), LocalTime.of(16, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var rescheduleDto = new DeliveryRescheduleDto(
                newFrom,
                newTo,
                CLIENT_REFUSED_NO_TAPE,
                "Нет скотча"
        );
        var reschedule = DeliveryReschedule.fromCourier(user, rescheduleDto);

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), routePointId, tod.getId(), reschedule, Instant.now(), userShift.getZoneId()
        ));

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);

        commandService.reopenDeliveryTask(user, new UserShiftCommand.ReopenOrderDeliveryTask(
                userShift.getId(), routePointId, tod.getId(), Source.COURIER
        ));

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);
        var events = clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), PAGE_REQUEST);
        var revertedEvent = events.stream()
                .filter(event -> event.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_REOPENED)
                .findAny();
        assertThat(revertedEvent).isPresent();
        assertThat(revertedEvent.get().getSource()).isEqualTo(Source.COURIER);
    }

    @Test
    @DisplayName("Успешный перенос задания на забор возврата")
    void rescheduleClientReturnWithTask() {
        var routePoint = testDataFactory.createEmptyRoutePoint(user, userShift.getId());
        long routePointId = routePoint.getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );
        assertThat(tod).isNotNull();
        assertThat(tod.getClientReturnId()).isEqualTo(clientReturn.getId());

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);

        testUserHelper.openShift(user, userShift.getId());

        var newFrom = LocalDateTime.of(
                clientReturn.getArriveIntervalFrom().toLocalDate().plusDays(1), LocalTime.of(14, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var newTo = LocalDateTime.of(
                clientReturn.getArriveIntervalTo().toLocalDate().plusDays(1), LocalTime.of(16, 0)
        ).toInstant(ZoneOffset.ofHours(3));

        var rescheduleDto = new DeliveryRescheduleDto(
                newFrom,
                newTo,
                CLIENT_REFUSED_NO_TAPE,
                "Нет скотча"
        );
        var reschedule = DeliveryReschedule.fromCourier(user, rescheduleDto);

        commandService.rescheduleDeliveryTask(user, new UserShiftCommand.RescheduleOrderDeliveryTask(
                userShift.getId(), routePointId, tod.getId(), reschedule, Instant.now(), userShift.getZoneId()
        ));

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);

        var events = clientReturnHistoryEventRepository.findByClientReturnId(clientReturn.getId(), PAGE_REQUEST);
        var rescheduledEvent = events.stream()
                .filter(event -> event.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_RESCHEDULED)
                .findAny();
        assertThat(rescheduledEvent).isPresent();
        assertThat(rescheduledEvent.get().getSource()).isEqualTo(Source.COURIER);

        routePoint = routePointRepository.findByIdOrThrow(routePointId);
        assertThat(routePoint.getStatus()).isEqualTo(FINISHED);

        tod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(tod.getStatus()).isEqualTo(DELIVERY_FAILED);

    }

    @Test
    @DisplayName("При отмене задания с определенной причиной мы переносим задание на завтра")
    void rescheduleClientReturnDeliveryTaskWithFailReason() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        testUserHelper.openShift(user, userShift.getId());
        jdbcTemplate.execute("update route_point " +
                "set status = 'IN_PROGRESS' " +
                "where id =" + routePointId);

        commandService.failDeliveryTask(user, getFailTaskCommand(routePointId, tod,
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_WRONG_ADDRESS_BY_CLIENT));

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);
    }

    @ParameterizedTest
    @MethodSource("orderToClientReturnFailReasons")
    void failClientReturnDeliveryTaskWithOrderFailReason(OrderDeliveryTaskFailReasonType orderFailReason,
                                                         OrderDeliveryTaskFailReasonType expectedClientReturnFailReason) {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.checkinAndFinishPickup(userShift);

        commandService.failDeliveryTask(user, getFailTaskCommand(routePointId, tod, orderFailReason));

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);
        assertThat(updatedTod.getFailReason().getType()).isEqualTo(expectedClientReturnFailReason);
    }

    @Test
    void shouldNotUpdateAfterLostStatus() {
        long routePointId = testDataFactory.createEmptyRoutePoint(user, userShift.getId()).getId();
        Instant deliveryTime = Instant.now(clock);
        var clientReturn = clientReturnGenerator.generateReturnFromClient();

        var tod = commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), routePointId, clientReturn.getId(), deliveryTime
                )
        );

        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.checkinAndFinishPickup(userShift);

        commandService.arriveAtRoutePoint(user, new UserShiftCommand.ArriveAtRoutePoint(
                userShift.getId(), routePointId,
                new LocationDto(BigDecimal.ZERO, BigDecimal.ZERO, "myDevice", userShift.getId())
        ));

        clientReturnService.assignBarcodeAndFinishTask(
                List.of(ClientReturn.CLIENT_RETURN_AT_ADDRESS_BARCODE_PREFIX + "123456"),
                Map.of(),
                clientReturn.getExternalReturnId(),
                user,
                tod.getId());

        var failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_WAS_LOST, "some comment", Source.OPERATOR
        );
        partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(), failReason);

        testUserHelper.finishFullReturnAtEnd(userShift.getId());
        commandService.finishUserShift(new UserShiftCommand.Finish(userShift.getId()));
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(LOST);

        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 1);
    }

    private static Stream<Arguments> orderToClientReturnFailReasons() {
        return Stream.of(
                Arguments.of(CLIENT_REFUSED, CLIENT_RETURN_CLIENT_REFUSED),
                Arguments.of(WRONG_ADDRESS_BY_CLIENT, CLIENT_RETURN_WRONG_ADDRESS_BY_CLIENT),
                Arguments.of(ORDER_IS_DAMAGED, CLIENT_RETURN_CLIENT_REFUSED),
                Arguments.of(NO_PASSPORT, CLIENT_RETURN_CLIENT_REFUSED),
                Arguments.of(NO_CONTACT, NO_CONTACT),
                Arguments.of(ORDER_ITEMS_QUANTITY_MISMATCH, ORDER_ITEMS_QUANTITY_MISMATCH)

        );
    }
}
