package ru.yandex.market.tpl.core.domain.partner.clientreturn;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressCancelReason;
import ru.yandex.market.logistics.les.dto.TplReturnAtClientAddressModificationSource;
import ru.yandex.market.logistics.les.tpl.TplReturnAtClientAddressCancelledEvent;
import ru.yandex.market.tpl.api.model.order.clientreturn.ClientReturnHistoryEventType;
import ru.yandex.market.tpl.api.model.order.clientreturn.PartnerClientReturnDetailsDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryFailReasonDto;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.Source;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.web.exception.TplInvalidActionException;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnQueryService;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnHistoryEventRepository;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.sqs.SendClientReturnEventToSqsService;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus.DELIVERY_FAILED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.CANCELLED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.LOST;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.PREPARED_FOR_CANCEL;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RECEIVED;
import static ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus.RETURN_CREATED;

@RequiredArgsConstructor
class PartnerClientReturnServiceTest extends TplAbstractTest {
    private final TestDataFactory testDataFactory;
    private final TestUserHelper testUserHelper;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;
    private final UserShiftCommandService commandService;
    private final JdbcTemplate jdbcTemplate;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final ClientReturnRepository clientReturnRepository;
    private final UserShiftCommandService userShiftCommandService;
    private final ClientReturnQueryService clientReturnQueryService;
    private final PartnerClientReturnService partnerClientReturnService;
    private final ClientReturnHistoryEventRepository clientReturnHistoryEventRepository;
    private final DbQueueTestUtil dbQueueTestUtil;
    @MockBean
    private final SendClientReturnEventToSqsService sendClientReturnEventToSqsService;

    private User user;
    private UserShift userShift;


    @BeforeEach
    public void init() {
        user = testUserHelper.findOrCreateUser(1);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));
    }

    @Test
    void failClientReturnDeliveryTaskWithSqlCheatFromClientReturnService() {
        OrderDeliveryFailReasonDto failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.ORDER_ITEMS_MISMATCH, "some comment", Source.OPERATOR
        );
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

        partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(), failReason);

        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);
        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(PREPARED_FOR_CANCEL);
    }

    @Test
    @DisplayName("Проверка, что при отмене клиентского возврата, в лес отправляется событие об отмене")
    void sendCancelledCourierClientReturnToLms() {
        OrderDeliveryFailReasonDto failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED, "some comment", Source.OPERATOR
        );
        //подготавливаем возврат на точке
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

        //проваливаем возврат, чтобы не было активных тасок и возвратв  статусе PREPARED_FOR_CANCEL
        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), tod.getRoutePoint().getId(), tod.getId(), new OrderDeliveryFailReason(failReason,
                Source.OPERATOR)
        ));
        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);

        //делаем отмену
        partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(), failReason);

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(CANCELLED);

        var captor = ArgumentCaptor.forClass(TplReturnAtClientAddressCancelledEvent.class);
        Mockito.verify(sendClientReturnEventToSqsService).sendSynchronously(
                Mockito.anyString(),
                Mockito.anyLong(),
                captor.capture(),
                Mockito.anyString()
        );
        //запись уходит в лес
        assertThat(captor.getValue().getReturnId()).isEqualTo(clientReturn.getExternalReturnId());
        assertThat(captor.getValue().getCancelReason()).isEqualTo(TplReturnAtClientAddressCancelReason.CLIENT_REFUSED);
        assertThat(captor.getValue().getSource()).isEqualTo(TplReturnAtClientAddressModificationSource.SYSTEM);
    }

    @Test
    void verifyDtoContent_WhenCourierClientReturnCancelled() {
        OrderDeliveryFailReasonDto failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_CLIENT_REFUSED, "some comment", Source.OPERATOR
        );
        //подготавливаем возврат на точке
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

        //проваливаем возврат, чтобы не было активных тасок и возвратв  статусе PREPARED_FOR_CANCEL
        userShiftCommandService.failDeliveryTask(user, new UserShiftCommand.FailOrderDeliveryTask(
                userShift.getId(), tod.getRoutePoint().getId(), tod.getId(), new OrderDeliveryFailReason(failReason,
                Source.OPERATOR)
        ));
        var updatedTod = orderDeliveryTaskRepository.findByIdOrThrow(tod.getId());
        assertThat(updatedTod.getStatus()).isEqualTo(DELIVERY_FAILED);

        var partnerClientReturnPreCancelDto =
                clientReturnQueryService.getClientReturnInfo(clientReturn.getExternalReturnId());
        assertThat(partnerClientReturnPreCancelDto.getDetails().getStatus()).isEqualTo(PREPARED_FOR_CANCEL.getDescription());

        //делаем отмену
        var partnerClientReturnDto = partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(),
                failReason);

        //Убеждаемся, что дто принадлежит все тому же возврату и что статус изменился
        PartnerClientReturnDetailsDto clientReturnDtoDetails = partnerClientReturnDto.getDetails();
        assertThat(clientReturnDtoDetails.getStatus()).isEqualTo(CANCELLED.getDescription());
        assertThat(clientReturnDtoDetails.getExternalReturnId()).isEqualTo(clientReturn.getExternalReturnId());
    }

    @Test
    void setClientReturnLost() {
        var failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_WAS_LOST, "some comment", Source.OPERATOR
        );
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        clientReturn.setStatus(RECEIVED);
        clientReturn = clientReturnRepository.save(clientReturn);

        partnerClientReturnService.cancelClientReturn(clientReturn.getExternalReturnId(), failReason);

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(LOST);

        var history = clientReturnHistoryEventRepository.findByClientReturnId(
                        clientReturn.getId(), Pageable.unpaged()
                )
                .stream()
                .filter(he -> he.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_LOST)
                .findFirst();
        assertThat(history.isPresent()).isEqualTo(true);
        dbQueueTestUtil.assertQueueHasSize(QueueType.CLIENT_RETURN_CREATE_OW_TICKET, 1);
    }

    @Test
    void setClientReturnLostShouldThrowAnException() {
        var failReason = new OrderDeliveryFailReasonDto(
                OrderDeliveryTaskFailReasonType.CLIENT_RETURN_WAS_LOST, "some comment", Source.OPERATOR
        );
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        var extId = clientReturn.getExternalReturnId();

        assertThrows(TplInvalidActionException.class, () -> partnerClientReturnService.cancelClientReturn(extId,
                failReason));

        clientReturn = clientReturnRepository.findByIdOrThrow(clientReturn.getId());
        assertThat(clientReturn.getStatus()).isEqualTo(RETURN_CREATED);

        var history = clientReturnHistoryEventRepository.findByClientReturnId(
                        clientReturn.getId(), Pageable.unpaged()
                )
                .stream()
                .filter(he -> he.getType() == ClientReturnHistoryEventType.CLIENT_RETURN_LOST)
                .findFirst();
        assertThat(history.isEmpty()).isEqualTo(true);
    }
}
