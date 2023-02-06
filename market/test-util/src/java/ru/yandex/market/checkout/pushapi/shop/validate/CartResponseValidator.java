package ru.yandex.market.checkout.pushapi.shop.validate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.common.pay.FinancialValidator;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.checkout.pushapi.client.util.DateUtilBean;
import ru.yandex.market.common.report.model.FeedOfferId;

import java.util.*;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.*;


@Component
public class CartResponseValidator {

    public static final int MAXIMUM_LENGTH_OF_CLIENT_STRINGS = 50;
    private DateUtilBean dateUtil;
    private FinancialValidator financialValidator;

    @Autowired
    public void setDateUtil(DateUtilBean dateUtil) {
        this.dateUtil = dateUtil;
    }

    @Autowired
    public void setFinancialValidator(FinancialValidator financialValidator) {
        this.financialValidator = financialValidator;
    }

    public void validate(Cart cart, CartResponse cartResponse) throws ValidationException {
        notNull(cartResponse, "cartResponse is null");
        notNull(cartResponse.getItems(), "items is null");

        //cart must have items
        if (cartResponse.getItems().isEmpty()) {
            return;
        }

        //if all items have {deliver:false}, cart may have no delivery options
        boolean noItemsToDeliver = true;
        for (OrderItem orderItem : cartResponse.getItems()) {
            if (orderItem.getDelivery() != null && orderItem.getDelivery())
                noItemsToDeliver = false;
        }

        if (!noItemsToDeliver) {
            notNull(cartResponse.getDeliveryOptions(), "delivery options is null");
            nonEmpty(cartResponse.getDeliveryOptions(), "delivery options is empty but there are items to be delivered");
        }

        //check delivery options
        boolean allDeliveryOptionsHavePaymentMethods = false;
        if (cartResponse.getDeliveryOptions() != null) {
            allDeliveryOptionsHavePaymentMethods = true;
            for (DeliveryResponse deliveryResponse : cartResponse.getDeliveryOptions())
                if (deliveryResponse.getPaymentOptions().isEmpty())
                    allDeliveryOptionsHavePaymentMethods = false;

            //no duplicate delivery options allowed
            final List<DeliveryResponse> deliveryOptions = cartResponse.getDeliveryOptions();
            final Set<DeliveryResponse> uniqueDeliveryOptions = new HashSet<>(deliveryOptions);
            eq(deliveryOptions.size(), uniqueDeliveryOptions.size(), "delivery options contains duplicates");

            //delivery options checks
            for (DeliveryResponse deliveryResponse : deliveryOptions) {
                checkDeliveryResponses(deliveryResponse);
            }
        }

        //if all delivery options have payment methods, cart may have no default payment method
        //if all items have {deliver:false}, cart may have no payment options at all
        if (!allDeliveryOptionsHavePaymentMethods && !noItemsToDeliver) {
            notNull(cartResponse.getPaymentMethods(), "cart payment methods is null and not all delivery options have payment methods");
            nonEmpty(cartResponse.getPaymentMethods(), "cart payment methods is empty and not all delivery options have payment methods");
        }

        //check cart payment options if present
        if (cartResponse.getPaymentMethods() != null) {
            for (PaymentMethod paymentMethod : cartResponse.getPaymentMethods()) {
                checkPaymentMethod(paymentMethod, "cart");
            }
        }

        //check items
        for (OrderItem item : cartResponse.getItems()) {
            notNull(item.getFeedId(), "item feedId is null");
            notNull(item.getOfferId(), "item offerId is null");
            notNull(item.getCount(), "item count is null");
            isNull(item.getCategoryId(), "item category is not null");
            isNull(item.getFeedCategoryId(), "item feed category is not null");
            if(item.getCount() > 0)
                notNull(item.getPrice(), "item price is null");

            //validate price only if count > 0
            if(item.getCount() > 0) {
                notNull(item.getPrice(), "item price is null");
                positive(item.getPrice(), "item price is not positive: " + item.getPrice());
                financialValidator.validatePrice(item.getPrice(), "item price");
            }

            nonEmpty(item.getOfferId(), "item offerId is empty");

            positive(item.getFeedId(), "item feedId is not positive: " + item.getFeedId());
            nonNegative(item.getCount(), "item count is negative: " + item.getCount());
        }

        notLessThan(cart.getItems().size(), cartResponse.getItems().size(), "number of items in cartResponse is greater than number of items in cartRequest");

        Map<FeedOfferId, OrderItem> cartItemsById = new HashMap<>();
        for (OrderItem cartItem : cart.getItems()) {
            cartItemsById.put(cartItem.getFeedOfferId(), cartItem);
        }

        for (OrderItem cartResponseItem : cartResponse.getItems()) {
            isTrue(
                    cartItemsById.containsKey(cartResponseItem.getFeedOfferId()),
                    "cartResponse contains extra item with feedId=" + cartResponseItem.getFeedId()
                            + " and offerId=" + cartResponseItem.getOfferId()
            );
        }
    }

    private void checkDeliveryResponses(DeliveryResponse delivery) {
        notNull(delivery.getType(), "delivery type is null");
        notNull(delivery.getServiceName(), "delivery serviceName is null");
        notNull(delivery.getPrice(), "delivery price is null");

        if (delivery.getType() == DeliveryType.PICKUP) {
            notNull(delivery.getOutletIds(), "delivery type=PICKUP and outlets is null");
            nonEmpty(delivery.getOutletIds(), "delivery outlets is empty");

            for (Long outletId : delivery.getOutletIds()) {
                notNull(outletId, "delivery outletId is null");
                positive(outletId, "delivery outletId is not positive: " + outletId);
            }
        } else {
            isNull(delivery.getOutletIds(), "delivery type=DELIVERY and outlets is not null");
        }

        maximumLength(
                delivery.getServiceName(), MAXIMUM_LENGTH_OF_CLIENT_STRINGS,
                "delivery serviceName is longer than " + MAXIMUM_LENGTH_OF_CLIENT_STRINGS + " symbols"
        );

        if (delivery.getId() != null) {
            maximumLength(
                    delivery.getId(), MAXIMUM_LENGTH_OF_CLIENT_STRINGS,
                    "delivery id is longer than " + MAXIMUM_LENGTH_OF_CLIENT_STRINGS + " symbols"
            );
        }

        isNull(delivery.getAddress(), "delivery address is not null: " + delivery.getAddress());
        isNull(delivery.getRegionId(), "delivery regionId is not null: " + delivery.getRegionId());

        nonNegative(delivery.getPrice(), "delivery price is negative: " + delivery.getPrice());
        financialValidator.validatePrice(delivery.getPrice(), "delivery price");

        //check delivery dates
        notNull(delivery.getDeliveryDates(), "deliveryDates is null");
        notNull(delivery.getDeliveryDates().getFromDate(), "fromDate in deliveryDates is null");
        final DeliveryDates dates = delivery.getDeliveryDates();
        if (dates != null && dates.getFromDate() != null) {
            final Date fromDate = dates.getFromDate();
            isTrue(
                    !fromDate.before(dateUtil.getToday()),
                    "from-date should not be before current day. From-date=" + fromDate
            );
            if (dates.getToDate() != null) {
                final Date toDate = dates.getToDate();
                isTrue(
                        !fromDate.after(toDate),
                        "from-date should not be after to-date. From-date=" + fromDate + ". To-date=" + toDate
                );
            }
        }

        //check delivery payment options if present
        for (PaymentMethod paymentMethod : delivery.getPaymentOptions()) {
            checkPaymentMethod(paymentMethod, "delivery");
        }
    }

    private void checkPaymentMethod(PaymentMethod paymentMethod, String entity) {
        //YANDEX_MONEY and BANK_CARD are deprecated payment methods. Probably no longer used.
        isTrue(paymentMethod != PaymentMethod.YANDEX_MONEY, entity + " payment-method YANDEX_MONEY isn't allowed");
        isTrue(paymentMethod != PaymentMethod.BANK_CARD, entity + " payment-method BANK_CARD isn't allowed");
        notNull(paymentMethod, entity + " payment-method is null");
    }

}
