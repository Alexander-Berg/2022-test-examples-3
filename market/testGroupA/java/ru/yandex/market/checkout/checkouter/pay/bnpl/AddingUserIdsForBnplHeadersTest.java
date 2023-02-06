package ru.yandex.market.checkout.checkouter.pay.bnpl;

import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.checkouter.pay.PaymentService;
import ru.yandex.market.checkout.checkouter.viewmodel.CreatePaymentResponse;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.GAID;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.MM_DEVICE_ID;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER;
import static ru.yandex.market.checkout.checkouter.pay.bnpl.model.BnplUserId.YANDEX_UID_HEADER_COOKIE;
import static ru.yandex.market.checkout.helpers.BnplTestHelper.findHttpHeaderByFirstRelevantUrl;
import static ru.yandex.market.checkout.providers.BnplTestProvider.defaultBnplParameters;
import static ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer.POST_ORDER_CREATE;
import static ru.yandex.market.checkout.util.bnpl.BnplMockConfigurer.POST_PLAN_CHECK;

public class AddingUserIdsForBnplHeadersTest extends AbstractWebTestBase {

    private static final String ANY_UID = "3621963471644421204";

    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private BnplMockConfigurer bnplMockConfigurer;
    @Autowired
    private PaymentService paymentService;

    @BeforeEach
    public void mockBnpl() {
        checkouterProperties.setEnableServicesPrepay(true);
        checkouterProperties.setEnableBnpl(true);
        trustMockConfigurer.mockWholeTrust();
        bnplMockConfigurer.mockWholeBnpl();
    }

    @Test
    public void addUidAndYandexUidForHeaders() {
        bnplMockConfigurer.mockWholeBnpl();

        Parameters parameters = defaultBnplParameters();
        parameters.getBuyer().setYandexUid(ANY_UID);

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        var planCheckHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);


        assertEquals("359953025", Objects.requireNonNull(planCheckHeaders).getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, planCheckHeaders.getHeader(YANDEX_UID_HEADER_COOKIE).firstValue());

        assertEquals("359953025", Objects.requireNonNull(orderCreateHeaders).getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, orderCreateHeaders.getHeader(YANDEX_UID_HEADER_COOKIE).firstValue());
    }

    @Test
    public void addUidAndIosIdForHeaders() {
        bnplMockConfigurer.mockWholeBnpl();

        Parameters parameters = defaultBnplParameters();
        parameters.getBuyer().setYandexUid(null);
        String iosId = "{\"ios_device_id\":\"" + ANY_UID + "\"}";

        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, iosId);

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        assertNotNull(orderCreateHeaders);

        assertEquals("359953025", orderCreateHeaders.getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, orderCreateHeaders.getHeader(MM_DEVICE_ID).firstValue());

        var planCheckHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);
        assertNotNull(planCheckHeaders);

        assertEquals("359953025", planCheckHeaders.getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, planCheckHeaders.getHeader(MM_DEVICE_ID).firstValue());
    }

    @Test
    public void addUidAndGoogleUidForHeaders() {
        bnplMockConfigurer.mockWholeBnpl();

        Parameters parameters = defaultBnplParameters();
        parameters.getBuyer().setYandexUid(null);
        String iosId = "{\"ios_device_id\":\"" + ANY_UID + "\"}";
        String googleId = "{\"androidBuildModel\":\"Redmi 5A\"," +
                "\"androidDeviceId\":\"deadbeeff8f2e9fc\"," +
                "\"googleServiceId\":\"" + "3621963471644421204" + "\"," +
                "\"androidBuildManufacturer\":\"Xiaomi\"," +
                "\"androidHardwareSerial\":\"f0ad12345678\"}";
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, iosId);
        parameters.getOrder().setProperty(OrderPropertyType.DEVICE_ID, googleId);

        Order order = orderCreateHelper.createOrder(parameters);
        CreatePaymentResponse paymentResponse = orderPayHelper.payWithRealResponse(order);
        paymentService.getPayment(paymentResponse.getId(), ClientInfo.SYSTEM);

        var events = bnplMockConfigurer.servedEvents();

        var orderCreateHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_ORDER_CREATE);
        var planCheckHeaders = findHttpHeaderByFirstRelevantUrl(events, POST_PLAN_CHECK);

        assertEquals("359953025", Objects.requireNonNull(planCheckHeaders).getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, planCheckHeaders.getHeader(GAID).firstValue());

        assertEquals("359953025", Objects.requireNonNull(orderCreateHeaders).getHeader(YANDEX_UID_HEADER).firstValue());
        assertEquals(ANY_UID, orderCreateHeaders.getHeader(GAID).firstValue());
    }
}
