package ru.yandex.market.cashier.mocks.trust.checkers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import ru.yandex.common.util.currency.Currency;

import static com.google.common.collect.Streams.stream;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CreateBasketParams {
    private String yandexUid;
    private Long uid;
    private String userIp;

    private String payMethodId;
    private Matcher<String> backUrl;
    private Currency currency;
    private String userEmail;
    private Matcher<Object> paymentTimeout;
    private String returnPath;
    private Matcher<String> developerPayload;
    private Matcher<String> passParams;
    private Collection<TrustOrderToCreate> orders = new ArrayList<>();
    private Map<String, String> spasiboOrderMap;

//    public CreateBasketParams withOrdersByItemsAndDelivery(Order order) {
//        order.getItems().forEach(i -> withOrder(i.getBalanceOrderId(),
//                i.getCount(),
//                i.getBuyerPrice(),
//                i.getOfferName(),
//                i.getVat().getTrustId(),
//                PaymentTestHelper.DEFAULT_SUPPLIER_INN));
//
//        if (!order.getDelivery().isFree()) {
//            Delivery delivery = order.getDelivery();
//            withOrder(
//                    delivery.getBalanceOrderId(),
//                    1,
//                    delivery.getBuyerPrice(),
//                    "Доставка",
//                    delivery.getVat().getTrustId(),
//                    (order.isFulfilment() || OrderTypeUtils.isCrossdock(order)) && order.getRgb() != Color.RED ?
//                            PaymentTestHelper.DEFAULT_MARKET_INN : PaymentTestHelper.DEFAULT_SUPPLIER_INN);
//        }
//
//        return this;
//    }

    public Long getUid() {
        return uid;
    }

    public String getUserIp() {
        return userIp;
    }

    public String getYandexUid() {
        return yandexUid;
    }

    public static CreateBasketParams createBasket() {
        return new CreateBasketParams();
    }

    public CreateBasketParams withPayMethodId(String method) {
        this.payMethodId = method;
        return this;
    }

    public CreateBasketParams withBackUrl(Matcher<String> backUrl) {
        this.backUrl = backUrl;
        return this;
    }

    public CreateBasketParams withCurrency(Currency currency) {
        this.currency = currency;
        return this;
    }

    public CreateBasketParams withUid(Long uid) {
        this.uid = uid;
        return this;
    }

    public CreateBasketParams withYandexUid(String yandexUid) {
        this.yandexUid = yandexUid;
        return this;
    }

    public CreateBasketParams withDeveloperPayload(Matcher<String> developerPayload) {
        this.developerPayload = developerPayload;
        return this;
    }

    public CreateBasketParams withDeveloperPayload(String developerPayload) {
        this.developerPayload = equalTo(developerPayload);
        return this;
    }

    public CreateBasketParams withPassParams(Matcher<String> passParams) {
        this.passParams = passParams;
        return this;
    }

    public CreateBasketParams withPassParams(String passParams) {
        this.passParams = equalTo(passParams);
        return this;
    }

    public CreateBasketParams withReturnPath(String returnPath) {
        this.returnPath = returnPath;
        return this;
    }

    public CreateBasketParams withUserIp(String userIp) {
        this.userIp = userIp;
        return this;
    }

    public CreateBasketParams withOrder(String orderId, int quantity, BigDecimal price) {
        orders.add(new TrustOrderToCreate(orderId, quantity, price));
        return this;
    }

    public CreateBasketParams withOrder(String orderId, int quantity, BigDecimal price, String fiscalTitle,
                                        String fiscalNds, String fiscalInn) {
        orders.add(new TrustOrderToCreate(orderId, quantity, price, fiscalTitle, fiscalNds, fiscalInn));
        return this;
    }


    public CreateBasketParams withUserEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }

    public CreateBasketParams withPaymentTimeout(Matcher<Object> paymentTimeout) {
        this.paymentTimeout = paymentTimeout;
        return this;
    }

    public CreateBasketParams withSpasiboOrderMap(Map<String, String> spasiboOrderMap) {
        this.spasiboOrderMap = spasiboOrderMap;
        return this;
    }

    static class TrustOrderToCreate {
        private String orderId;
        private int quantity;
        private BigDecimal price;

        private String fiscalTitle;
        private String fiscalNds;
        private String fiscalInn;

        public TrustOrderToCreate(String orderId, int quantity, BigDecimal price) {
            this(orderId, quantity, price, null, null, null);
        }

        public TrustOrderToCreate(String orderId, int quantity, BigDecimal price, String fiscalTitle, String
                fiscalNds, String fiscalInn) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.price = price;
            this.fiscalTitle = fiscalTitle;
            this.fiscalNds = fiscalNds;
            this.fiscalInn = fiscalInn;
        }

        @Override
        public String toString() {
            return "TrustOrderToCreate{" +
                    "orderId='" + orderId + '\'' +
                    ", quantity=" + quantity +
                    ", price=" + price +
                    ", fiscalTitle='" + fiscalTitle + '\'' +
                    ", fiscalNds='" + fiscalNds + '\'' +
                    ", fiscalInn='" + fiscalInn + '\'' +
                    '}';
        }

        public Matcher<JsonElement> toJsonMatcher() {
            return new AbstractMatcher(this.toString()) {
                @Override
                protected boolean matchObjectFields(JsonObject object) {
                    return matchPropertiesSize(object, orderId, quantity, price, fiscalInn, fiscalNds, fiscalTitle)
                            && matchValue(object, "order_id", orderId)
                            && matchValue(object, "qty", quantity)
                            && matchValue(object, "price", price)
                            && matchValue(object, "fiscal_title", fiscalTitle)
                            && matchValue(object, "fiscal_nds", fiscalNds)
                            && matchValue(object, "fiscal_inn", fiscalInn);
                }
            };
        }
    }

    @Override
    public String toString() {
        return "CreateBasketParams{" +
                "payMethodId='" + payMethodId + '\'' +
                ", backUrl=" + backUrl +
                ", currency=" + currency +
                ", yandexUid='" + yandexUid + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", uid=" + uid +
                ", paymentTimeout=" + paymentTimeout +
                ", returnPath='" + returnPath + '\'' +
                ", userIp='" + userIp + '\'' +
                ", developerPayload='" + developerPayload + '\'' +
                ", passParams='" + passParams + '\'' +
                ", orders=" + orders +
                '}';
    }

    public Matcher<JsonElement> toJsonMatcher() {
        return new AbstractMatcher(this.toString()) {
            @Override
            protected boolean matchObjectFields(JsonObject object) {
                JsonElement ordersElement = object.get("orders");
                if ((ordersElement == null || !ordersElement.isJsonArray() || ordersElement.getAsJsonArray().size()
                        == 0)) {
                    if (!orders.isEmpty()) {
                        return false;
                    }
                } else {
                    JsonArray jsonOrders = ordersElement.getAsJsonArray();
                    assertThat(jsonOrders.size(), equalTo(orders.size()));
                    if (!stream(jsonOrders.iterator()).allMatch(
                            jsonOrder -> anyOf(orders.stream().map(TrustOrderToCreate::toJsonMatcher).collect
                                    (Collectors.toList())).matches(jsonOrder)
                    )) {
                        return false;
                    }
                }
                JsonObject spasiboMapElement = object.getAsJsonObject("spasibo_order_map");
                if ((spasiboMapElement == null || !spasiboMapElement.isJsonObject())) {
                    if ((spasiboOrderMap != null) && !spasiboOrderMap.isEmpty()) {
                        return false;
                    }
                } else {
                    assertThat(spasiboMapElement.size(), equalTo(spasiboOrderMap.size()));
                    return true;
                }

                return matchPropertiesSize(object, uid, backUrl, currency, paymentTimeout, userEmail,
                        payMethodId, returnPath, developerPayload, passParams, orders, spasiboOrderMap)
                        && matchValue(object, "uid", uid)
                        && matchValue(object, "back_url", backUrl)
                        && matchValue(object, "currency", currency == null ? null : currency.name())
                        && matchValue(object, "payment_timeout", paymentTimeout)
                        && matchValue(object, "user_email", userEmail)
                        && matchValue(object, "paymethod_id", payMethodId)
                        && matchValue(object, "return_path", returnPath)
                        && matchValue(object, "developer_payload", developerPayload)
                        && matchValue(object, "pass_params", passParams);
            }
        };
    }
}

