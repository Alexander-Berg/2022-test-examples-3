package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import ru.yandex.market.checkout.checkouter.pay.Payment;
import ru.yandex.market.checkout.checkouter.pay.TrustBasketKey;
import ru.yandex.market.checkout.util.balance.OneElementBackIterator;

import static java.util.Comparator.comparing;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CANCEL_BASKET_LINE_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CHECK_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CLEAR_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_ACCOUNT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_CREDIT;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_DELIVERY_CREDIT_RECEIPT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_DELIVERY_RECEIPT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_ORDERS_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_PRODUCT_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.CREATE_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.DO_REFUND_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.GET_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.LIST_WALLET_BALANCE;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.LOAD_PARTNER_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.MARKUP_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.PAYMENT_METHODS_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.PAY_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.TOPUP_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.UNBIND_CARD_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.UNHOLD_BASKET_STUB;
import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.UPDATE_BASKET_LINE_STUB;
import static ru.yandex.market.checkout.util.matching.Matchers.zero;

public final class TrustCallsChecker {

    public static final String UID_HEADER = "X-Uid";
    public static final String YANDEX_UID_HEADER = "X-Yandexuid";
    private static final Logger LOG = LoggerFactory.getLogger(TrustCallsChecker.class);
    private static final String USER_IP_HEADER = "X-User-Ip";
    private static final String SESSION_ID_HEADER = "X-Session-Id";

    private TrustCallsChecker() {
    }

    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter,
                                                          Long uid) {
        checkBatchServiceOrderCreationCall(eventsIter, uid, (List<CreateBalanceOrderParams>) null);
    }

    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter,
                                                          Long uid, List<CreateBalanceOrderParams> orderParams) {
        ServeEvent event = eventsIter.next();
        assertEquals(CREATE_ORDERS_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");

        if (orderParams == null) {
            return;
        }

        JsonObject body = getRequestBodyAsJson(event);
        JsonElement orders = body.remove("orders");
        if (orderParams.isEmpty()) {
            if (orders != null) {
                assertThat(orders.getAsJsonArray().size(), zero());
            }
        } else {
            JsonArray ordersArray = orders.getAsJsonArray();
            assertThat(ordersArray.size(), equalTo(orderParams.size()));

            ordersArray.forEach(jsonOrder -> assertThat(
                    jsonOrder,
                    anyOf(orderParams.stream().map(CreateBalanceOrderParams::toJsonMatcher)
                            .collect(Collectors.toList()))));
        }

        assertThat(body.size(), zero());
    }

    public static void checkBatchServiceOrderCreationCall(Iterator<ServeEvent> eventsIter,
                                                          Long uid, CreateBalanceOrderParams singleOrderParam) {
        checkBatchServiceOrderCreationCall(eventsIter, uid, Collections.singletonList(singleOrderParam));
    }

    public static void checkOptionalCreateServiceProductCall(
            OneElementBackIterator<ServeEvent> eventIterator, CreateProductParams params) {
        ServeEvent event = eventIterator.next();

        // Если вызван другой метод или не совпали поля (может быть такой же но, с другими полями),
        // то вероятно попали в кэш, возвращаем итератор на 1 позицию назад
        if (!event.getStubMapping().getName().equals(CREATE_PRODUCT_STUB)) {
            LOG.warn("Found unexpected call: {}" + event.getStubMapping().getName());
            eventIterator.skipAdvanceOnNext();
            return;
        }

        try {
            if (params != null) {
                params.matches(getRequestBodyAsJson(event));
            }
        } catch (AssertionError e) {
            LOG.warn("Found unexpected call, CreateProduct method call may be cached: {}" + e.getMessage());
            eventIterator.skipAdvanceOnNext();
        }
    }

    public static void checkLoadPartnerCall(Iterator<ServeEvent> eventsIter, Long partnerId) {
        ServeEvent event = eventsIter.next();
        assertEquals(LOAD_PARTNER_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/partners/" + partnerId));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkOptionalLoadPartnerCall(OneElementBackIterator<ServeEvent> eventsIter, Long partnerId) {
        ServeEvent event = eventsIter.next();
        if (!event.getStubMapping().getName().equals(LOAD_PARTNER_STUB)) {
            LOG.warn("Found unexpected call: {}" + event.getStubMapping().getName());
            eventsIter.skipAdvanceOnNext();
        } else {
            assertEquals(LOAD_PARTNER_STUB, event.getStubMapping().getName());

            String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
            assertThat(path, endsWith("/partners/" + partnerId));
            assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
        }
    }

    public static TrustBasketKey checkCheckBasketCall(Iterator<ServeEvent> eventsIter, String purchaseToken) {
        ServeEvent event = eventsIter.next();
        assertEquals(CHECK_BASKET_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + purchaseToken));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());

        JsonObject responseJson = getResponseBodyAsJson(event);
        return new TrustBasketKey(
                responseJson.get("trust_payment_id").getAsString(),
                responseJson.get("purchase_token").getAsString());
    }

    public static TrustBasketKey checkStatusBasketCall(Iterator<ServeEvent> eventsIter, String purchaseToken) {
        ServeEvent event = eventsIter.next();
        assertEquals(GET_BASKET_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payment_status/" + purchaseToken));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());

        JsonObject responseJson = getResponseBodyAsJson(event);
        return new TrustBasketKey(
                responseJson.get("trust_payment_id").getAsString(),
                responseJson.get("purchase_token").getAsString());
    }

    public static TrustBasketKey checkCreateBasketCall(Iterator<ServeEvent> eventsIter, CreateBasketParams params) {
        ServeEvent event = eventsIter.next();

        checkCreateBasketCall(event, params);

        //получаем сформированный purchase token
        JsonObject responseJson = getResponseBodyAsJson(event);

        //см. тудушку в TrustRestAPI#createBasket после того как в ответе создания будут возвращаться оба поля, второго
        // вызова не будет
        return new TrustBasketKey(
                responseJson.get("trust_payment_id").getAsString(),
                responseJson.get("purchase_token").getAsString());
    }

    public static void checkCreateBasketCall(@Nonnull ServeEvent event, CreateBasketParams params) {
        assertEquals(CREATE_BASKET_STUB, event.getStubMapping().getName());

        checkHeader(event, UID_HEADER, params.getUid());
//        checkHeader(event, USER_IP_HEADER, params.getUserIp());
        checkHeader(event, YANDEX_UID_HEADER, params.getYandexUid());

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, params.toJsonMatcher());
    }

    public static TrustBasketKey checkCreateCreditCall(Iterator<ServeEvent> eventsIter, CreateBasketParams params) {
        ServeEvent event = eventsIter.next();

        checkCreateCreditCall(event, params);

        //получаем сформированный purchase token
        String purchaseToken;
        JsonObject responseJson = getResponseBodyAsJson(event);

        return new TrustBasketKey(
                responseJson.get("trust_payment_id").getAsString(),
                responseJson.get("purchase_token").getAsString());
    }

    public static void checkCreateCreditCall(@Nonnull ServeEvent event, CreateBasketParams params) {
        assertEquals(CREATE_CREDIT, event.getStubMapping().getName());

        checkHeader(event, UID_HEADER, params.getUid());
        checkHeader(event, USER_IP_HEADER, params.getUserIp());
        checkHeader(event, YANDEX_UID_HEADER, params.getYandexUid());

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, params.toJsonMatcher());
    }

    public static void checkTopupCall(OneElementBackIterator<ServeEvent> eventIterator, long uid,
                                      String accountPaymethodId, BigDecimal expectedAmount) {
        ServeEvent event = eventIterator.next();
        assertEquals(TOPUP_STUB, event.getStubMapping().getName());

        checkHeader(event, UID_HEADER, uid);
        JsonObject body = getRequestBodyAsJson(event);
        String paymethodId = body.get("paymethod_id").getAsString();
        assertEquals(accountPaymethodId, paymethodId);
        String amount = body.get("amount").getAsString();
        assertThat(expectedAmount, comparesEqualTo(new BigDecimal(amount)));
    }

    public static void checkPayBasketCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketKey basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(PAY_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");

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

    public static void checkBasketClearCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketKey basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(CLEAR_BASKET_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/clear"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkBasketCancelCall(Iterator<ServeEvent> eventsIter, Long uid, TrustBasketKey basketKey) {
        ServeEvent event = eventsIter.next();
        assertEquals(UNHOLD_BASKET_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/unhold"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkListPaymentMethodsCall(Iterator<ServeEvent> eventsIter, String uid, String userIp,
                                                   @Nullable String regionId) {
        ServeEvent event = eventsIter.next();
        assertEquals(PAYMENT_METHODS_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, userIp);

        List<String> regionQueryParam = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build()
                .getQueryParams().get("region_id");
        if (regionId == null) {
            assertNull(regionQueryParam);
        } else {
            assertNotNull(regionQueryParam);
            assertThat(regionQueryParam.size(), is(1));
            assertThat(regionQueryParam.get(0), is(regionId));
        }
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkListWalletBalanceMethodCall(Iterator<ServeEvent> eventsIter) {
        ServeEvent event = eventsIter.next();
        assertEquals(LIST_WALLET_BALANCE, event.getStubMapping().getName());
    }

    public static void checkCreateAccountMethodCall(Iterator<ServeEvent> eventsIter, String uid) {
        ServeEvent event = eventsIter.next();
        assertEquals(CREATE_ACCOUNT_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        JsonObject body = getRequestBodyAsJson(event);
        JsonElement currency = body.get("currency");
        assertThat(currency.getAsString(), is("RUB"));
    }

    public static void checkUnbindCardCall(Iterator<ServeEvent> eventsIter, String sessionId, String userIp, String
            cardId) {
        ServeEvent event = eventsIter.next();
        assertEquals(UNBIND_CARD_STUB, event.getStubMapping().getName());
        checkHeader(event, USER_IP_HEADER, userIp);
        checkHeader(event, SESSION_ID_HEADER, sessionId);

        JsonObject body = getRequestBodyAsJson(event);
        JsonElement actualCardId = body.get("id");
        assertThat(actualCardId.getAsString(), is(cardId));
        assertThat(body.size(), is(1));
    }

    public static String checkCreateRefundCall(Iterator<ServeEvent> eventsIter, Long uid, CreateRefundParams config) {
        return checkCreateRefundCall(findServeEventByStubNappingName(eventsIter, CREATE_REFUND_STUB), uid, config);
    }

    @Nonnull
    private static ServeEvent findServeEventByStubNappingName(
            Iterator<ServeEvent> eventsIter,
            String stubMappingName) {
        if (!(eventsIter instanceof OneElementBackIterator)) {
            return eventsIter.next();
        }
        var oneElementBackIterator = (OneElementBackIterator<ServeEvent>) eventsIter;
        var nextElement = oneElementBackIterator.next();
        oneElementBackIterator.skipAdvanceOnNext();
        if (nextElement.getStubMapping().getName().equals(stubMappingName)) {
            return eventsIter.next();
        }
        return Optional.of(oneElementBackIterator)
                .map(OneElementBackIterator::getSource)
                .flatMap(events -> events.stream()
                        .filter(serveEvent -> serveEvent.getStubMapping().getName().equals(stubMappingName))
                        .findFirst())
                .orElseGet(eventsIter::next);

    }

    public static String checkCreateRefundCall(ServeEvent event, Long uid, CreateRefundParams params) {
        assertEquals(CREATE_REFUND_STUB, event.getStubMapping().getName());
//        checkHeader(event, UID_HEADER, uid);
        if (params != null) {
            checkHeader(event, USER_IP_HEADER, params.getUserIp());

            JsonObject body = getRequestBodyAsJson(event);
            assertThat(body, params.toJsonMatcher());
        }

        //получаем айдишку рефандам
        return getResponseBodyAsJson(event).get("trust_refund_id").getAsString();
    }

    public static void checkDoRefundCall(Iterator<ServeEvent> eventsIter, String refundId, Long uid) {
        ServeEvent event = findServeEventByStubNappingName(eventsIter, DO_REFUND_STUB);
        assertEquals(DO_REFUND_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, "127.0.0.1");

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/refunds/" + refundId + "/start"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void checkMarkupBasketCall(Iterator<ServeEvent> eventsIter, MarkupBasketParams params) {
        ServeEvent event = eventsIter.next();
        assertEquals(MARKUP_BASKET_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, params.getUid());
        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        String actualPurchaseToken = StringUtils.substringAfterLast(StringUtils.removeEnd(path, "/markup"), "/");
        assertThat(actualPurchaseToken, params.getPurchaseToken());

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, params.toJsonMatcher());
    }

    public static void checkCreateDeliveryReceiptCall(ServeEvent event, Payment payment,
                                                      Consumer<JsonObject> validator) {
        assertEquals(CREATE_DELIVERY_RECEIPT_STUB, event.getStubMapping().getName());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        String actualPurchaseToken = StringUtils.substringAfterLast(StringUtils.removeEnd(path, "/deliver"), "/");
        assertEquals(actualPurchaseToken, payment.getBasketKey().getPurchaseToken());

        JsonObject body = new JsonParser().parse(event.getRequest().getBodyAsString()).getAsJsonObject();

        validator.accept(body);
    }

    public static void checkCreateCreditDeliveryReceiptCall(ServeEvent event, Payment payment,
                                                            Consumer<JsonObject> validator) {
        assertEquals(CREATE_DELIVERY_CREDIT_RECEIPT_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, payment.getUid());

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        String actualPurchaseToken = StringUtils.substringAfterLast(StringUtils.removeEnd(path, "/deliver"), "/");
        assertEquals(actualPurchaseToken, payment.getBasketKey().getPurchaseToken());

        JsonObject body = new JsonParser().parse(event.getRequest().getBodyAsString()).getAsJsonObject();

        validator.accept(body);
    }

    public static void checkCashbackBalanceCalls(OneElementBackIterator<ServeEvent> callIter) {
        //Вызывается дважды. На cart и checkout но теперь кэш
        assertEquals(LIST_WALLET_BALANCE, callIter.next().getStubMapping().getName());
//        assertEquals(LIST_WALLET_BALANCE, callIter.next().getStubMapping().getName());
    }

    public static Iterator<ServeEvent> skipCheckBasketCalls(Iterable<ServeEvent> balanceCalls) {
        return StreamSupport.stream(balanceCalls.spliterator(), false)
                .filter(serveEvent -> !CHECK_BASKET_STUB.equals(serveEvent.getStubMapping()
                        .getName())).iterator();
    }

    public static Iterator<ServeEvent> skipStatusBasketCalls(Iterable<ServeEvent> balanceCalls) {
        return StreamSupport.stream(balanceCalls.spliterator(), false)
                .filter(serveEvent -> !GET_BASKET_STUB.equals(serveEvent.getStubMapping()
                        .getName())).iterator();
    }

    public static JsonObject getRequestBodyAsJson(ServeEvent event) {
        JsonParser parser = new JsonParser();
        return parser.parse(event.getRequest().getBodyAsString()).getAsJsonObject();
    }

    public static JsonObject getResponseBodyAsJson(ServeEvent event) {
        JsonParser parser = new JsonParser();
        return parser.parse(event.getResponse().getBodyAsString()).getAsJsonObject();
    }

    public static void checkHeader(ServeEvent event, String headerName, Object expectedValue) {
        if (expectedValue == null) {
            assertThat("header " + headerName,
                    event.getRequest().getHeader(headerName), isEmptyOrNullString());
        } else {
            assertThat("header " + headerName,
                    event.getRequest().getHeader(headerName), equalTo(expectedValue.toString()));
        }
    }

    private static void checkUpdateBasketLineCall(Iterator<ServeEvent> eventsIter, UpdateBasketParams
            .UpdateLineParams param,
                                                  TrustBasketKey basketKey, Long uid, String userIp) {
        ServeEvent event = eventsIter.next();
        assertEquals(UPDATE_BASKET_LINE_STUB, event.getStubMapping().getName());
        checkHeader(event, UID_HEADER, uid);
        checkHeader(event, USER_IP_HEADER, userIp);

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/orders/" + param.getOrderId() +
                "/resize"));

        JsonObject body = getRequestBodyAsJson(event);
        assertThat(body, param.toResizeJsonMatcher());
    }

    private static void checkCancelBasketLineCall(Iterator<ServeEvent> eventsIter,
                                                  UpdateBasketParams.UpdateLineParams param,
                                                  TrustBasketKey basketKey,
                                                  Long uid,
                                                  String userIp) {
        ServeEvent event = eventsIter.next();
        assertEquals(CANCEL_BASKET_LINE_STUB, event.getStubMapping().getName());
        checkHeader(event, USER_IP_HEADER, userIp);

        String path = UriComponentsBuilder.fromHttpUrl(event.getRequest().getAbsoluteUrl()).build().getPath();
        assertThat(path, endsWith("/payments/" + basketKey.getPurchaseToken() + "/orders/" + param.getOrderId() +
                "/unhold"));
        assertThat(event.getRequest().getBodyAsString(), isEmptyOrNullString());
    }

    public static void skipTrustCall(Iterator<ServeEvent> eventIterator, String requestType) {
        ServeEvent event = eventIterator.next();
        assertEquals(requestType, event.getStubMapping().getName());
    }

    public static Iterator<ServeEvent> findEvents(List<ServeEvent> events, String method) {
        return events.stream()
                .filter(e -> method.equals(e.getStubMapping().getName()))
                .iterator();
    }
}
