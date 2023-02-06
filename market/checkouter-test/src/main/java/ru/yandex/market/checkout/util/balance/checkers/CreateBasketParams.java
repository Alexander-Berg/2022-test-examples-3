package ru.yandex.market.checkout.util.balance.checkers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.hamcrest.Matcher;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.balance.trust.model.BasketMarkup;
import ru.yandex.market.checkout.checkouter.balance.trust.model.FiscalAgentType;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.pay.PaymentTestHelper;

import static com.google.common.collect.Streams.stream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.market.checkout.checkouter.order.OrderTypeUtils.isFulfilment;
import static ru.yandex.market.checkout.checkouter.order.OrderTypeUtils.isMarketDelivery;

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
    private final Collection<BalanceOrderToCreate> orders = new ArrayList<>();
    private Map<String, String> spasiboOrderMap;
    private Integer fiscalForce;
    private String supplierInn = PaymentTestHelper.DEFAULT_SUPPLIER_INN;
    private String deliveryInn;
    private boolean deliveryInnWasSet = false;
    private String fiscalAgentType;
    private BasketMarkup basketMarkup;

    private static String getDeliveryInn(Order order) {
        if (!isFulfilment(order) && !isMarketDelivery(order)) {
            return PaymentTestHelper.DEFAULT_SUPPLIER_INN;
        }

        return PaymentTestHelper.DEFAULT_MARKET_INN;
    }

    public static CreateBasketParams createBasket() {
        return new CreateBasketParams();
    }

    public CreateBasketParams withOrdersByItemsAndDelivery(Order order, boolean fiscalAgentTypeEnabled) {
        order.getItems()
                .forEach(item -> {
                    withOrder(
                            item.getBalanceOrderId(),
                            item.getQuantityIfExistsOrCount(),
                            item.getQuantPriceIfExistsOrBuyerPrice(),
                            item.getOfferName(),
                            item.getVat().getTrustId(),
                            supplierInn,
                            fiscalAgentTypeEnabled ?
                                    FiscalAgentType.fromSupplierType(item.getSupplierType()).getTrustAgentType() :
                                    null
                    );
                    item.getServices()
                            .forEach(itemService ->
                                    withOrder(
                                            itemService.getBalanceOrderId(),
                                            BigDecimal.valueOf(itemService.getCount()),
                                            itemService.getPrice(),
                                            itemService.getTitle(),
                                            itemService.getVat().getTrustId(),
                                            itemService.getInn(),
                                            fiscalAgentTypeEnabled ? "agent" : null
                                    )
                            );
                });

        if (!order.getDelivery().isFreeWithLift()) {
            Delivery delivery = order.getDelivery();
            withOrder(
                    delivery.getBalanceOrderId(),
                    BigDecimal.ONE,
                    delivery.getBuyerPriceWithLift(),
                    "Доставка",
                    delivery.getVat().getTrustId(),
                    deliveryInnWasSet ? deliveryInn : getDeliveryInn(order),
                    fiscalAgentTypeEnabled ? "none_agent" : null);
        }

        return this;
    }

    public Long getUid() {
        return uid;
    }

    public String getUserIp() {
        return userIp;
    }

    public String getYandexUid() {
        return yandexUid;
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

    public CreateBasketParams withReturnPath(String returnPath) {
        this.returnPath = returnPath;
        return this;
    }

    public CreateBasketParams withUserIp(String userIp) {
        this.userIp = userIp;
        return this;
    }

    public CreateBasketParams withOrder(String orderId, BigDecimal quantity, BigDecimal price) {
        orders.add(new BalanceOrderToCreate(orderId, quantity, price));
        return this;
    }

    public CreateBasketParams withBasketMarkup(BasketMarkup basketMarkup) {
        this.basketMarkup = basketMarkup;
        return this;
    }

    public CreateBasketParams withOrder(
            String orderId,
            BigDecimal quantity,
            BigDecimal price,
            String fiscalTitle,
            String fiscalNds,
            String fiscalInn,
            String fiscalAgentType
    ) {
        orders.add(new BalanceOrderToCreate(orderId, quantity, price, fiscalTitle, fiscalNds, fiscalInn,
                fiscalAgentType));
        return this;
    }

    public CreateBasketParams withOrder(BalanceOrderToCreate order) {
        orders.add(order);
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

    public CreateBasketParams withFiscalForce(int fiscalForce) {
        this.fiscalForce = fiscalForce;
        return this;
    }

    public CreateBasketParams withSupplierInn(final String supplierInn) {
        this.supplierInn = supplierInn;
        return this;
    }

    public CreateBasketParams withDeliveryInn(final String deliveryInn) {
        this.deliveryInn = deliveryInn;
        this.deliveryInnWasSet = true;
        return this;
    }

    public CreateBasketParams withFiscalAgentType(String fiscalAgentType) {
        this.fiscalAgentType = fiscalAgentType;
        return this;
    }

    @Override
    public String toString() {
        return "CreateBasketParams{" +
                "yandexUid='" + yandexUid + '\'' +
                ", uid=" + uid +
                ", userIp='" + userIp + '\'' +
                ", payMethodId='" + payMethodId + '\'' +
                ", backUrl=" + backUrl +
                ", currency=" + currency +
                ", userEmail='" + userEmail + '\'' +
                ", paymentTimeout=" + paymentTimeout +
                ", returnPath='" + returnPath + '\'' +
                ", developerPayload=" + developerPayload +
                ", passParams=" + passParams +
                ", orders=" + orders +
                ", spasiboOrderMap=" + spasiboOrderMap +
                ", fiscalForce=" + fiscalForce +
                ", supplierInn='" + supplierInn + '\'' +
                ", deliveryInn='" + deliveryInn + '\'' +
                ", deliveryInnWasSet=" + deliveryInnWasSet +
                ", fiscalAgentType='" + fiscalAgentType + '\'' +
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
                    assertThat("orders array size", jsonOrders.size(), equalTo(orders.size()));
                    List<JsonElement> notMatched = stream(jsonOrders.iterator()).filter(
                            jsonOrder -> !anyOf(orders.stream().map(BalanceOrderToCreate::toJsonMatcher)
                                    .collect(Collectors.toList())).matches(jsonOrder)
                    ).collect(Collectors.toList());

                    if (!notMatched.isEmpty()) {
                        return false;
                    }
                }
                JsonObject spasiboMapElement = object.getAsJsonObject("spasibo_order_map");
                if (spasiboMapElement == null || !spasiboMapElement.isJsonObject()) {
                    if (spasiboOrderMap != null && !spasiboOrderMap.isEmpty()) {
                        return false;
                    }
                } else {
                    assertThat(spasiboMapElement.size(),
                            equalTo(Optional.ofNullable(spasiboOrderMap).map(Map::size).orElse(0)));
                    return true;
                }

                return matchPropertiesSize(object,
                        uid,
                        backUrl,
                        currency,
                        paymentTimeout,
                        userEmail,
                        payMethodId,
                        returnPath,
                        developerPayload,
                        passParams,
                        orders,
                        spasiboOrderMap,
                        fiscalForce,
                        fiscalAgentType
                )
                        && matchValue(object, "uid", uid)
                        && matchValue(object, "back_url", backUrl)
                        && matchValue(object, "currency", currency == null ? null : currency.name())
                        && matchValue(object, "payment_timeout", paymentTimeout)
                        && matchValue(object, "user_email", userEmail)
                        && matchValue(object, "paymethod_id", payMethodId)
                        && matchValue(object, "return_path", returnPath)
                        && matchValue(object, "developer_payload", developerPayload)
                        && matchValue(object, "pass_params", passParams)
                        && matchValue(object, "fiscal_force", fiscalForce)
                        && matchValue(object, "fiscal_agent_type", fiscalAgentType);
            }
        };
    }

    static class BalanceOrderToCreate {

        private final String orderId;
        private final BigDecimal quantity;
        private final BigDecimal price;

        private final String fiscalTitle;
        private final String fiscalNds;
        private final String fiscalInn;
        private final String fiscalAgentType;

        BalanceOrderToCreate(String orderId, BigDecimal quantity, BigDecimal price) {
            this(orderId, quantity, price, null, null, null, null);
        }

        BalanceOrderToCreate(
                String orderId,
                BigDecimal quantity,
                BigDecimal price,
                String fiscalTitle,
                String fiscalNds,
                String fiscalInn,
                String fiscalAgentType
        ) {
            this.orderId = orderId;
            this.quantity = quantity;
            this.price = price;
            this.fiscalTitle = fiscalTitle;
            this.fiscalNds = fiscalNds;
            this.fiscalInn = fiscalInn;
            this.fiscalAgentType = fiscalAgentType;
        }

        @Override
        public String toString() {
            return "BalanceOrderToCreate{" +
                    "orderId='" + orderId + '\'' +
                    ", quantity=" + quantity +
                    ", price=" + price +
                    ", fiscalTitle='" + fiscalTitle + '\'' +
                    ", fiscalNds='" + fiscalNds + '\'' +
                    ", fiscalInn='" + fiscalInn + '\'' +
                    ", fiscalAgentType='" + fiscalAgentType + '\'' +
                    '}';
        }

        public Matcher<JsonElement> toJsonMatcher() {
            return new AbstractMatcher(this.toString()) {
                @Override
                protected boolean matchObjectFields(JsonObject object) {
                    return matchPropertiesSize(object, orderId, quantity, price, fiscalInn, fiscalNds, fiscalTitle,
                            fiscalAgentType)
                            && matchValue(object, "order_id", orderId)
                            && matchValue(object, "qty", quantity)
                            && matchValue(object, "price", price)
                            && matchValue(object, "fiscal_title", fiscalTitle)
                            && matchValue(object, "fiscal_nds", fiscalNds)
                            && matchValue(object, "fiscal_inn", fiscalInn)
                            && matchValue(object, "fiscal_agent_type", fiscalAgentType);
                }
            };
        }
    }
}
