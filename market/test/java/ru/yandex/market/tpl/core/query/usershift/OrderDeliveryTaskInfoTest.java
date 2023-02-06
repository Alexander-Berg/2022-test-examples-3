package ru.yandex.market.tpl.core.query.usershift;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.core.domain.clientreturn.ClientReturnGenerator;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.routing.merge.SimpleStrategies;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;

@RequiredArgsConstructor
public class OrderDeliveryTaskInfoTest extends TplAbstractTest {

    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandService commandService;
    private final UserShiftRepository userShiftRepository;
    private final UserShiftQueryService queryService;
    private final ClientReturnGenerator clientReturnGenerator;
    private final Clock clock;

    private User user;
    private UserShift userShift;

    @BeforeEach
    void init() {
        user = testUserHelper.findOrCreateUser(1L);
        var shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .active(true)
                .shiftId(shift.getId())
                .mergeStrategy(SimpleStrategies.BY_DATE_INTERVAL_MERGE)
                .build();

        long userShiftId = commandService.createUserShift(createCommand);
        userShift = userShiftRepository.findByIdOrThrow(userShiftId);
    }

    @Test
    void getCorrectTotalPrice() {
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .items(OrderGenerateService.OrderGenerateParam.Items.builder().itemsPrice(BigDecimal.TEN).itemsItemCount(2).build())
                .build());

        var deliveryTask = testUserHelper.addDeliveryTaskToShift(user, userShift, order);
        Instant deliveryTime = order.getDelivery().getDeliveryIntervalFrom();
        var clientReturn = clientReturnGenerator.generateReturnFromClient();
        commandService.addClientReturnDeliveryTaskToRoutePoint(user,
                new UserShiftCommand.ManualAddClientReturnDeliveryTask(
                        userShift.getId(), deliveryTask.getRoutePoint().getId(), clientReturn.getId(),
                        deliveryTime
                )
        );

        var deliveryTaskInfo = queryService.getDeliveryTaskInfo(user, deliveryTask.getRoutePoint().getId(),
                deliveryTask.getId());

        assertThat(deliveryTaskInfo.getOrder().getTotalPrice().compareTo(BigDecimal.valueOf(40))).isEqualTo(0);

    }
}
