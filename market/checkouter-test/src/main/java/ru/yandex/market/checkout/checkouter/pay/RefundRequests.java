package ru.yandex.market.checkout.checkouter.pay;

import java.math.BigDecimal;
import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.shaded.com.google.common.base.Throwables;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.checkout.checkouter.client.CheckoutCommonParams;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.util.ClientHelper;

import static java.lang.String.format;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.checkout.util.GenericMockHelper.withRefereeRole;
import static ru.yandex.market.checkout.util.GenericMockHelper.withShopRole;
import static ru.yandex.market.checkout.util.GenericMockHelper.withUserRole;

/**
 * @author : poluektov
 * date: 11.07.17.
 */
public final class RefundRequests {

    static final long SHOP_UID = 32832382;
    private static final RefundReason DEFAULT_REFUND_REASON = RefundReason.ORDER_CHANGED;
    private static final String ORDER_REFUND_REQUEST_TEMPLATE = "/orders/{orderId}/refund";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RefundRequests() {
    }

    public static MockHttpServletRequestBuilder refundRequest(@Nullable RefundableItems itemsToRefund,
                                                              @Nonnull BigDecimal refundAmount,
                                                              @Nonnull Order order,
                                                              @Nonnull RefundReason refundReason,
                                                              boolean isPartialRefund) {
        final String amount = isPartialRefund ?
                refundAmount.toString() :
                (itemsToRefund != null) ? "" : refundAmount.toString();
        return withShopRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(SHOP_UID))
                .param("reason", refundReason.name())
                .param("amount", amount)
                .content(getRefundRequestBody(itemsToRefund)).contentType(APPLICATION_JSON_UTF8);
    }

    // TODO Нужно перейти на json-сериализацию и отказаться от StringBuilder'а
    @Deprecated
    private static String getRefundRequestBody(RefundableItems itemsToRefund) {
        if (itemsToRefund != null) {
            StringBuilder refundBody = new StringBuilder("{\"items\": [");
            itemsToRefund.getItems().forEach(i -> {
                refundBody.append(format(
                        "{\"itemId\": %s, \"feedId\": %s, \"offerId\": \"%s\", \"count\": %s, \"isDeliveryService\": " +
                                "false, \"quantity\": %s},",
                        i.getId(), i.getFeedId(), i.getOfferId(), i.getRefundableCount(),
                        i.getRefundableQuantityIfExistsOrRefundableCount()
                ));
            });
            itemsToRefund.getItemServices().forEach(s -> {
                refundBody.append(format(
                        "{\"itemServiceId\": %s, \"count\": %s, \"isDeliveryService\": false},",
                        s.getId(), s.getRefundableCount()
                ));
            });
            if (itemsToRefund.getDelivery() != null && itemsToRefund.getDelivery().isRefundable()) {
                refundBody.append("{\"count\": 1, \"isDeliveryService\": true}");
            }
            if (refundBody.charAt(refundBody.length() - 1) == ',') {
                refundBody.deleteCharAt(refundBody.length() - 1);
            }
            refundBody.append("]}");
            return refundBody.toString();
        } else {
            return "{}";
        }
    }

    private static String getRefundRequestBody(@Nonnull RefundItems refundItems) {
        try {
            return OBJECT_MAPPER.writeValueAsString(refundItems);
        } catch (JsonProcessingException e) {
            throw Throwables.propagate(e);
        }
    }

    private static String getRefundRequestBody(Order order) {
        return getRefundRequestBody(RefundTestHelper.refundableItemsFromOrder(order));
    }

    public static MockHttpServletRequestBuilder refundUnknownOrderId(Order order) {
        return withShopRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId() + 1000), order)
                .param("uid", String.valueOf(SHOP_UID))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .content(getRefundRequestBody(order)).contentType(APPLICATION_JSON_UTF8);
    }

    public static MockHttpServletRequestBuilder refundWrongUid(Order order) {
        return withUserRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(123456))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .content(getRefundRequestBody(order)).contentType(APPLICATION_JSON_UTF8);
    }

    public static MockHttpServletRequestBuilder refundWithoutReason(Order order) {
        return withShopRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(SHOP_UID))
                .content(getRefundRequestBody(order)).contentType(APPLICATION_JSON_UTF8);
    }

    public static MockHttpServletRequestBuilder refundWithUserRole(Order order) {
        return withUserRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(order.getBuyer().getUid()))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .content(getRefundRequestBody(order))
                .contentType(APPLICATION_JSON_UTF8);
    }

    public static MockHttpServletRequestBuilder refundByAmount(Order order) {
        return withShopRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(SHOP_UID))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .param("amount", order.getBuyerTotal().toString());
    }

    public static MockHttpServletRequestBuilder refundByAmountUnderReferee(long orderId,
                                                                           @Nonnull BigDecimal refundAmount) {
        return refundByAmountUnderReferee(orderId, refundAmount, false);
    }

    public static MockHttpServletRequestBuilder refundByAmountUnderReferee(long orderId,
                                                                           @Nonnull BigDecimal refundAmount,
                                                                           boolean sandbox) {
        return withRefereeRole(post(ORDER_REFUND_REQUEST_TEMPLATE, orderId))
                .param("uid", String.valueOf(ClientHelper.REFEREE_UID))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .param("amount", refundAmount.toString())
                .param(CheckoutCommonParams.SANDBOX, String.valueOf(sandbox));
    }

    public static MockHttpServletRequestBuilder refundByAmountUnderReferee(long orderId,
                                                                           @Nonnull BigDecimal refundAmount,
                                                                           @Nullable RefundItem item) {
        MockHttpServletRequestBuilder builder = withRefereeRole(post(ORDER_REFUND_REQUEST_TEMPLATE, orderId))
                .param("uid", String.valueOf(ClientHelper.REFEREE_UID))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .param("amount", refundAmount.toString());
        if (item != null) {
            builder.content(getRefundRequestBody(new RefundItems(Collections.singletonList(item))))
                    .contentType(APPLICATION_JSON_UTF8);
        }
        return builder;
    }

    static MockHttpServletRequestBuilder refundWithEmptyBody(Order order) {
        return withShopRole(post(ORDER_REFUND_REQUEST_TEMPLATE, order.getId()), order)
                .param("uid", String.valueOf(SHOP_UID))
                .param("reason", DEFAULT_REFUND_REASON.name())
                .content("{}").contentType(APPLICATION_JSON_UTF8);
    }
}
