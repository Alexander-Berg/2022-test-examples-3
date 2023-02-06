package ru.yandex.market.tpl.api.controller.api.locker.boxbot;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.locker.boxbot.request.MarketDeliveryErrorNotifyDto;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskFailReasonType;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.Shift;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.domain.usershift.commands.OrderDeliveryFailReason;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class DeliveryControllerIntTest extends BaseApiIntTest {

    private final LockerDeliveryTaskRepository lockerDeliveryTaskRepository;
    private final PickupPointRepository pickupPointRepository;

    private final DbQueueTestUtil dbQueueTestUtil;
    private final OrderGenerateService orderGenerateService;
    private final TestUserHelper testUserHelper;
    private final TestDataFactory testDataFactory;
    private final Clock clock;
    private final ObjectMapper tplObjectMapper;

    private Order order;
    private LockerDeliveryTask lockerDeliveryTask;

    @BeforeEach
    void setUp() {
        Shift shift = testUserHelper.findOrCreateOpenShift(LocalDate.now(clock));
        User user = testUserHelper.findOrCreateUser(UID);

        UserShift userShift = testUserHelper.createEmptyShift(user, shift);

        PickupPoint pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.LOCKER, 1L, 1L)
        );

        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());
        lockerDeliveryTask = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
        testUserHelper.arriveAtRoutePoint(userShift, lockerDeliveryTask.getRoutePoint().getId());
    }

    @SneakyThrows
    @Test
    void shouldSavePhotoUrls() {
        var errorDto = new MarketDeliveryErrorNotifyDto(
                -1,
                OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                "",
                order.getExternalOrderId(),
                lockerDeliveryTask.getId(),
                "Я СЛОМАЛЬ",
                List.of("http://example.org")
        );

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(BoxBotConstants.POSTAMAT_TOKEN, "rofl")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .content(tplObjectMapper.writeValueAsString(errorDto))
                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        lockerDeliveryTask = lockerDeliveryTaskRepository.findByIdOrThrow(lockerDeliveryTask.getId());

        OrderDeliveryFailReason failReason = lockerDeliveryTask.getSubtasks().get(0).getFailReason();
        Assertions.assertThat(failReason).isNotNull();
        Assertions.assertThat(failReason.getPhotoUrls()).hasSize(1);
        Assertions.assertThat(failReason.getPhotoUrls().get(0)).isEqualTo("http://example.org");
        Assertions.assertThat(failReason.getComment()).isEqualTo("Я СЛОМАЛЬ");
        Assertions.assertThat(failReason.getType()).isEqualTo(OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED);

        dbQueueTestUtil.assertQueueHasSize(QueueType.LOCKER_CANCEL, 1);


    }
}
