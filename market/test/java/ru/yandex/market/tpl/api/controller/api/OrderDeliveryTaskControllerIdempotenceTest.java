package ru.yandex.market.tpl.api.controller.api;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaidBatchDto;
import ru.yandex.market.tpl.api.model.order.OrderPaidDto;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.common.util.validation.ValidRequestBodyList;
import ru.yandex.market.tpl.common.web.util.Idempotency;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.CONTACTLESS_DELIVERY_PREFIX;

public class OrderDeliveryTaskControllerIdempotenceTest extends BaseApiIntTest {
    @Autowired
    private TestUserHelper userHelper;
    @Autowired
    private OrderGenerateService orderGenerateService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserPropertyService userPropertyService;

    private Long taskId;
    private Long routePointId;
    private User user;

    @BeforeEach
    public void setUp() {
        this.user = userHelper.findOrCreateUser(824125L);
        mockBlackboxClient(this.user.getUid());
        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_DELIVERY_PHOTO_ENABLED, true);
        var order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CARD)
                .recipientNotes(CONTACTLESS_DELIVERY_PREFIX)
                .build());

        var shift = userHelper.findOrCreateOpenShift(LocalDate.now());
        var userShift = userHelper.createEmptyShift(user, shift);
        var deliveryTask = userHelper.addDeliveryTaskToShift(user, userShift, order);
        userHelper.openShift(user, userShift.getId());

        this.taskId = deliveryTask.getId();
        this.routePointId = userShift.findRoutePointIdByTaskId(taskId).orElseThrow();

        userHelper.finishPickupAtStartOfTheDay(userShift);
        userHelper.arriveAtRoutePoint(userShift, routePointId);

    }

    @SneakyThrows
    @Test
    void shouldAllowToCallPayTwice() {
        OrderPaidDto body = new OrderPaidDto(OrderPaymentType.CARD);

        UUID idempotencyKey = new UUID(0x13371337, 0x13371337);

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(log())
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldAllowToCallPrintChequeTwice() {
        OrderPaidDto body = new OrderPaidDto(OrderPaymentType.CARD);

        UUID idempotencyKey = new UUID(0x13371337, 0x13371337);

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(log())
                .andExpect(status().isOk());
    }

    @SneakyThrows
    @Test
    void shouldAllowToCallPayBatchTwice() {
        var batch = new OrderPaidBatchDto();
        batch.setTaskId(taskId);
        batch.setPaymentType(OrderPaymentType.CARD);

        ValidRequestBodyList<OrderPaidBatchDto> body = new ValidRequestBodyList<>(
                List.of(batch)
        );

        UUID idempotencyKey = new UUID(0x13371337, 0x13371337);

        mockMvc.perform(post("/api/tasks/order-delivery/pay-batch", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-delivery/pay-batch", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(log())
                .andExpect(status().isOk());
    }


    @SneakyThrows
    @Test
    void shouldAllowToCallPayAndRegisterChequeTwice() {
        OrderChequeRemoteDto body = new OrderChequeRemoteDto(OrderPaymentType.CARD, OrderChequeType.SELL);

        UUID idempotencyKey = new UUID(0x13371337, 0x13371337);

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay-and-register-cheque", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/tasks/order-delivery-single/{task-id}/pay-and-register-cheque", taskId)
                .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                .header(Idempotency.IDEMPOTENCY_HEADER_KEY, idempotencyKey.toString())
                .content(objectMapper.writeValueAsString(body))
                .contentType(MediaType.APPLICATION_JSON)
        )
                .andDo(log())
                .andExpect(status().isOk());
    }




}
