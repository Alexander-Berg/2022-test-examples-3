package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;

import ru.yandex.market.tpl.api.model.shift.ShiftStatus;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenter;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.shift.TplRoutingShiftManager;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.external.routing.api.RoutingMockType;
import ru.yandex.market.tpl.core.external.routing.api.RoutingProfileType;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

@RequiredArgsConstructor
public class TplRoutingManagerTest extends TplAbstractTest {
    @MockBean
    protected final TplRoutingManager routingManager;
    private final OrderGenerateService orderGenerateService;
    private final Clock clock;
    private final UserShiftReassignManager userShiftReassignManager;
    private final TplRoutingShiftManager routingShiftManager;
    private final TestUserHelper userHelper;

    @Test
    void testRoutingAfterReassign() {
        Mockito.doNothing().when(routingManager).queueRoutingRequestGroup(Mockito.any(), Mockito.any(),
                Mockito.anyLong(), Mockito.any(), Mockito.any());
        Mockito.doReturn(new TplRoutingManager.Result()).when(routingManager).rerouteUserShift(Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.anyBoolean(), Mockito.anyBoolean());
        User user = userHelper.findOrCreateUser(777L);
        Shift shift = userHelper.findOrCreateShiftForScWithStatus(LocalDate.now(clock), SortingCenter.DEFAULT_SC_ID,
                ShiftStatus.CREATED);
        var params = OrderGenerateService.OrderGenerateParam.builder().deliveryDate(LocalDate.now(clock));
        Order order = orderGenerateService.createOrder(params.build());
        userShiftReassignManager.reassignOrders(Set.of(order.getId()), Set.of(), Set.of(), user.getId());
        routingShiftManager.routeShiftGroupAsyncMulti(shift, RoutingMockType.REAL, RoutingProfileType.GROUP_PROFILES,
                LocalTime.now(clock));
        Mockito.verify(routingManager, Mockito.times(1)).queueRoutingRequestGroup(Mockito.any(), Mockito.any(),
                Mockito.anyLong(), Mockito.any(), Mockito.any());
        Mockito.reset(routingManager);
    }
}
