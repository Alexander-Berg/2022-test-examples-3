package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandDataHelper;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftCommandService;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.domain.usershift.commands.UserShiftCommand;
import ru.yandex.market.tpl.core.external.routing.vrp.RoutingApiDataHelper;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author ungomma
 */
@RequiredArgsConstructor(onConstructor = @__(@Autowired))

@CoreTest
class ReRoutingTest {

    private static final String PROCESSING_ID = "PROCESSING_ID";
    private final RoutingApiDataHelper helper = new RoutingApiDataHelper();

    private final Clock clock;

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftCommandDataHelper usHelper;

    private final UserShiftCommandService commandService;

    private final UserShiftRepository userShiftRepository;

    private final TransactionTemplate tt;

    private final TplRoutingManager routingManager;
    private  final VrpClient vrpClient;
    @MockBean
    private CollectRoutingResultManager collectRoutingResultManager;

    private UserShift userShift;

    private long orderId;
    private long orderId2;
    private long orderId3;

    @BeforeEach
    void init() {
        User user = tt.execute(t -> userHelper.findOrCreateUser(777L));
        var shift = userHelper.findOrCreateOpenShift(LocalDate.now(clock));

        var params = OrderGenerateService.OrderGenerateParam.builder().deliveryDate(LocalDate.now(clock));
        Order order = orderGenerateService.createOrder(params.build());
        Order order2 = orderGenerateService.createOrder(params.build());
        Order order3 = orderGenerateService.createOrder(params.build());
        orderId = order.getId();
        orderId2 = order2.getId();
        orderId3 = order3.getId();

        var createCommand = UserShiftCommand.Create.builder()
                .userId(user.getId())
                .shiftId(shift.getId())
                .routePoint(usHelper.taskUnpaid("addr1", 12, order.getId()))
                .routePoint(usHelper.taskPrepaid("addr3", 14, order2.getId()))
                .routePoint(usHelper.taskPrepaid("addrPaid", 13, order3.getId()))
                .build();

        userShift = userShiftRepository.findById(commandService.createUserShift(createCommand)).orElseThrow();
        commandService.checkin(user, new UserShiftCommand.CheckIn(userShift.getId()));
        commandService.startShift(user, new UserShiftCommand.Start(userShift.getId()));

        routingManager.setPool(MoreExecutors.newDirectExecutorService());
    }

    @BeforeEach
    void resetMocks() {
        reset(vrpClient);
        reset(collectRoutingResultManager);
    }

    @Test
    void shouldStillAcceptOrderIfRoutingFails() {
        Instant originalRpTime = tt.execute(t -> {
            UserShift us = userShiftRepository.findByIdOrThrow(userShift.getId());

            assertThat(us.getStatus()).isEqualTo(UserShiftStatus.ON_TASK);
            assertThat(us.streamDeliveryRoutePoints()
                    .filter(rp -> !rp.getStatus().isTerminal()).toList()
            ).hasSize(3);

            return us.streamOrderDeliveryTasks()
                    .findFirst(dt -> dt.getOrderId() == orderId)
                    .orElseThrow().getRoutePoint().getExpectedDateTime();
        });

        when(vrpClient.addMVRPTask(any(), any()))
                .thenThrow(new RuntimeException("ROUTING ERROR"));

        userHelper.finishPickupAtStartOfTheDay(userShift, List.of(orderId), List.of(orderId2));

        tt.execute(t -> {
            UserShift us = userShiftRepository.findByIdOrThrow(userShift.getId());

            // время не изменилось
            assertThat(us.getCurrentRoutePoint()).isNotNull();
            assertThat(us.getCurrentRoutePoint().getExpectedDateTime())
                    .isEqualTo(originalRpTime);

            // но задачка по непринятому заказу закрылась
            assertThat(us.streamDeliveryRoutePoints()
                    .filter(rp -> !rp.getStatus().isTerminal()).toList()
            ).hasSize(1);
            return null;
        });
    }

}
