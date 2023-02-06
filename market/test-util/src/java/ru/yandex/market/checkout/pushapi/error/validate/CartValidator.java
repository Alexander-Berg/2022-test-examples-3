package ru.yandex.market.checkout.pushapi.error.validate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.common.currency.AllowedCurrencies;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.*;

@Component
public class CartValidator implements Validator<Cart> {

    private AddressValidator addressValidator;

    @Autowired
    public void setAddressValidator(AddressValidator addressValidator) {
        this.addressValidator = addressValidator;
    }

    @Override
    public void validate(Cart object) throws ValidationException {
        notNull(object, "cart is null");

        final Currency currency = object.getCurrency();
        notNull(currency, "cart currency is null");
        isTrue(
            AllowedCurrencies.isAllowed(currency),
            "currency " + currency + " is not allowed"
        );

        notNull(object.getItems(), "cart items is null");
        nonEmpty(object.getItems(), "cart items is empty");

        final Delivery delivery = object.getDelivery();
        notNull(delivery, "cart delivery is null");
        notNull(delivery.getRegionId(), "delivery regionId is null");
        nonNegative(delivery.getRegionId(), "delivery region is negative");


        final Address address = delivery.getAddress();
        if(address != null) {
            addressValidator.validate(address);
        }

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
            notNull(offerItem.getCount(), "item count is empty");
        }


    }

}
