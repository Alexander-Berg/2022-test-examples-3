package ru.yandex.market.tpl.core.domain.usershift.calltask;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturn;
import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.clientreturn.mapper.ClientReturnOrderDtoMapper;
import ru.yandex.market.tpl.core.domain.ds.DsZoneOffsetCachingService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.usershift.OrderDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.query.usershift.mapper.CallTaskDtoMapper;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.common.util.DateTimeUtil.todayAtHour;

@RequiredArgsConstructor
public class ClientReturnCallTaskTest extends TplAbstractTest {
    private final TestUserHelper userHelper;
    private final UserShiftCommandDataHelper helper;

    private final UserShiftCommandService commandService;
    private final UserPropertyService userPropertyService;
    private final UserShiftRepository repository;
    private final CallTaskDtoMapper callTaskDtoMapper;
    private final Clock clock;
    private final ClientReturnGenerator clientReturnGenerator;
    private final TransactionTemplate transactionTemplate;
    private final DsZoneOffsetCachingService dsZoneOffsetCachingService;
    private final ClientReturnOrderDtoMapper clientReturnOrderDtoMapper;

    private User user;
    private Shift shift;

    @BeforeEach
    void init() {
        user = userHelper.findOrCreateUser(824125L, LocalDate.now(clock));
        shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));


    }

    @Test
    @DisplayName("Проверка, что DTO правильно формируется")
    void calltaskDtoCreate_WhenClientReturnPresent() {
        ClientReturn clientReturn = clientReturnGenerator.generateReturnFromClient();

        var crTask = helper.clientReturn("Addr1", 15, clientReturn.getId());
        var pickupTask = helper.taskOrderPickup(todayAtHour(8, clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(crTask)
                .routePoint(pickupTask)
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        var calltasks = transactionTemplate.execute(
                cmd -> {
                    userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
                    userPropertyService.addPropertyToUser(user,
                            UserProperties.CLIENT_RETURN_ADD_TO_MULTI_ITEM_ENABLED, true);

                    long id = commandService.createUserShift(createCommand);
                    UserShift userShift = repository.findById(id).orElseThrow();

                    commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
                    commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));
                    userHelper.finishPickupAtStartOfTheDay(userShift, true, true, false);
                    userPropertyService.addPropertyToUser(user, UserProperties.CALL_TO_RECIPIENT_ENABLED, true);
                    Hibernate.initialize(userShift.streamRoutePoints().collect(Collectors.toSet()));
                    return Map.of(userShift.getCallToRecipientTasks(),
                            userShift.streamOrderDeliveryTasks()
                                    .filter(OrderDeliveryTask::isClientReturn)
                                    .findFirst()
                                    .orElseThrow());
                }
        );

        var dto = callTaskDtoMapper.map(calltasks.keySet().stream().findAny().orElseThrow().get(0), List.of(),
                List.of(clientReturn), Map.of());

        var task = calltasks.values().stream().findAny().orElseThrow();
        var expetedCrDto = clientReturnOrderDtoMapper.mapToClientReturnOrderDto(clientReturn,
                task.getId(), task.getOrdinalNumber());
        var offset = dsZoneOffsetCachingService.getOffsetForDs(clientReturn.getDeliveryServiceId());
        assertThat(dto.getIntervalFrom()).isCloseTo(Instant.ofEpochMilli(clientReturn.getArriveIntervalFrom().toInstant(offset).toEpochMilli()), new TemporalUnitWithinOffset(1, ChronoUnit.SECONDS));
        assertThat(dto.getClientReturns()).hasSize(1);
        assertThat(dto.getClientReturns().get(0)).isEqualTo(expetedCrDto);
    }
}
