package ru.yandex.market.checkout.pushapi.error.validate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.common.currency.AllowedCurrencies;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.*;

@Component
public class OrderValidator implements Validator<Order> {

    private AddressValidator addressValidator;

    @Autowired
    public void setAddressValidator(AddressValidator addressValidator) {
        this.addressValidator = addressValidator;
    }

    @Override
    public void validate(Order object) throws ValidationException {
        validate(object, true);
    }

    public void validate(Order object, boolean validateRegionId) throws ValidationException {
        notNull(object, "order is null");
        if(object.getId() != null) {
            positive(object.getId(), "orderId is not positive");
        }
        final Currency currency = object.getCurrency();
        notNull(currency, "order currency is null");
        isTrue(
            AllowedCurrencies.isAllowed(currency),
            "currency " + currency + " is not allowed"
        );


        if(object.getPaymentType() != null) {
            if(object.getPaymentType() == PaymentType.POSTPAID) {
                notNull(object.getPaymentMethod(), "order paymentType is POSTPAID but paymentMethod is null");
            }
            if(object.getPaymentMethod() != null) {
                final PaymentType paymentType = object.getPaymentType();
                eq(
                    object.getPaymentMethod().getPaymentType(), paymentType,
                    "order paymentType is " + paymentType + " but paymentMethods is " + object.getPaymentMethod()
                );
            }
        }
        notNull(object.getItems(), "order items is null");
        nonEmpty(object.getItems(), "order items is empty");

        for(OrderItem offerItem : object.getItems()) {
            notNull(offerItem, "item is null");
            notNull(offerItem.getFeedId(), "item feedId is null");
            positive(offerItem.getFeedId(), "item feedId is not positive");
            notNull(offerItem.getOfferId(), "item offerId is null");
            nonEmpty(offerItem.getOfferId(), "item offerId is empty");
            notNull(offerItem.getFeedCategoryId(), "item feedCategoryId is null");
            nonEmpty(offerItem.getFeedCategoryId(), "item feedCategoryId is empty");
            notNull(offerItem.getOfferName(), "item offerName is null");
            nonEmpty(offerItem.getOfferName(), "item offerName is empty");
            notNull(offerItem.getCount(), "item count is not positive");
            if(offerItem.getCount() > 0) {
                notNull(offerItem.getPrice(), "item price is null");
                positive(offerItem.getPrice(), "item price is not positive");
            }
        }

        final Delivery delivery = object.getDelivery();
        notNull(delivery, "order delivery is null");

        notNull(delivery.getType(), "delivery type is null");
        notNull(delivery.getPrice(), "delivery price is null");
        nonNegative(delivery.getPrice(), "delivery price is negative");
        notNull(delivery.getServiceName(), "delivery serviceName is null");

        notNull(delivery.getDeliveryDates(), "deliveryDates is null");
        notNull(delivery.getDeliveryDates().getToDate(), "toDate in deliveryDates is null");
        notNull(delivery.getDeliveryDates().getFromDate(), "fromDate in deliveryDates is null");
        if(validateRegionId) {
            notNull(delivery.getRegionId(), "delivery regionId is null");
        }

        if (delivery.getType() == DeliveryType.DELIVERY || delivery.getType() == DeliveryType.POST) {
            if(!object.getAcceptMethod().equals(OrderAcceptMethod.WEB_INTERFACE)) {
                notNull(delivery.getAddress(), "delivery address is null");
            }

            final Address address = delivery.getAddress();
            if (address != null) {
                addressValidator.validate(address);
            }
            isNull(delivery.getOutletId(), "delivery outletId is not null");
        }
        if (delivery.getType() == DeliveryType.PICKUP) {
            notNull(delivery.getOutletId(), "delivery outletId is null");
            isNull(delivery.getAddress(), "delivery address is not null");
        }
        isNull(delivery.getOutletIds(), "delivery outletIds is not null");
    }
}
