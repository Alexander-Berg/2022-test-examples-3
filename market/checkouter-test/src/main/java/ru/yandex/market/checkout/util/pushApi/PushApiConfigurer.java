package ru.yandex.market.checkout.util.pushApi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.jetbrains.annotations.NotNull;
import org.junit.platform.commons.util.StringUtils;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;

import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderApprove;
import ru.yandex.market.checkout.checkouter.order.OrderFailure;
import ru.yandex.market.checkout.checkouter.order.OrderFailureException;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.error.ErrorSubCode;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;

/**
 * Данный класс предназначен для формирования мокированных ответов при актуализации/резервации заказов.
 * Когда-то давным давно все данные запросы ходили в push-api и большое количество тестов мокирует ответы
 * ручек /cart и /accept push-api.
 * Потом появились Fulfillment заказы для которых поход в push-api не нужен, так как это заказы самого Маркета
 * и их не надо подтверждать.
 * <p>
 * В итоге данный класс теперь поддерживает по сути 2 разных режима проверки мокированных ответов:
 * 1. Если заказ Fulfillment, то запрос теперь не идёт в push-api и единственный вариант удостовериться это
 * проверить моки интерфейсов OrderApprove и PushApiCartResponseFetcher
 * 2. Если заказ не Fulfillment, то можно проверять любым способом, так как шпионские методы для OrderApprove и
 * PushApiCartResponseFetcher не только возвращают мокированные ответы, но и делают реальные вызовы в моки push-api.
 */
@TestComponent
public class PushApiConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(PushApiConfigurer.class);
    @Autowired
    private WireMockServer pushApiMock;
    @Autowired
    private OrderApprove orderApprove;
    @Autowired
    private TestSerializationService serializationService;
    @Captor
    private ArgumentCaptor<Order> cartOrderCaptor;
    @Captor
    private ArgumentCaptor<Order> acceptOrderCaptor;

    public PushApiConfigurer() {
        resetMocks();
    }

    public void resetMocks() {
        if (pushApiMock != null) {
            pushApiMock.resetAll();
        }
        MockitoAnnotations.initMocks(this);
    }

    public void mockCart(Order order, List<DeliveryResponse> deliveryOptions, boolean multiCartMock) {
        mockCart(order.getItems(), order.getShopId(), deliveryOptions, order.getAcceptMethod(), multiCartMock);
    }

    public void mockCart(Order order, List<DeliveryResponse> deliveryOptions, List<PaymentMethod> paymentMethods,
                         boolean multiCartMock) {
        CartResponse response = generateResponse(order.getItems(), deliveryOptions, order.getAcceptMethod(),
                paymentMethods);
        mockCart(order, multiCartMock, response);
    }

    public void mockCart(Collection<OrderItem> items, Long shopId, List<DeliveryResponse> deliveryOptions,
                         OrderAcceptMethod acceptMethod, boolean multiCartMock) {
        CartResponse response = generateResponse(items, deliveryOptions, acceptMethod);
        mockCart(items, shopId, multiCartMock, response);
    }

    public void mockCart(Order order, boolean multiCartMock, CartResponse response) {
        mockCart(order.getItems(), order.getShopId(), multiCartMock, response);
    }

    public void mockCart(Collection<OrderItem> items, Long shopId, boolean multiCartMock, CartResponse response) {
        MappingBuilder builder = post(urlPathEqualTo("/shops/" + shopId + "/cart"));
        String offerId = null;
        if (multiCartMock && !CollectionUtils.isEmpty(items)) {
            OrderItem firstItem = items.iterator().next();
            offerId = firstItem.getOfferId();
            if (StringUtils.isNotBlank(offerId)) {
                builder.withRequestBody(WireMock.containing("offer-id=\"" + offerId + "\""));
            }
        }
        String stringResponse = responseToString(response);

        pushApiMock.stubFor(builder
                .willReturn(new ResponseDefinitionBuilder()
                        .withHeader("Content-type", MediaType.APPLICATION_XML_VALUE)
                        .withBody(stringResponse)
                ));
    }

    public void mockCart500Failure(Long shopId) {
        MappingBuilder builder = post(urlPathEqualTo("/shops/" + shopId + "/cart"));
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("<error>\n" +
                                "   <code>INTERNAL_SERVER_ERROR</code>\n" +
                                "   <message>trololo</message>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n")));
    }

    public void mockCartShopFailure(Long shopId, boolean shopAdmin) {
        MappingBuilder builder = post(urlPathEqualTo("/shops/" + shopId + "/cart"));
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_GATEWAY.value())
                        .withBody("<error>\n" +
                                "   <code>HTTP</code>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n")));
    }


    public void mockAccept(Order order) {
        mockAccept(order, true);
    }

    public void mockAccept(Order order, boolean accepted) {
        mockAccept(Collections.singletonList(order), accepted);
    }

    /**
     * Мокирует корректную резервацию заказа (или отказ в резервации)
     *
     * @param orders   набор заказов
     * @param accepted принять заказ
     */
    public void mockAccept(List<Order> orders, boolean accepted) {
        for (Order order : orders) {
            OrderResponse response = new OrderResponse();
            response.setAccepted(accepted);
            if (accepted) {
                response.setId("100500");
            }
            ResponseDefinitionBuilder responseDefBuilder = new ResponseDefinitionBuilder()
                    .withHeader("Content-type", MediaType.APPLICATION_XML_VALUE)
                    .withBody(serializationService.serializePushApiObject(response));

            MappingBuilder builder = acceptOrderRequest(order);
            pushApiMock.stubFor(builder.willReturn(responseDefBuilder));
            mockOrderApprove(order, response);
        }
    }

    private void mockOrderApprove(Order order, OrderResponse response) {
        Mockito.doAnswer(a -> {
            OrderResponse realAnswer = (OrderResponse) a.callRealMethod();
            Order o = a.getArgument(0);
            if (Objects.equals(o.getShopId(), order.getShopId())) {
                return response;
            }
            return realAnswer;
        }).when(orderApprove).approve(acceptOrderCaptor.capture(), any(Function.class));
    }

    @NotNull
    private MappingBuilder acceptOrderRequest(Order order) {
        return post(urlPathEqualTo("/shops/" + order.getShopId() + "/order/accept"));
    }

    /**
     * Мокирует внутреннюю ошибку сервера при резервации заказа.
     * По сути мокирование идёт по идентификатору магазина (Order::getShopId)
     *
     * @param order заказ
     */
    public void mockAcceptFailure(Order order) {
        MappingBuilder builder = acceptOrderRequest(order);
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("<error>\n" +
                                "   <code>INTERNAL_SERVER_ERROR</code>\n" +
                                "   <message>trololo</message>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n"))
        );
        mockOrderApproveException(order, OrderFailure.Code.UNKNOWN_ERROR, "INTERNAL_SERVER_ERROR");
    }

    private void mockOrderApproveException(Order order, OrderFailure.Code errorCode, String details) {
        Mockito.doAnswer(a -> {
            OrderResponse realAnswer = (OrderResponse) a.callRealMethod();
            Order o = a.getArgument(0);
            if (Objects.equals(o.getShopId(), order.getShopId())) {
                if (errorCode != OrderFailure.Code.UNKNOWN_ERROR) {
                    throw new OrderFailureException(new OrderFailure(o, errorCode, details));
                } else {
                    throw new ResourceAccessException("Push api is not available");
                }
            }
            return realAnswer;

        }).when(orderApprove).approve(any(Order.class), any(Function.class));
    }

    /**
     * Мокирует ошибку таймату при резервации заказа.
     * По сути мокирование идёт по идентификатору магазина (Order::getShopId)
     *
     * @param order заказ
     */
    public void mockAcceptShopFailure(Order order) {
        MappingBuilder builder = acceptOrderRequest(order);
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.UNPROCESSABLE_ENTITY.value())
                        .withBody("<error>\n" +
                                "   <code>CONNECTION_TIMED_OUT</code>\n" +
                                "   <message>trololo</message>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n"))
        );
        mockOrderApproveException(order, OrderFailure.Code.SHOP_ERROR, "CONNECTION_TIMED_OUT");
    }

    /**
     * Мокирует внутреннюю ошибку сервера при резервации заказа.
     * По сути мокирование идёт по идентификатору магазина (Order::getShopId)
     *
     * @param order заказ
     */
    public void mockAcceptShopFailure(Order order, ErrorSubCode error) {
        MappingBuilder builder = acceptOrderRequest(order);
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withBody("<error>\n" +
                                "   <code>" + error.name() + "</code>\n" +
                                "   <message>trololo</message>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n"))
        );
        mockOrderApproveException(order, OrderFailure.Code.UNKNOWN_ERROR, "INTERNAL_SERVER_ERROR");
    }

    /**
     * Мокирует внутреннюю ошибку сервера при резервации заказа.
     * По сути мокирование идёт по идентификатору магазина (Order::getShopId)
     *
     * @param order заказ
     */
    public void mockServiceFault(Order order) {
        MappingBuilder builder = acceptOrderRequest(order);
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.BAD_GATEWAY.value()))
        );
    }

    /**
     * Имитирует ошибку ответа сервера с таймайтом при резервации заказа.
     * По сути мокирование идёт по идентификатору магазина (Order::getShopId)
     *
     * @param order заказ
     */
    public void mockDelayedShopFailure(Order order, int delay) {
        MappingBuilder builder = acceptOrderRequest(order);
        pushApiMock.stubFor(builder
                .willReturn(aResponse()
                        .withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .withFixedDelay(delay)
                        .withBody("<error>\n" +
                                "   <code>CONNECTION_TIMED_OUT</code>\n" +
                                "   <message>trololo</message>\n" +
                                "   <shop-admin>false</shop-admin>\n" +
                                "</error>\n"))
        );
        mockOrderApproveException(order, OrderFailure.Code.SHOP_ERROR, "CONNECTION_TIMED_OUT");
    }

    /**
     * Используется для проверки запросов к push-api, в том числе проверки URL, параметров запроса, тела запроса и т.п.
     *
     * @param requestPatternBuilder билдер для построения фильтра искомых запросов
     * @return {@link XmlMatcherBuilder}
     */
    public XmlMatcherBuilder verify(RequestPatternBuilder requestPatternBuilder) {
        List<LoggedRequest> requests = pushApiMock.findAll(requestPatternBuilder);
        assertThat(requests, hasSize(greaterThan(0)));
        requests.forEach(loggedRequest -> LOG.debug(loggedRequest.getBodyAsString()));
        return new XmlMatcherBuilder(requests);
    }

    private CartResponse generateResponse(@Nonnull Collection<OrderItem> items,
                                          @Nullable List<DeliveryResponse> deliveryOptions,
                                          @Nullable OrderAcceptMethod acceptMethod) {
        CartResponse result = new CartResponse(
                new ArrayList<>(items),
                deliveryOptions,
                deliveryOptions == null ?
                        null :
                        deliveryOptions.stream()
                                .flatMap(r -> r.getPaymentOptions().stream())
                                .distinct()
                                .collect(Collectors.toList())
        );
        if (acceptMethod == OrderAcceptMethod.WEB_INTERFACE) {
            result.setShopAdmin(true);
        }
        return result;
    }

    private String responseToString(CartResponse response) {
        return serializationService.serializePushApiObject(response);
    }

    private CartResponse responseFromString(String response) {
        return serializationService.deserializePushApiObject(response, CartResponse.class);
    }

    private CartResponse generateResponse(@Nonnull Collection<OrderItem> items,
                                          @Nullable List<DeliveryResponse> deliveryOptions,
                                          @Nullable OrderAcceptMethod acceptMethod,
                                          @Nullable List<PaymentMethod> paymentMethods
    ) {
        CartResponse result = new CartResponse(
                new ArrayList<>(items),
                deliveryOptions,
                paymentMethods
        );
        if (acceptMethod == OrderAcceptMethod.WEB_INTERFACE) {
            result.setShopAdmin(true);
        }
        return result;
    }

    /**
     * Используется для контроля значения заказа пришедшего на актуализацию.
     *
     * @param matcher матчер
     */
    public void assertCartOrder(Matcher<Order> matcher) {
        MatcherAssert.assertThat(cartOrderCaptor.getValue(), matcher);
    }

    /**
     * Используется для контроля параметров заказа, который пришёл на резервацию.
     *
     * @param matcher матчер
     */
    public void assertAcceptOrder(Matcher<Order> matcher) {
        MatcherAssert.assertThat(acceptOrderCaptor.getValue(), matcher);
    }

    /**
     * Получение заказа от ArgumentCaptor.
     * Рекомендуется использовать если имеется сложная логика валидации через матчеры в методе
     * {@link #assertAcceptOrder}
     *
     * @return Заказ, который пришёл на вход в {@link OrderApprove#approve}
     */
    public Order getAcceptOrder() {
        return acceptOrderCaptor.getValue();
    }
}
