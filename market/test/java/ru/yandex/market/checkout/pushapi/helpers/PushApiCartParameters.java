package ru.yandex.market.checkout.pushapi.helpers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import ru.yandex.market.checkout.checkouter.order.ApiSettings;
import ru.yandex.market.checkout.checkouter.order.Context;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.util.OfferItemUtils;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.providers.PushApiCartProvider;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.test.providers.DeliveryProvider;
import ru.yandex.market.common.report.model.FeedOfferId;

import static java.util.stream.Collectors.groupingBy;

public class PushApiCartParameters {

    public static final long DEFAULT_SHOP_ID = 242102L;
    public static final boolean DEFAULT_SANDBOX = false;

    private final Cart request;
    private long shopId;
    private boolean sandbox = DEFAULT_SANDBOX;
    private long uid = 0;
    private Context context;
    private ApiSettings apiSettings;
    private String actionId;
    private CartResponse cartResponse;

    private boolean partnerInterface;
    private DataType dataType = DataType.JSON;

    public PushApiCartParameters() {
        this(PushApiCartProvider.buildCartRequest());
    }

    public PushApiCartParameters(Cart request) {
        this.request = request;
        this.shopId = DEFAULT_SHOP_ID;
    }

    public Cart getRequest() {
        return request;
    }

    public long getShopId() {
        return shopId;
    }

    public void setShopId(long shopId) {
        this.shopId = shopId;
    }

    public boolean isSandbox() {
        return sandbox;
    }

    public void setSandbox(boolean sandbox) {
        this.sandbox = sandbox;
    }

    public long getUid() {
        return uid;
    }

    public void setUid(long uid) {
        this.uid = uid;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public ApiSettings getApiSettings() {
        return apiSettings;
    }

    public void setApiSettings(ApiSettings apiSettings) {
        this.apiSettings = apiSettings;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(String actionId) {
        this.actionId = actionId;
    }

    public boolean isPartnerInterface() {
        return partnerInterface;
    }

    public void setPartnerInterface(boolean partnerInterface) {
        this.partnerInterface = partnerInterface;
    }

    public CartResponse getShopCartResponse() {
        return cartResponse == null ?
                mapCartRequestToResponse(request, dataType) : cartResponse;
    }

    public void setShopCartResponse(CartResponse cartResponse) {
        this.cartResponse = cartResponse;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    @Override
    public String toString() {
        return "PushApiCartParameters{" +
                "request=" + request +
                ", shopId=" + shopId +
                ", sandbox=" + sandbox +
                ", uid=" + uid +
                ", context=" + context +
                ", apiSettings=" + apiSettings +
                ", actionId='" + actionId + '\'' +
                ", partnerInterface='" + partnerInterface + '\'' +
                '}';
    }

    public static CartResponse mapCartRequestToResponse(Cart request, DataType dataType) {
        List<OrderItem> items = mapRequestItems(request, dataType);
        List<DeliveryResponse> deliveryOptions = buildDeliveryOptions();

        CartResponse cartResponse = new CartResponse(
                items,
                deliveryOptions,
                collectPaymentOptions(deliveryOptions));
        cartResponse.setShopAdmin(false);
        return cartResponse;
    }

    private static List<PaymentMethod> collectPaymentOptions(List<DeliveryResponse> deliveryOptions) {
        if (deliveryOptions == null) {
            return null;
        }

        return deliveryOptions.stream()
                .flatMap(dr -> dr.getPaymentOptions().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    private static List<OrderItem> mapRequestItems(Cart request, DataType dataType) {
        if (DataType.XML == dataType) {
            //shop admin stub
            return request.getItems().stream()
                    .map(oi -> {
                        OrderItem copy = new OrderItem(new FeedOfferId(oi.getOfferId(), oi.getFeedId()), null,
                                oi.getCount());
                        copy.setPrice(oi.getBuyerPrice());
                        copy.setQuantPrice(oi.getBuyerPrice());
                        return copy;
                    })
                    .collect(Collectors.toUnmodifiableList());
        } else {
            //shops. join offers by feed offer
            final Map<FeedOfferId, Optional<OrderItem>> cartItemMap = request.getItems().stream()
                    .map(oi -> {
                        OrderItem copy = new OrderItem(new FeedOfferId(oi.getOfferId(), oi.getFeedId()), null,
                                oi.getCount());
                        copy.setPrice(oi.getBuyerPrice());
                        copy.setQuantPrice(oi.getBuyerPrice());
                        return copy;
                    })
                    .collect(groupingBy(item -> FeedOfferId.from(item.getFeedId(), item.getOfferId()),
                            LinkedHashMap::new,
                            Collectors.reducing(PushApiCartParameters::join)));

            return cartItemMap.values().stream()
                    .map(Optional::get)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    private static OrderItem join(
            OrderItem i1,
            OrderItem i2
    ) {
        OrderItem copy = OfferItemUtils.deepCopy(i1);
        copy.setBundleId(null);
        int count = i1.getCount() + i2.getCount();
        copy.setCount(count);
        copy.setValidIntQuantity(count);
        return copy;
    }

    private static List<DeliveryResponse> buildDeliveryOptions() {
        List<DeliveryResponse> pushApiDeliveryResponses = new ArrayList<>();
        pushApiDeliveryResponses.add(DeliveryProvider.shopSelfDelivery().buildResponse(DeliveryResponse::new));
        pushApiDeliveryResponses.add(DeliveryProvider.shopSelfPickupDeliveryByOutletCode()
                .buildResponse(DeliveryResponse::new));
        pushApiDeliveryResponses.add(DeliveryProvider.shopSelfPostDelivery().buildResponse(DeliveryResponse::new));

        pushApiDeliveryResponses.forEach(dr -> {
            dr.setRegionId(null);
            dr.setAddress(null);
            dr.setBuyerAddress(null);
            dr.setShopAddress(null);
            dr.setVat(null);
        });

        return pushApiDeliveryResponses;
    }
}
