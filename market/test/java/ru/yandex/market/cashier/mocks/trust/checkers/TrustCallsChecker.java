package ru.yandex.market.cashier.mocks.trust.checkers;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;
import ru.yandex.market.cashier.mocks.OneElementBackIterator;
import ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer;
import ru.yandex.market.cashier.trust.TrustBasketId;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparing;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CANCEL_BASKET_LINE_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CHECK_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CLEAR_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CREATE_ORDERS_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CREATE_PRODUCT_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.CREATE_REFUND_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.DO_REFUND_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.LOAD_PARTNER_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.MARKUP_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.PAY_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.UNHOLD_BASKET_STUB;
import static ru.yandex.market.cashier.mocks.trust.TrustMockConfigurer.UPDATE_BASKET_LINE_STUB;

public class TrustCallsChecker {
    private static final Logger log = LoggerFactory.getLogger(TrustCallsChecker.class);
    private static final String UID_HEADER = "X-Uid";
    private static final String USER_IP_HEADER = "X-User-Ip";
    private static final String YANDEX_UID_HEADER = "X-Yandexuid";
    private static final String SERVICE_TOKEN_HEADER = "X-Service-Token";


    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter, Long tokenId, Long uid) {
        checkBatchServiceOrderCreationCall(eventsIter, uid, tokenId, (List<CreateTrustOrderParams>) null);
    }

    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter, Long tokenId,
                                                          Long uid, List<CreateTrustOrderParams> orderParams) {
        ServeEvent event = eventsIter.next();
        assertEquals(CREATE_ORDERS_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));

        if (orderParams == null) {
            return;
        }

        JsonObject body = getRequestBodyAsJson(event);
        JsonElement orders = body.remove("orders");
        if (orderParams.isEmpty()) {
            if (orders != null) {
                assertThat(orders.getAsJsonArray().size(), equalTo(0));
            }
        } else {
            JsonArray ordersArray = orders.getAsJsonArray();
            assertThat(ordersArray.size(), equalTo(orderParams.size()));

            ordersArray.forEach(jsonOrder -> assertThat(
                    jsonOrder,
                    anyOf(orderParams.stream().map(CreateTrustOrderParams::toJsonMatcher).collect(Collectors.toList
                            ()))));
        }

        assertThat(body.size(), equalTo(0));
    }

    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter, Long tokenId,
                                                          Long uid, CreateTrustOrderParams singleOrderParam)
            throws Exception {
        checkBatchServiceOrderCreationCall(eventsIter, tokenId, uid, Collections.singletonList(singleOrderParam));
    }

    public static void checkOptionalCreateServiceProductCall(
            OneElementBackIterator<ServeEvent> eventIterator, Long tokenId, CreateProductParams params) {
        ServeEvent event = eventIterator.next();

        // Если вызван другой метод или не совпали поля (может быть такой же но, с другими полями),
        // то вероятно попали в кэш, возвращаем итератор на 1 позицию назад
        if (!event.getStubMapping().getName().equals(CREATE_PRODUCT_STUB)) {
            log.warn("Found unexpected call: {}" + event.getStubMapping().getName());
            eventIterator.skipAdvanceOnNext();
            return;
        }
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));

        try {
            if (params != null) {
                params.matches(getRequestBodyAsJson(event));
            }
        } catch (AssertionError e) {
            log.warn("Found unexpected call, CreateProduct method call may be cached: {}" + e.getMessage());
            eventIterator.skipAdvanceOnNext();
        }
    }

    public static void checkLoadPartnerCall(Iterator<ServeEvent> eventsIter, Long tokenId, Long partnerId) {
        ServeEvent event = eventsIter.next();
        assertEquals(LOAD_PARTNER_STUB, event.getStubMapping().getName());
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/partners/" + partnerId));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkOptionalLoadPartnerCall(OneElementBackIterator<ServeEvent> eventsIter, Long tokenId, Long partnerId) {
        ServeEvent event = eventsIter.next();
        if (!event.getStubMapping().getName().equals(LOAD_PARTNER_STUB)) {
            log.warn("Found unexpected call: {}" + event.getStubMapping().getName());
            eventsIter.skipAdvanceOnNext();
        } else {
            assertEquals(LOAD_PARTNER_STUB, event.getStubMapping().getName());
            checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));

            String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
            assertThat(path, endsWith("/partners/" + partnerId));
            assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
        }
    }

    public static void checkCheckBasketCall(Iterator<ServeEvent> eventsIter, TrustBasketId basketId) {
        ServeEvent event = eventsIter.next();
        assertEquals(CHECK_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketId.getServiceTokenId()));


        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketId.getPurchaseToken()));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }


    public static TrustBasketId checkCreateBasketCall(Iterator<ServeEvent> eventsIter, Long tokenId, CreateBasketParams params) {
        ServeEvent event = eventsIter.next();
        assertEquals(CREATE_BASKET_STUB, event.getStubMapping().getName());

        checkHeader(event, UID_HEADER, params.getUid());
        checkHeader(event, USER_IP_HEADER, params.getUserIp());
        checkHeader(event, YANDEX_UID_HEADER, params.getYandexUid());
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));


        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, params.toJsonMatcher());

        //получаем сформированный purchase token
        String purchaseToken = getResponseBodyAsJson(event).get("purchase_token").getAsString();
        TrustBasketId trustBasketId = new TrustBasketId(purchaseToken, tokenId);

        //см. тудушку в TrustRestAPI#createBasket после того как в ответе создания будет возраться оба поля, второго
        // вызова не будет
        checkCheckBasketCall(eventsIter, trustBasketId);
        return trustBasketId;
    }

    public static void checkPayBasketCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketId basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(PAY_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketKey.getServiceTokenId()));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/start"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkUpdateBasketCalls(Iterator<ServeEvent> eventsIter, UpdateBasketParams params) {
        params.getLinesToUpdate().stream().sorted(comparing(UpdateBasketParams.UpdateLineParams::getOrderId))
                .forEach(param -> {
                            if (param.getAction() == UpdateBasketParams.UpdateAction.CANCEL) {
                                checkCancelBasketLineCall(eventsIter, param, params.getBasketKey(), params.getUid(),
                                        params.getUserIp());
                            } else if (param.getAction() == UpdateBasketParams.UpdateAction.UPDATE) {
                                checkUpdateBasketLineCall(eventsIter, param, params.getBasketKey(), params.getUid(),
                                        params.getUserIp());
                            } else {
                                throw new IllegalArgumentException(Objects.toString(param.getAction()));
                            }
                        }
                );
    }

    public static void checkBasketClearCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketId basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(CLEAR_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketKey.getServiceTokenId()));


        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/clear"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkBasketCancelCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketId basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(UNHOLD_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketKey.getServiceTokenId()));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/unhold"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static String checkCreateRefundCall(Iterator<ServeEvent> eventsIter, CreateRefundParams config) {
        return checkCreateRefundCall(eventsIter.next(), config);
    }

    public static String checkCreateRefundCall(ServeEvent event, CreateRefundParams params) {
        assertEquals(CREATE_REFUND_STUB, event.getStubMapping().getName());
        if (params != null) {
            checkHeader(event, USER_IP_HEADER, params.getUserIp());
            checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(params.getBasketKey().getServiceTokenId()));

            JsonObject body = getRequestBodyAsJson(event);
            assertThat(body, params.toJsonMatcher());
        }

        //получаем айдишку рефандам
        return getResponseBodyAsJson(event).get("trust_refund_id").getAsString();
    }

    public static void checkDoRefundCall(Iterator<ServeEvent> eventsIter, Long tokenId, String refundId, Long uid) {
        ServeEvent event = eventsIter.next();
        assertEquals(DO_REFUND_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(tokenId));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/refunds/" + refundId + "/start"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkMarkupBasketCall(Iterator<ServeEvent> eventsIter, MarkupBasketParams params) {
        ServeEvent event = eventsIter.next();
        assertEquals(MARKUP_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(params.getTokenId()));
        checkHeader(event, UID_HEADER, params.getUid());
        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        String actualPurchaseToken = StringUtils.substringAfterLast(StringUtils.removeEnd(path, "/markup"), "/");
        assertThat(actualPurchaseToken, params.getPurchaseToken());

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, params.toJsonMatcher());
    }


    public static Iterator<ServeEvent> skipCheckBasketCalls(Iterable<ServeEvent> calls) throws Exception {
        return StreamSupport.stream(calls.spliterator(), false)
                .filter(serveEvent -> !TrustMockConfigurer.CHECK_BASKET_STUB.equals(serveEvent.getStubMapping()
                        .getName())).iterator();
    }

    private static JsonObject getRequestBodyAsJson(ServeEvent event) {
        JsonParser parser = new JsonParser();
        return parser.parse(event.getRequest().getBodyAsString()).getAsJsonObject();
    }

    private static JsonObject getResponseBodyAsJson(ServeEvent event) {
        JsonParser parser = new JsonParser();
        return parser.parse(event.getResponse().getBodyAsString()).getAsJsonObject();
    }

    private static void checkHeader(ServeEvent event, String headerName, Object expectedValue) {
        if (expectedValue == null) {
            assertThat(event.getRequest().getHeader(headerName), isEmptyOrNullString());
        } else {
            assertThat(event.getRequest().getHeader(headerName), equalTo(expectedValue.toString()));
        }
    }

    private static void checkUpdateBasketLineCall(Iterator<ServeEvent> eventsIter,
                                                  UpdateBasketParams.UpdateLineParams param,
                                                  TrustBasketId basketKey, Long uid, String userIp) {
        ServeEvent event = eventsIter.next();
        assertEquals(UPDATE_BASKET_LINE_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, userIp);
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketKey.getServiceTokenId()));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/orders/" + param.getOrderId() +
                "/resize"));

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, param.toResizeJsonMatcher());
    }

    private static void checkCancelBasketLineCall(Iterator<ServeEvent> eventsIter,
                                                  UpdateBasketParams.UpdateLineParams param,
                                                  TrustBasketId basketKey, Long uid, String userIp) {
        ServeEvent event = eventsIter.next();
        assertEquals(CANCEL_BASKET_LINE_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, userIp);
        checkHeader(event, SERVICE_TOKEN_HEADER, tokensMap.get(basketKey.getServiceTokenId()));

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/orders/" + param.getOrderId() +
                "/unhold"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }


    public static final Long BLUE_PAY_TOKEN_ID = 610L;
    public static final Long BLUE_SUBSIDY_TOKEN_ID = 609L;
    public static final Long COMPENSATION_TOKEN_ID = 613L;
    public static final Long RED_SUBSIDY_TOKEN_ID = 615L;
    public static final Long RED_PAY_TOKEN_ID = 620L;
    public static final Long EXTERNAL_PAYMENT_TOKEN_ID = 633L;


    private static final Map<Long, String> tokensMap = ImmutableMap.<Long, String>builder()
            .put(BLUE_PAY_TOKEN_ID, "blue_market_payments_5fac16d65c83b948a5b10577f373ea7c")
            .put(BLUE_SUBSIDY_TOKEN_ID, "blue_market_subsidy_0b634330e3697ad34d74f0fe5af88230")
            .put(COMPENSATION_TOKEN_ID, "blue_market_refunds_01ffa15ed858f597e4295447aacfd8b5")
            .put(RED_SUBSIDY_TOKEN_ID, "red_market_subsidy_ae74a5673d21c22e0c02fc09d2ea2909")
            .put(RED_PAY_TOKEN_ID, "red_market_balance_9220756d7eb772645679f907ba2500a3")
            .put(EXTERNAL_PAYMENT_TOKEN_ID, "blue_market_certificate_25e71234d955ef39a350399da315b232")
            .build();
}
