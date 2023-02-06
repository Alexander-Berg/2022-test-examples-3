package ru.yandex.market.checkout.checkouter.controller;

import java.text.SimpleDateFormat;
import java.util.List;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.common.util.date.DateUtil;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.config.web.ErrorsConfig;
import ru.yandex.market.checkout.checkouter.config.web.ViewsConfig;
import ru.yandex.market.checkout.checkouter.controllers.oms.PaymentController;
import ru.yandex.market.checkout.checkouter.mocks.Mocks;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.views.services.PaymentViewModelService;
import ru.yandex.market.checkout.checkouter.views.services.mappers.PaymentToCreatePaymentResponseMapper;
import ru.yandex.market.checkout.checkouter.views.services.mappers.PaymentToPaymentViewModelMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = PaymentControllerTest.Context.class)
public class PaymentControllerTest extends AbstractControllerTestBase {

    public static final long PAYMENT_ID = 123L;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private OrderService orderService;

    @DisplayName("/payments/basket должен возвращать 404, если платеж - фейковый.")
    @Tag(Tags.AUTO)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    @Test
    public void shouldReturnNotFoundIfOrderIsFake() throws Exception {
        Payment payment = new Payment();
        payment.setFake(true);

        Mockito.doReturn(payment)
                .when(paymentService).findPayment(PAYMENT_ID, ClientInfo.SYSTEM);
        Mockito.doReturn(List.of(1L)).when(orderService).getOrderIdsByPayment(PAYMENT_ID);
        Mockito.doNothing().when(orderService).checkOrderAccess(1L, ClientInfo.SYSTEM);

        mockMvc.perform(get("/payments/{paymentId}/basket", PAYMENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Payment " + PAYMENT_ID + " is sandbox"));
    }

    @Configuration
    @Import({ViewsConfig.class, ErrorsConfig.class})
    @ImportResource({"classpath:int-test-views.xml"})
    static class Context {

        @Bean
        public PaymentService paymentService() {
            return Mocks.createMock(PaymentService.class);
        }

        @Bean
        public OrderService orderService() {
            return Mocks.createMock(OrderService.class);
        }

        @Bean
        public ThreadLocal<SimpleDateFormat> simpleDateFormatThreadLocal() {
            return DateUtil.newThreadLocalOfSimpleDateFormat("dd-MM-yyyy");
        }

        @Bean
        public PaymentController paymentController(PaymentService paymentService, OrderService orderService) {
            return new PaymentController(
                    new PaymentViewModelService(
                            new PaymentToPaymentViewModelMapper(),
                            new PaymentToCreatePaymentResponseMapper()
                    ),
                    paymentService, orderService, null, null, null, null,
                    null, null, null, null, null, null);
        }
    }
}
