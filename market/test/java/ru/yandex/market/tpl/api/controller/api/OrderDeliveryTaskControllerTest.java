package ru.yandex.market.tpl.api.controller.api;

import java.util.concurrent.atomic.AtomicLong;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tpl.api.BaseApiTest;
import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.common.util.exception.TplErrorCode;
import ru.yandex.market.tpl.common.util.exception.TplException;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.task.TaskErrorRepository;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;

import static ru.yandex.market.tpl.core.service.tracking.TrackingService.CONTACTLESS_DELIVERY_PREFIX;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class OrderDeliveryTaskControllerTest extends BaseApiTest {
    private final OrderDeliveryTaskController controller;

    private final TestUserHelper userHelper;
    private final OrderGenerateService orderGenerateService;
    private final TaskErrorRepository taskErrorRepository;
    private final UserPropertyService userPropertyService;
    private final UserRepository userRepository;

    private Long taskId;
    private Long routePointId;
    private User user;


    @BeforeEach
    void setUp() {
        this.user = userHelper.findOrCreateUser(824125L);
        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_DELIVERY_PHOTO_ENABLED, true);
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.PREPAID)
                .recipientNotes(CONTACTLESS_DELIVERY_PREFIX)
                .build());
        AtomicLong taskIdBox = new AtomicLong();
        UserShift userShift = userHelper.createShiftWithDeliveryTask(user, UserShiftStatus.SHIFT_OPEN, order,
                taskIdBox);
        this.taskId = taskIdBox.get();
        this.routePointId = userShift.findRoutePointIdByTaskId(taskId).orElseThrow();
        taskErrorRepository.deleteAll(taskErrorRepository.findByTaskId(taskId));
    }

    @Test
    void endOfPrepaidTaskRequiresPhotoOrComment() {
        OrderChequeRemoteDto request = new OrderChequeRemoteDto(OrderPaymentType.PREPAID, OrderChequeType.SELL);

        Assertions.assertThatThrownBy(() -> controller.registerCheque(taskId, request, user, null))
                .isInstanceOf(TplException.class)
                .matches(e -> ((TplException) e).getCode().equals(TplErrorCode.DELIVERY_PHOTO_REQUIRED.name()));
    }
}
