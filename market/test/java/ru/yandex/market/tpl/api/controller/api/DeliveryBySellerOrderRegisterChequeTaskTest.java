package ru.yandex.market.tpl.api.controller.api;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.tpl.api.BaseApiIntTest;
import ru.yandex.market.tpl.api.model.order.OrderChequeRemoteDto;
import ru.yandex.market.tpl.api.model.order.OrderChequeType;
import ru.yandex.market.tpl.api.model.order.OrderPaymentType;
import ru.yandex.market.tpl.api.model.receipt.CreateReceiptRequestDto;
import ru.yandex.market.tpl.api.model.receipt.ReceiptServiceClientDto;
import ru.yandex.market.tpl.common.web.util.Idempotency;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderGenerateService;
import ru.yandex.market.tpl.core.domain.partner.SortingCenterService;
import ru.yandex.market.tpl.core.domain.receipt.ReceiptService;
import ru.yandex.market.tpl.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.core.domain.user.User;
import ru.yandex.market.tpl.core.domain.user.UserProperties;
import ru.yandex.market.tpl.core.domain.user.UserPropertyService;
import ru.yandex.market.tpl.core.domain.user.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.tpl.core.service.tracking.TrackingService.CONTACTLESS_DELIVERY_PREFIX;

class DeliveryBySellerOrderRegisterChequeTaskTest extends BaseApiIntTest {
    @Autowired
    private TestUserHelper userHelper;
    @Autowired
    private OrderGenerateService orderGenerateService;
    @Autowired
    private UserPropertyService userPropertyService;
    @Autowired
    private ObjectMapper objectMapper;

    private ReceiptService receiptService;
    private UserService userService;
    private SortingCenterService sortingCenterService;

    private Long taskId;
    private Long routePointId;
    private User user;
    private Order order;

    @BeforeEach
    public void setUp() {
        receiptService = spy(ReceiptService.class);
        userService = spy(UserService.class);
        sortingCenterService = spy(SortingCenterService.class);
        this.user = userHelper.createOrFindDbsUser();
        mockBlackboxClient(this.user.getUid());
        doReturn(true).when(sortingCenterService).usePvz(any());

        userPropertyService.addPropertyToUser(user, UserProperties.FEATURE_DELIVERY_PHOTO_ENABLED, true);
        userHelper.createOrFindDbsDeliveryService();
        order = orderGenerateService.createOrder(OrderGenerateService.OrderGenerateParam.builder()
                .paymentType(OrderPaymentType.CARD)
                .recipientNotes(CONTACTLESS_DELIVERY_PREFIX)
                .deliveryServiceId(TestUserHelper.DBS_DELIVERY_SERVICE_ID)
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

    @AfterEach
    void after() {
        Mockito.reset(userService);
        Mockito.reset(sortingCenterService);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                    "/api/route-points/0/tasks/order-delivery/{task-id}/register-cheque",
                    "/api/tasks/order-delivery-single/{task-id}/pay-and-register-cheque",
                    "/api/tasks/order-delivery-single/{task-id}/register-cheque",
            }
    )
    @SneakyThrows
    void registerCheque(String urlTemplate) {
        OrderChequeRemoteDto body = new OrderChequeRemoteDto(OrderPaymentType.CARD, OrderChequeType.SELL);

        mockMvc.perform(post(urlTemplate, taskId)
                        .header(AUTHORIZATION, AUTH_HEADER_VALUE)
                        .header(Idempotency.IDEMPOTENCY_HEADER_KEY, UUID.randomUUID())
                        .content(objectMapper.writeValueAsString(body))
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(receiptService, times(0))
                .createReceiptData(any(CreateReceiptRequestDto.class), any(ReceiptServiceClientDto.class));
    }
}
