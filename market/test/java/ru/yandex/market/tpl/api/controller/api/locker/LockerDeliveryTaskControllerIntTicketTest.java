package ru.yandex.market.tpl.api.controller.api.locker;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.controller.api.locker.boxbot.BoxBotConstants;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliveryErrorNotifyDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.api.model.task.locker.delivery.LockerDeliveryCancelRequestDto;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class LockerDeliveryTaskControllerIntTicketTest extends BaseApiIntTest {
    private final PickupPointRepository pickupPointRepository;

    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final ObjectMapper tplObjectMapper;

    private final Clock clock;

    private User user;
    private UserShift userShift;
    private LockerDeliveryTask task;
    private Order order;

    @BeforeEach
    void setUp() {
        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());

        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        task = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
    }

    @SneakyThrows
    @Test
    void shouldEnqueueCancelLockerTask() {
        var request = new LockerDeliveryCancelRequestDto(
                OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING, "", List.of()
        );

        mockMvc.perform(post("/api/tasks/locker-delivery/{taskId}/cancel", task.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.LOCKER_CANCEL);
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(
            value = OrderDeliveryTaskFailReasonType.class,
            names = {
                    "DIMENSIONS_EXCEEDS", "ORDER_IS_DAMAGED", "CELL_IS_DIRTY", "CELL_IS_EMPTY",
                    "ANOTHER_PLACE_IN_CELL_EXTRADITION", "ANOTHER_PLACE_IN_CELL_DELIVERY", "OTHER"}
    )
    void shouldEnqueueCancelOrderLockerTask(OrderDeliveryTaskFailReasonType failReasonType) {
        var request = new LockerDeliveryCancelRequestDto(
                failReasonType, "Comment", List.of()
        );

        mockMvc.perform(post("/api/tasks/locker-delivery/{taskId}/cancel", task.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());


        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.LOCKER_CANCEL);

        // 3, т.к. в очереди должно быть 3 евента new, execute, successful
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 3);
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(
            value = OrderDeliveryTaskFailReasonType.class,
            names = {"DIMENSIONS_EXCEEDS", "ORDER_IS_DAMAGED", "OTHER"}
    )
    void shouldEnqueueCancelOrderLockerTaskOnDeliveryError(OrderDeliveryTaskFailReasonType failReasonType) {
        var request = new MarketDeliveryErrorNotifyDto(
                0, failReasonType, "123", order.getExternalOrderId(), task.getId(), "123", List.of()
        );

        testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(BoxBotConstants.POSTAMAT_TOKEN, "1231")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());


        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 1);
        dbQueueTestUtil.executeAllQueueItems(QueueType.LOCKER_CANCEL);

        // 3, т.к. в очереди должно быть 3 евента new, execute, successful
        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 3);
    }

    @SneakyThrows
    @ParameterizedTest
    @EnumSource(
            value = OrderDeliveryTaskFailReasonType.class,
            names = {
                    "CELL_IS_DIRTY", "CELL_IS_EMPTY",
                    "ANOTHER_PLACE_IN_CELL_EXTRADITION", "ANOTHER_PLACE_IN_CELL_DELIVERY"
            }
    )
    void shouldNotEnqueueCancelOrderLockerTaskOnDeliveryError(OrderDeliveryTaskFailReasonType failReasonType) {
        var request = new MarketDeliveryErrorNotifyDto(
                0, failReasonType, "123", order.getExternalOrderId(), task.getId(), "123", List.of()
        );

        testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(BoxBotConstants.POSTAMAT_TOKEN, "1231")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());


        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 0);
    }
}
