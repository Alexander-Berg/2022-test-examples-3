package ru.yandex.market.tpl.core.domain.routing;

import java.time.Clock;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftReassignManager;
import ru.yandex.market.tpl.core.domain.usershift.UserShiftRepository;
import ru.yandex.market.tpl.core.external.routing.vrp.client.VrpClient;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfo;
import ru.yandex.market.tpl.core.external.routing.vrp.model.TaskInfoStatus;
import ru.yandex.market.tpl.core.test.TestDataFactory;
import ru.yandex.market.tpl.core.test.TplAbstractTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@RequiredArgsConstructor
class RerouteUserShiftTest extends TplAbstractTest {

    private final VrpClient vrpClient;
    private final TestUserHelper testUserHelper;
    private final SortingCenterService sortingCenterService;
    private final TestDataFactory testDataFactory;
    private final UserShiftRepository userShiftRepository;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;
    private final OrderGenerateService orderGenerateService;
    private final UserShiftReassignManager userShiftReassignManager;
    private final RerouteUserShiftHelper rerouteUserShiftHelper;

    private User user;
    private UserShift userShift;

    @AfterEach
    void cleanUp() {
        reset(vrpClient);
    }

    @BeforeEach
    void init() {
        mockVrpClient();

        user = testUserHelper.findOrCreateUser(1L);
        Shift shift = testUserHelper.findOrCreateOpenShiftForSc(LocalDate.now(clock),
                sortingCenterService.findSortCenterForDs(239).getId());

        Order order = orderGenerateService.createOrder(
                OrderGenerateService.OrderGenerateParam.builder().externalOrderId("231432").build()
        );

        userShift = transactionTemplate.execute(t -> {
            UserShift us = userShiftRepository.findByIdOrThrow(testDataFactory.createEmptyShift(shift.getId(), user));
            userShiftReassignManager.assign(us, order);
            return us;
        });

        testUserHelper.checkinAndFinishPickup(userShift);
    }

    @Test
    void rerouteCurrentUserShiftWithoutTransaction() {
        rerouteUserShiftHelper.rerouteCurrentUserShift(user);
    }

    private void mockVrpClient() {
        TaskInfo taskInfo = new TaskInfo();
        taskInfo.setId("1");
        taskInfo.setMessage("324234");
        taskInfo.setStatus(new TaskInfoStatus());
        when(vrpClient.addMVRPTask(any(), any())).thenReturn(taskInfo);

        when(vrpClient.getTaskResult(any(), any())).thenThrow(new RuntimeException("Bad result..."));
    }


}
