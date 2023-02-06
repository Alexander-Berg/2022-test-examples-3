package ru.yandex.market.checkout.checkouter.pay.bnpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.auth.AuthService;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.BnplTestHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.GAID;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.MM_DEVICE_ID;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER_COOKIE;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplNoAuthParameters;
import static ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer.POST_ORDER_CREATE;
import static ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer.POST_PLAN_CHECK;

public class BnplNoAuthPaymentTest extends AbstractWebTestBase {

    private static final String NO_AUTH_ID = "3621963471644421204";

    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private AuthService authService;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        trustMockConfigurer.mockWholeTrust();
    }

    @DisplayName("Создаем bnpl-заказ для незалогина в вебе")
    @Test
    public void testUnauthorizedByYandexUidPayment() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.BNPL_FOR_UNAUTHORIZED_USER, true);

        AuthInfo authInfo = authHelper.getAuthInfo();
        Long uid = authInfo.getMuid();

        Parameters parameters = defaultBnplNoAuthParameters(uid);
        parameters.getBuyer().setYandexUid(NO_AUTH_ID);

        bnplMockConfigurer.mockWholeNoAuthBnpl(YANDEX_UID_HEADER_COOKIE);

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.isNoAuth());

        orderPayHelper.payWithRealResponse(order);

        assertTrue(order.isBnpl());

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        var planCheckHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);

        assertNotNull(orderCreateHeaders);
        assertEquals(NO_AUTH_ID, orderCreateHeaders.getHeader(YANDEX_UID_HEADER_COOKIE).firstValue());

        var orderCreateHeaderKeys = orderCreateHeaders.keys();
        assertFalse(orderCreateHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(orderCreateHeaderKeys.contains(GAID));
        assertFalse(orderCreateHeaderKeys.contains(MM_DEVICE_ID));

        assertNotNull(planCheckHeaders);
        assertEquals(NO_AUTH_ID, planCheckHeaders.getHeader(YANDEX_UID_HEADER_COOKIE).firstValue());

        var planCheckHeaderKeys = orderCreateHeaders.keys();
        assertFalse(planCheckHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(planCheckHeaderKeys.contains(GAID));
        assertFalse(planCheckHeaderKeys.contains(MM_DEVICE_ID));
    }

    @DisplayName("Создаем bnpl-заказ для незалогина с андроида")
    @Test
    public void testUnauthorizedByGoogleIdPayment() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.BNPL_FOR_UNAUTHORIZED_USER, true);

        AuthInfo authInfo = authHelper.getAuthInfo();
        Long uid = authInfo.getMuid();
        String deviceId = "{\"androidBuildModel\":\"Redmi 5A\"," +
                "\"androidDeviceId\":\"deadbeeff8f2e9fc\"," +
                "\"googleServiceId\":\"" + NO_AUTH_ID + "\"," +
                "\"androidBuildManufacturer\":\"Xiaomi\"," +
                "\"androidHardwareSerial\":\"f0ad12345678\"}";

        Parameters parameters = defaultBnplNoAuthParameters(uid);
        parameters.getBuyer().setYandexUid(null);
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, deviceId);

        bnplMockConfigurer.mockWholeNoAuthBnpl(GAID);

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.isNoAuth());

        orderPayHelper.payWithRealResponse(order);

        assertTrue(order.isBnpl());

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        var planCheckHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);

        assertNotNull(orderCreateHeaders);
        assertEquals(NO_AUTH_ID, orderCreateHeaders.getHeader(GAID).firstValue());

        var orderCreateHeaderKeys = orderCreateHeaders.keys();
        assertFalse(orderCreateHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(orderCreateHeaderKeys.contains(YANDEX_UID_HEADER_COOKIE));
        assertFalse(orderCreateHeaderKeys.contains(MM_DEVICE_ID));

        assertNotNull(planCheckHeaders);
        assertEquals(NO_AUTH_ID, planCheckHeaders.getHeader(GAID).firstValue());

        var planCheckHeaderKeys = orderCreateHeaders.keys();
        assertFalse(planCheckHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(planCheckHeaderKeys.contains(YANDEX_UID_HEADER_COOKIE));
        assertFalse(planCheckHeaderKeys.contains(MM_DEVICE_ID));
    }

    @DisplayName("Создаем bnpl-заказ для незалогина с айфона")
    @Test
    public void testUnauthorizedByIosIdPayment() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.BNPL_FOR_UNAUTHORIZED_USER, true);

        AuthInfo authInfo = authHelper.getAuthInfo();
        Long uid = authInfo.getMuid();
        String deviceId = "{\"ios_device_id\":\"" + NO_AUTH_ID + "\"}";

        Parameters parameters = defaultBnplNoAuthParameters(uid);
        parameters.getBuyer().setYandexUid(null);
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, deviceId);

        bnplMockConfigurer.mockWholeNoAuthBnpl(MM_DEVICE_ID);

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.isNoAuth());

        orderPayHelper.payWithRealResponse(order);

        assertTrue(order.isBnpl());

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        var planCheckHeaders = BnplTestHelper.findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);

        assertNotNull(orderCreateHeaders);
        assertEquals(NO_AUTH_ID, orderCreateHeaders.getHeader(MM_DEVICE_ID).firstValue());

        var orderCreateHeaderKeys = orderCreateHeaders.keys();
        assertFalse(orderCreateHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(orderCreateHeaderKeys.contains(GAID));
        assertFalse(orderCreateHeaderKeys.contains(YANDEX_UID_HEADER_COOKIE));

        assertNotNull(planCheckHeaders);
        assertEquals(NO_AUTH_ID, planCheckHeaders.getHeader(MM_DEVICE_ID).firstValue());

        var planCheckHeaderKeys = orderCreateHeaders.keys();
        assertFalse(planCheckHeaderKeys.contains(YANDEX_UID_HEADER));
        assertFalse(planCheckHeaderKeys.contains(GAID));
        assertFalse(planCheckHeaderKeys.contains(YANDEX_UID_HEADER_COOKIE));
    }

    @DisplayName("Проверка на закрытие bnpl для незалогина с выключенным тогглом")
    @Test
    public void testUnauthorizedByToggleOn() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        Long uid = authInfo.getMuid();

        Parameters parameters = defaultBnplNoAuthParameters(uid);
        parameters.getBuyer().setYandexUid(NO_AUTH_ID);

        bnplMockConfigurer.mockWholeNoAuthBnpl(YANDEX_UID_HEADER_COOKIE);

        Order order = orderCreateHelper.createOrder(parameters);

        assertTrue(order.isNoAuth());

        orderPayHelper.payWithRealResponse(order);
        assertFalse(order.isBnpl());
    }

    @DisplayName("Склейка bnpl-платежа для незалогина")
    @Test
    public void testUnauthorizedCallBackPayment() throws Exception {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.BNPL_FOR_UNAUTHORIZED_USER, true);

        AuthInfo authInfo = authHelper.getAuthInfo();
        Long uid = authInfo.getMuid();

        Parameters parameters = defaultBnplNoAuthParameters(uid);
        parameters.getBuyer().setYandexUid(NO_AUTH_ID);

        bnplMockConfigurer.mockWholeNoAuthBnpl(YANDEX_UID_HEADER_COOKIE);

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        Payment payment = paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);

        assertNull(authService.getPassportUid(order));
        assertTrue(order.isNoAuth());

        orderPayHelper.notifyBnplFinished(payment, 4086639770L);

        order = orderService.getOrder(order.getId());
        assertFalse(order.isNoAuth());
        assertEquals(4086639770L, authService.getPassportUid(order));

        payment = paymentService.getPayment(payment.getId(), ClientInfo.SYSTEM);
        assertEquals(4086639770L, payment.getUid());
    }

}
