package ru.yandex.market.checkout.checkouter.itemservice;

import java.io.IOException;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceFetcher;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCObjectType;
import ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order.ItemServiceRegistrationPayload;
import ru.yandex.market.checkout.checkouter.yauslugi.rest.YaUslugiClient;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.queuedcalls.QueuedCall;
import ru.yandex.market.queuedcalls.QueuedCallService;
import ru.yandex.market.queuedcalls.QueuedCallType;
import ru.yandex.market.queuedcalls.impl.QueuedCallDao;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.ITEM_SERVICE_REGISTRATION;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.order.ItemServiceRegistrationProcessor.QUEUED_CALL_PAYLOAD_OBJECT_MAPPER;

/**
 * @author gelvy
 * Created on: 15.09.2021
 **/

public class ItemServiceBookingTest extends AbstractWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @Autowired
    private ItemServiceFetcher itemServiceFetcher;

    @Autowired
    private QueuedCallDao queuedCallDao;

    @Autowired
    private YaUslugiClient yaUslugiApi;

    @Mock
    private RestTemplate testRestTemplate;

    @Autowired
    private WireMockServer yaUslugiMock;

    @Value("${market.checkouter.yauslugi.api.url:localhost}")
    private String serviceUrl;

    @AfterEach
    void cleanContext() {
        queuedCallService.setQueuedCallDao(queuedCallDao);
    }

    @Test
    public void shouldAddItemServiceRegistrationQueueCall() throws IOException {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();

        String reqId = "someReqId";
        Order order = orderCreateHelper.createOrder(parameters, multiCart ->
            multiCart.getCarts()
                    .get(0)
                    .getItemServices()
                    .iterator()
                    .next()
                    .setYaUslugiTimeslotReqId(reqId)
        );

        ItemService itemService = order.getItemServices().iterator().next();
        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                ITEM_SERVICE_REGISTRATION,
                itemService.getId());
        assertEquals(1, result.size());
        QueuedCall queuedCall = result.iterator().next();
        ItemServiceRegistrationPayload payload = QUEUED_CALL_PAYLOAD_OBJECT_MAPPER.readValue(
                queuedCall.getPayload(), ItemServiceRegistrationPayload.class
        );
        assertEquals(reqId, payload.getYaUslugiTimeslotReqId());
    }

    @Test
    public void shouldAddItemServicesRegistrationQueueCalls() throws InterruptedException {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(2, result.size());
        Set<QueuedCallType> qcTypes = result.stream()
                .map(QueuedCall::getCallType)
                .collect(Collectors.toSet());
        assertEquals(1, qcTypes.size());
        assertEquals(ITEM_SERVICE_REGISTRATION, qcTypes.iterator().next());
    }

    @Test
    public void shouldCancelItemServiceWhenCantAddQueuedCall() throws InterruptedException {
        QueuedCallDao qcDao = mock(QueuedCallDao.class);
        queuedCallService.setQueuedCallDao(qcDao);

        doThrow(new RuntimeException()).when(qcDao).createQueuedCalls(any(), any());

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Long serviceId = extractServiceIds(order).iterator().next();
        ItemService itemService = itemServiceFetcher.fetchById(serviceId);

        assertEquals(ItemServiceStatus.CANCELLED, itemService.getStatus());
    }

    @Test
    public void addTwoOrderAndCheckExecutionTest() throws Exception {
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .willReturn(okJson("{}")));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(2, result.size());
        Set<QueuedCallType> qcTypes = result.stream()
                .map(QueuedCall::getCallType)
                .collect(Collectors.toSet());
        assertEquals(1, qcTypes.size());
        assertEquals(ITEM_SERVICE_REGISTRATION, qcTypes.iterator().next());

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertTrue(queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).isEmpty());
    }

    @Test
    public void addTwoOrderAndCheckRetryExecutionTest() throws Exception {
        Mockito
                .doThrow(new RuntimeException())
                .doThrow(new RuntimeException())
                .doReturn(null)
                .when(testRestTemplate).postForObject(
                        Mockito.eq(new URI(serviceUrl + "/ydo/api/market_partner_orders/services")),
                Mockito.any(HttpEntity.class), Mockito.eq(Void.class));

        yaUslugiApi.setRestTemplate(testRestTemplate);

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.addItemService();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        Collection<Long> serviceIds = extractServiceIds(order);

        Collection<QueuedCall> result = queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(it -> it.getCallType() == ITEM_SERVICE_REGISTRATION));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertEquals(2, queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).size());

        // смещаемся на 9, чтобы убедиться, что время еще не настало проверки
        setFixedTime(getClock().instant().plus(9, ChronoUnit.MINUTES));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertEquals(2, queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).size());

        // смещаемся еще на 1 минуту
        setFixedTime(getClock().instant().plus(1, ChronoUnit.MINUTES));

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertEquals(0, queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).size());
    }

    @Test
    public void itemServiceHasPaymentTypeAndYaIdServiceTest() {
        yaUslugiMock.stubFor(
                post(urlPathEqualTo("/ydo/api/market_partner_orders/services"))
                        .withRequestBody(containing("yaServiceId"))
                        .withRequestBody(containing("paymentType"))
                        .withRequestBody(containing("paymentMethod"))
                        .willReturn(okJson("{}")));

        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setPaymentMethod(PaymentMethod.GOOGLE_PAY);
        parameters.addItemService();
        parameters.addItemService();
        Order order = orderCreateHelper.createOrder(parameters);

        assertNotNull(order.getPaymentType());
        assertNotNull(order.getPaymentMethod());
        assertFalse(order.getItems().isEmpty());

        order.getItems()
                .forEach(orderItem -> {
                    assertFalse(orderItem.getServices().isEmpty());

                    orderItem.getServices()
                            .forEach(it -> {
                                if (checkouterProperties.getEnableServicesPrepay() == Boolean.TRUE) {
                                    assertEquals(order.getPaymentType(), it.getPaymentType());
                                    assertEquals(order.getPaymentMethod(), it.getPaymentMethod());
                                } else {
                                    assertEquals(PaymentType.POSTPAID, it.getPaymentType());
                                    assertEquals(PaymentMethod.CARD_ON_DELIVERY, it.getPaymentMethod());
                                }
                            });
                });

        Collection<Long> serviceIds = extractServiceIds(order);
        long serviceId = serviceIds.iterator().next();

        ItemService itemService = itemServiceFetcher.fetchById(serviceId);
        assertNotNull(itemService.getPaymentType());
        assertNotNull(itemService.getYaServiceId());
        assertNotNull(itemService.getTitle());
        assertNotNull(itemService.getDescription());
        assertNotNull(itemService.getPaymentMethod());

        queuedCallService.executeQueuedCallBatch(ITEM_SERVICE_REGISTRATION);

        assertEquals(0, queuedCallService.findQueuedCalls(
                CheckouterQCObjectType.ITEM_SERVICE,
                serviceIds).size());
    }

    private Collection<Long> extractServiceIds(Order order) {
        return order.getItems().stream()
                .map(OrderItem::getServices)
                .flatMap(Collection::stream)
                .map(ItemService::getId)
                .collect(Collectors.toSet());
    }
}
