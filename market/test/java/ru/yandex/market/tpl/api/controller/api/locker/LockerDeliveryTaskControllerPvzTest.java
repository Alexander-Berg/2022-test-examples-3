package ru.yandex.market.tpl.api.controller.api.locker;

import java.time.Clock;
import java.time.LocalDate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.locker.PartnerSubType;
import ru.yandex.market.tpl.api.model.task.pickupPoint.LockerDeliveryTaskDto;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.pickup.PickupPoint;
import ru.yandex.market.tpl.core.domain.pickup.PickupPointRepository;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.LockerDeliveryTask;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.test.TestDataFactory;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})

public class LockerDeliveryTaskControllerPvzTest extends BaseApiIntTest {
    private final TestUserHelper testUserHelper;
    private final ObjectMapper tplObjectMapper;
    private final OrderGenerateService orderGenerateService;
    private final PickupPointRepository pickupPointRepository;
    private final TestDataFactory testDataFactory;
    private final Clock clock;

    private User user;
    private UserShift userShift;
    private LockerDeliveryTask task;

    @BeforeEach
    void setUp() {
        user = testUserHelper.findOrCreateUser(UID);
        userShift = testUserHelper.createEmptyShift(user, LocalDate.now(clock));

        PickupPoint pickupPointRequest = testDataFactory.createPickupPoint(PartnerSubType.LAVKA, 1L, 1L);
        pickupPointRequest.setPartnerSubType(PartnerSubType.LAVKA);
        var pickupPoint = pickupPointRepository.save(pickupPointRequest);

        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .pickupPoint(pickupPoint)
                .build());

        task = testUserHelper.addLockerDeliveryTaskToShift(user, userShift, order);
        testUserHelper.openShift(user, userShift.getId());
        testUserHelper.finishPickupAtStartOfTheDay(userShift);
    }

    @SneakyThrows
    @Test
    void shouldReturnPartnerType() {
        String content = mockMvc.perform(
                get("/api/tasks/locker-delivery/{taskId}", task.getId())
                        .header(HttpHeaders.AUTHORIZATION, AUTH_HEADER_VALUE)
        )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var task = tplObjectMapper.readValue(content, LockerDeliveryTaskDto.class);
        Assertions.assertThat(task.getLocker().getPartnerSubType()).isEqualTo(PartnerSubType.LAVKA);
    }
}
