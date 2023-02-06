package ru.yandex.market.tpl.api.controller.api.locker;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
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
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskStatus;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTaskRepository;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
class LockerDeliveryTaskControllerIntTest extends BaseApiIntTest {
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final OrderGenerateService orderGenerateService;
    private final LockerDeliveryTaskRepository lockerDeliveryTaskRepository;
    private final PickupPointRepository pickupPointRepository;
    private final ConfigurationServiceAdapter configurationService;
    private final TestDataFactory testDataFactory;
    private final Clock clock;
    private final LockerDeliveryTaskControllerRequests lockerDeliveryTaskControllerRequests;

    private User user;
    private UserShift userShift;
    private LockerDeliveryTask task;
    private Order order;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        var pickupPoint = pickupPointRepository.save(
                testDataFactory.createPickupPoint(PartnerSubType.PVZ, 1L, 1L));
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());

        task = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
    }

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryTaskFailReasonType.class, names = {
            "OTHER",
            "LOCKER_NOT_WORKING",
            "COULD_NOT_CONNECT_TO_LOCKER"
    })
    void shouldRequireCommentForFail(OrderDeliveryTaskFailReasonType type) throws Exception {
        configurationService.mergeValue(ConfigurationProperties.LOCKER_DELIVERY_COMMENT_AND_PHOTO_URLS_VALIDATION_ENABLED, true);

        var request = new LockerDeliveryCancelRequestDto(
                type,
                null,
                List.of("somePhotos")
        );

        lockerDeliveryTaskControllerRequests.performCancelRequest(task.getId(), request)
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = OrderDeliveryTaskFailReasonType.class, names = {
            "PVZ_CLOSED",
            "ORDER_IS_DAMAGED",
            "COULD_NOT_GET_TO_LOCKER",
            "COULD_NOT_CONNECT_TO_LOCKER",
            "LOCKER_NOT_WORKING",
            "DIMENSIONS_EXCEEDS"
    })
    void shouldRequirePhotosForFail(OrderDeliveryTaskFailReasonType type) throws Exception {
        configurationService.mergeValue(ConfigurationProperties.LOCKER_DELIVERY_COMMENT_AND_PHOTO_URLS_VALIDATION_ENABLED, true);

        var request = new LockerDeliveryCancelRequestDto(
                type,
                "someComment",
                null
        );

        lockerDeliveryTaskControllerRequests.performCancelRequest(task.getId(), request)
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldSavePhotos() throws Exception {
        var request = new LockerDeliveryCancelRequestDto(
                OrderDeliveryTaskFailReasonType.LOCKER_NOT_WORKING,
                "comment",
                List.of("photoUrl1", "photoUrl2")
        );

        lockerDeliveryTaskControllerRequests.performCancelRequest(task.getId(), request)
                .andExpect(status().isOk());

        task = lockerDeliveryTaskRepository.findByIdOrThrow(task.getId());
        Assertions.assertThat(task.getStatus())
                .isEqualTo(LockerDeliveryTaskStatus.CANCELLED);
        Assertions.assertThat(task.getFailReason())
                .isNotNull();
        Assertions.assertThat(task.getFailReason().getComment())
                .isEqualTo("comment");
        Assertions.assertThat(task.getFailReason().getPhotoUrls())
                .containsExactly("photoUrl1", "photoUrl2");
    }

    @SneakyThrows
    @Test
    void shouldShowCancelledOrderInTask() {
        var request = new MarketDeliveryErrorNotifyDto(
                0, OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                "123", order.getExternalOrderId(), task.getId(), "123", List.of()
        );

        testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(BoxBotConstants.POSTAMAT_TOKEN, "1231")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());


        var response = mockMvc.perform(get("/api/tasks/locker-delivery/{task-id}", task.getId())
                .param("shouldShowAllOrders", "true")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response).isNotNull();
        var result = tplObjectMapper.readValue(response, LockerDeliveryTaskDto.class);

        Assertions.assertThat(result.getOrders()).isNotEmpty();
        Assertions.assertThat(result.getOrders().get(0).getExternalOrderId()).isEqualTo(order.getExternalOrderId());

    }

    @SneakyThrows
    @Test
    void shouldHideCancelledOrderInTaskWithRequestParam() {
        var request = new MarketDeliveryErrorNotifyDto(
                0, OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                "123", order.getExternalOrderId(), task.getId(), "123", List.of()
        );

        testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(BoxBotConstants.POSTAMAT_TOKEN, "1231")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());

        var response = mockMvc.perform(get("/api/tasks/locker-delivery/{task-id}", task.getId())
                .param("shouldShowAllOrders", "false")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response).isNotNull();
        var result = tplObjectMapper.readValue(response, LockerDeliveryTaskDto.class);

        Assertions.assertThat(result.getOrders()).isEmpty();

    }

    @SneakyThrows
    @Test
    void shouldHideCancelledOrderInTaskWithoutRequestParam() {
        var request = new MarketDeliveryErrorNotifyDto(
                0, OrderDeliveryTaskFailReasonType.ORDER_IS_DAMAGED,
                "123", order.getExternalOrderId(), task.getId(), "123", List.of()
        );

        testUserHelper.arriveAtRoutePoint(userShift, task.getRoutePoint().getId());

        mockMvc.perform(put("/api/locker/boxbot/delivery/error")
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(BoxBotConstants.POSTAMAT_TOKEN, "1231")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(tplObjectMapper.writeValueAsString(request))
        ).andExpect(status().isOk());

        var response = mockMvc.perform(get("/api/tasks/locker-delivery/{task-id}", task.getId())
                .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        ).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Assertions.assertThat(response).isNotNull();
        var result = tplObjectMapper.readValue(response, LockerDeliveryTaskDto.class);

        Assertions.assertThat(result.getOrders()).isEmpty();

    }
}
