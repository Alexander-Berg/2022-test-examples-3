package ru.yandex.market.tpl.internal.controller.internal;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.tpl.api.model.shift.UserShiftStatus;
import ru.yandex.market.tpl.api.model.tracking.DeliveryDto;
import ru.yandex.market.tpl.api.model.tracking.OrderDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingDto;
import ru.yandex.market.tpl.api.model.tracking.TrackingIntervalDisplayMode;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.usershift.UserShift;
import ru.yandex.market.tpl.core.service.tracking.TrackingService;
import ru.yandex.market.tpl.internal.controller.BaseTplIntWebTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TrackingControllerIntegrationTest extends BaseTplIntWebTest {

    private final MockMvc mockMvc;
    private final TestUserHelper testUserHelper;
    private final OrderGenerateService orderGenerateService;
    private final TrackingService trackingService;
    private final ObjectMapper tplObjectMapper;

    private String trackingId;
    private Order order;
    private User user;

    @BeforeEach
    void setUpThis() {
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .deliveryPrice(new BigDecimal("56.7"))
                .items(
                        OrderGenerateService.OrderGenerateParam.Items.builder()
                                .itemsPrice(new BigDecimal("34.5"))
                                .build()
                )
                .build());

        user = testUserHelper.findOrCreateUser(UID);

        UserShift shiftWithDeliveryTask = testUserHelper.createShiftWithDeliveryTask(
                user,
                UserShiftStatus.SHIFT_OPEN,
                order
        );
        testUserHelper.finishPickupAtStartOfTheDay(shiftWithDeliveryTask);

        trackingId = trackingService.getTrackingLinkByOrder(order.getExternalOrderId()).orElseThrow();

    }

    @Test
    void shouldReturnOrdersField() throws Exception {
        MvcResult result = mockMvc.perform(get("/internal/tracking/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andReturn();

        TrackingDto trackingDto = tplObjectMapper.readValue(result.getResponse().getContentAsString(),
                TrackingDto.class);

        Assertions.assertThat(trackingDto.getOrder()).isNotNull();
        Assertions.assertThat(trackingDto.getOrders()).hasSize(1);

        OrderDto orderDto = trackingDto.getOrders().get(0);
        Assertions.assertThat(orderDto.getDeliveryPrice()).isEqualByComparingTo(new BigDecimal("56.7"));

        Assertions.assertThat(orderDto.getItems()).hasSize(2);
        Assertions.assertThat(orderDto.getItems().get(0).getPrice()).isEqualByComparingTo(new BigDecimal("34.5"));
        Assertions.assertThat(orderDto.getItems().get(1).getPrice()).isEqualByComparingTo(new BigDecimal("34.5"));
    }

    @Test
    void deliveryBySellerOrder() throws Exception {
        User dbsUser = testUserHelper.createOrFindDbsUser();

        UserShift shiftWithDeliveryTask = testUserHelper.createShiftWithDeliveryTask(
                dbsUser,
                UserShiftStatus.SHIFT_OPEN,
                order
        );
        testUserHelper.finishPickupAtStartOfTheDay(shiftWithDeliveryTask);

        MvcResult result = mockMvc.perform(get("/internal/tracking/{trackingId}", trackingId))
                .andExpect(status().isOk())
                .andReturn();

        TrackingDto trackingDto = tplObjectMapper.readValue(result.getResponse().getContentAsString(),
                TrackingDto.class);

        Assertions.assertThat(trackingDto.getOrders()).hasSize(1);
        DeliveryDto delivery = trackingDto.getDelivery();
        Assertions.assertThat(delivery.isRescheduleAvailabilityHidden()).isTrue();
        Assertions.assertThat(delivery.getIntervalDisplayMode()).isEqualTo(TrackingIntervalDisplayMode.HIDE);
    }
}
