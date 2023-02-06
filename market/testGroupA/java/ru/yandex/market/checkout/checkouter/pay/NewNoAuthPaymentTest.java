package ru.yandex.market.checkout.checkouter.pay;

import java.util.Arrays;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class NewNoAuthPaymentTest extends AbstractWebTestBase {

    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private OrderPayHelper paymentHelper;

    @Test
    public void shouldProcessPaymentWhenAlreadyHaveUnfinishedNoAuthPayment() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        Parameters parameters = defaultBlueOrderParameters();
        Long authorizedUid = parameters.getBuyer().getUid();

        parameters.getBuyer().setUid(from);
        parameters.getOrder().setUid(from);

        trustMockConfigurer.resetAll();
        Order order = orderCreateHelper.createOrder(parameters);
        Payment payment = paymentHelper.payForOrderWithoutNotification(order);
        assertThat(payment.getUid(), nullValue());

        moveOrder(order, authorizedUid, cookie);
        order = orderService.getOrder(order.getId());
        assertThat(order.getBuyer().getUid(), equalTo(authorizedUid));

        trustMockConfigurer.resetAll();
        Payment newPayment = paymentHelper.payForOrderWithoutNotification(order);
        assertThat(newPayment.getUid(), nullValue());

        trustMockConfigurer.servedEvents()
                .forEach(e -> Assertions.assertNull(e.getRequest().getHeader("X-Uid")));
    }

    @Test
    public void shouldProcessMultiPaymentWhenAlreadyHaveUnfinishedNoAuthMultiPayment() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long from = authInfo.getMuid();
        String responseCookie = authInfo.getCookie();
        Cookie cookie = new Cookie("muid", responseCookie);
        Parameters parameters = defaultBlueOrderParameters();
        Long authorizedUid = parameters.getBuyer().getUid();

        parameters.getBuyer().setUid(from);
        parameters.getOrder().setUid(from);

        trustMockConfigurer.resetAll();
        Order firstOrder = orderCreateHelper.createOrder(parameters);
        Order secondOrder = orderCreateHelper.createOrder(parameters);
        final Payment payment = paymentHelper.payForOrdersWithoutNotification(Arrays.asList(firstOrder, secondOrder));
        assertThat(payment.getUid(), nullValue());

        moveOrder(firstOrder, authorizedUid, cookie);
        moveOrder(secondOrder, authorizedUid, cookie);
        firstOrder = orderService.getOrder(firstOrder.getId());
        assertThat(firstOrder.getBuyer().getUid(), equalTo(authorizedUid));
        secondOrder = orderService.getOrder(secondOrder.getId());
        assertThat(secondOrder.getBuyer().getUid(), equalTo(authorizedUid));

        trustMockConfigurer.resetAll();
        Payment newPayment = paymentHelper.payForOrdersWithoutNotification(Arrays.asList(firstOrder, secondOrder));
        assertThat(newPayment.getUid(), nullValue());

        trustMockConfigurer.servedEvents()
                .forEach(e -> Assertions.assertNull(e.getRequest().getHeader("X-Uid")));
    }

    private void moveOrder(Order order, Long authorizedUid, Cookie cookie) throws Exception {
        Long orderId = order.getId();
        Long from = order.getUid();
        mockMvc.perform(
                post("/move-orders")
                        .content(String.format("{\"orders\": [%d]}", orderId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("fromMuid", from.toString())
                        .param("toUid", authorizedUid.toString())
                        .param("clientRole", ClientRole.USER.name())
                        .param("clientId", from.toString())
                        .cookie(cookie)
        )
                .andExpect(status().isOk());
    }
}
