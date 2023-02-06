package ru.yandex.market.tpl.core.service.sqs;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskStatus;
import ru.yandex.market.tpl.common.db.exception.TplEntityNotFoundException;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnStatus;
import ru.yandex.market.tpl.core.domain.clientreturn.repository.ClientReturnRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.service.sqs.processor.CancelClientReturnProcessor;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@RequiredArgsConstructor
public class CancelClientReturnProcessorTest extends TplAbstractTest {
    private final CancelClientReturnProcessor processor;
    private final ClientReturnRepository clientReturnRepository;
    private final ClientReturnGenerator clientReturnGenerator;
    private final UserShiftCommandDataHelper helper;
    private final TestUserHelper userHelper;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final TransactionTemplate transactionTemplate;
    private final OrderDeliveryTaskRepository orderDeliveryTaskRepository;
    private final Clock clock;

    ClientReturn clientReturn;


    @BeforeEach
    void init() {
        clientReturn = clientReturnGenerator.generateReturnFromClient();
        clientReturn.setExternalReturnId("1234567890");
        clientReturnRepository.save(clientReturn);
    }

    @Test
    @DisplayName("Проверка, что клиентский возврат и таска отменяются, если возврат находится в отменяемом статусе")
    void clientReturnIsCancelled_WhenCorrectStatus() {
        //given
        var user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var event = LrmReturnAtClientAddressCancelEventGenerateService.generateEvent(
                LrmReturnAtClientAddressCancelEventGenerateService.LrmReturnAtClientAdressCancelEventGenerateParam.builder()
                        .returnId(Long.valueOf(clientReturn.getExternalReturnId()))
                        .build()
        );

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturn.getId()))
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = userShiftRepository.findById(id).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        var taskId = transactionTemplate.execute(
                cmd -> {
                    var odt =
                            userShiftRepository.findByIdOrThrow(userShift.getId()).streamOrderDeliveryTasks().findFirst().orElseThrow();
                    return odt.getId();
                }
        );

        assertThat(orderDeliveryTaskRepository.findByIdOrThrow(taskId).getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

        //when
        processor.process(event);

        //then
        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);
        assertThat(orderDeliveryTaskRepository.findByIdOrThrow(taskId).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
    }

    @Test
    @DisplayName("Проверка, что не произойдет ошибки, при повторном ивенте на тот же возврат")
    void clientReturnDoesNotThrow_WhenPreCancelled() {
        //given
        var user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var event = LrmReturnAtClientAddressCancelEventGenerateService.generateEvent(
                LrmReturnAtClientAddressCancelEventGenerateService.LrmReturnAtClientAdressCancelEventGenerateParam.builder()
                        .returnId(Long.valueOf(clientReturn.getExternalReturnId()))
                        .build()
        );

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(helper.clientReturn("addr4", 13, clientReturn.getId()))
                .build();

        long id = commandService.createUserShift(createCommand);
        UserShift userShift = userShiftRepository.findById(id).orElseThrow();

        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
        var taskId = transactionTemplate.execute(
                cmd -> {
                    var odt =
                            userShiftRepository.findByIdOrThrow(userShift.getId()).streamOrderDeliveryTasks().findFirst().orElseThrow();
                    return odt.getId();
                }
        );

        assertThat(orderDeliveryTaskRepository.findByIdOrThrow(taskId).getStatus()).isEqualTo(OrderDeliveryTaskStatus.NOT_DELIVERED);
        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(ClientReturnStatus.RETURN_CREATED);

        //when
        processor.process(event);

        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);
        assertThat(orderDeliveryTaskRepository.findByIdOrThrow(taskId).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);

        processor.process(event);

        //then
        assertThat(clientReturnRepository.findByIdOrThrow(clientReturn.getId()).getStatus()).isEqualTo(ClientReturnStatus.CANCELLED);
        assertThat(orderDeliveryTaskRepository.findByIdOrThrow(taskId).getStatus()).isEqualTo(OrderDeliveryTaskStatus.DELIVERY_FAILED);
    }

    @Test
    @DisplayName("Проверка, что будет выброшено исключение при попытке отмены возврата, который не существует")
    void clientReturnIsNotCancelled_WhenCRIsNotFound() {

        var event = LrmReturnAtClientAddressCancelEventGenerateService.generateEvent(
                LrmReturnAtClientAddressCancelEventGenerateService.LrmReturnAtClientAdressCancelEventGenerateParam.builder()
                        .returnId(Long.valueOf(clientReturn.getExternalReturnId() + "012"))
                        .build()
        );
        //when

        assertThrows(TplEntityNotFoundException.class, () -> processor.process(event));
    }
}
