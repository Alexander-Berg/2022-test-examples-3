package ru.yandex.market.checkout.util.serialize;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;

/**
 * Сериализатор для CartResponse для тестов.
 */
public abstract class CartResponseJsonSerializer {
    public CartResponseJsonSerializer() {
        throw new UnsupportedOperationException();
    }

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT = ThreadLocal.withInitial(() -> {
        return new SimpleDateFormat("dd-MM-yyyy");
    });

    public static String serializeJson(CartResponse cartResponse) {
        try {

            JSONObject cart = new JSONObject();
            if (cartResponse.getDeliveryCurrency() != null) {
                cart.put("deliveryCurrency", cartResponse.getDeliveryCurrency().name());
            }
            cart.put("items", serializeItems(cartResponse.getItems()));
            cart.put("deliveryOptions", serializeDeliveryOptions(cartResponse.getDeliveryOptions()));
            if (CollectionUtils.isNotEmpty(cartResponse.getPaymentMethods())) {
                JSONArray paymentMethodsArray = new JSONArray();
                for (PaymentMethod paymentMethod : cartResponse.getPaymentMethods()) {
                    paymentMethodsArray.put(paymentMethod.name());
                }
                cart.put("paymentMethods", paymentMethodsArray);
            }

            JSONObject result = new JSONObject();
            result.put("cart", cart);
            return result.toString();


        } catch (JSONException jsonEx) {
            throw new RuntimeException(jsonEx);
        }
    }

    private static JSONArray serializeItems(List<OrderItem> items) throws JSONException {
        JSONArray result = new JSONArray();

        for (OrderItem oi : items) {
            JSONObject item = new JSONObject();
            if (oi.getId() != null) {
                item.put("id", oi.getId().longValue());
            }
            if (oi.getFeedId() != null) {
                item.put("feedId", oi.getFeedId().longValue());
            }
            item.put("offerId", oi.getOfferId());
            item.put("feedCategoryId", oi.getFeedCategoryId());
            item.put("offerName", oi.getOfferName());
            item.put("price", oi.getPrice());
            item.put("quantPrice", oi.getQuantPrice());
            item.put("count", oi.getCount());
            item.put("quantity", oi.getQuantity());
            item.put("delivery", oi.getDelivery());
            item.put("params", oi.getKind2ParametersAsString());
            if (oi.getVat() != null) {
                item.put("vat", oi.getVat().name());
            }
            item.put("sellerInn", oi.getSellerInn());

            result.put(item);
        }

        return result;
    }

    private static JSONArray serializeDeliveryOptions(List<DeliveryResponse> deliveryOptions) throws JSONException {
        JSONArray result = new JSONArray();

        for (DeliveryResponse deliveryResponse : deliveryOptions) {
            JSONObject delivery = new JSONObject();
            delivery.put("type", deliveryResponse.getType().name());
            delivery.put("price", deliveryResponse.getPrice());
            if (deliveryResponse.getVat() != null) {
                delivery.put("vat", deliveryResponse.getVat().name());
            }
            delivery.put("serviceName", deliveryResponse.getServiceName());
            delivery.put("id", deliveryResponse.getId());
            delivery.put("shopDeliveryId", deliveryResponse.getShopDeliveryId());
            delivery.put("deliveryOptionId", deliveryResponse.getDeliveryOptionId());
            delivery.put("hash", deliveryResponse.getHash());
            delivery.put("deliveryServiceId", deliveryResponse.getDeliveryServiceId());
            delivery.put("paymentAllow", deliveryResponse.isPaymentAllow());

            if (deliveryResponse.getPaymentOptions() != null && !deliveryResponse.getPaymentOptions().isEmpty()) {
                delivery.put(
                        "paymentMethods",
                        deliveryResponse.getPaymentOptions()
                                .stream()
                                .map(Enum::toString)
                                .collect(Collectors.toList()))
                ;
            }

            if (deliveryResponse.getDeliveryDates() != null) {
                delivery.put("dates", serializeDeliveryDates(deliveryResponse.getDeliveryDates(), deliveryResponse));
            }

            Optional<JSONArray> optOutlets = serializeOutlets(deliveryResponse.getOutletIdsSet(),
                    deliveryResponse.getOutletCodes());

            if (optOutlets.isPresent()) {
                delivery.put("outlets", optOutlets.get());
            }
            result.put(delivery);
        }
        return result;
    }

    private static JSONObject serializeDeliveryDates(DeliveryDates deliveryDates,
                                                     DeliveryResponse deliveryResponse) throws JSONException {
        JSONObject dates = new JSONObject();

        if (deliveryDates.getToDate() != null) {
            dates.put("toDate", DATE_FORMAT.get().format(deliveryDates.getToDate()));
        }

        if (deliveryDates.getFromDate() != null) {
            dates.put("fromDate", DATE_FORMAT.get().format(deliveryDates.getFromDate()));
        }

        if (deliveryResponse.getRawDeliveryIntervals() != null &&
                !deliveryResponse.getRawDeliveryIntervals().isEmpty()) {
            var response = deliveryResponse.getRawDeliveryIntervals()
                    .getCollection()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            dates.put(
                    "intervals",
                    response.stream()
                            .map(it -> {
                                var t = new HashMap<>();
                                t.put("date", DATE_FORMAT.get().format(it.getDate()));
                                t.put("fromTime", it.getFromTime());
                                t.put("toTime", it.getToTime());
                                return t;
                            })
                            .collect(Collectors.toList())
            );
        }

        return dates;
    }

    private static Optional<JSONArray> serializeOutlets(Set<Long> outletIds, Set<String> outletCodes) throws JSONException {
        if (CollectionUtils.isEmpty(outletIds) && CollectionUtils.isEmpty(outletCodes)) {
            return Optional.empty();
        }
        JSONArray outlets = new JSONArray();

        if (outletIds != null) {
            for (Long outletId : outletIds) {
                JSONObject outlet = new JSONObject();
                outlet.put("id", outletId);
                outlets.put(outlet);
            }
        }

        if (outletCodes != null) {
            for (String outletCode : outletCodes) {
                JSONObject outlet = new JSONObject();
                outlet.put("code", outletCode);
                outlets.put(outlet);
            }
        }

        return Optional.of(outlets);
    }

}
