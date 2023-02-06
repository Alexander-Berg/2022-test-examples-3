package ru.yandex.market.checkout.pushapi.error.validate;

import org.springframework.stereotype.Component;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static ru.yandex.market.checkout.pushapi.shop.validate.Validate.notNull;

@Component
public class AddressValidator implements Validator<Address> {
    @Override
    public void validate(Address address) throws ValidationException {
        notNull(address.getCountry(), "delivery.address.country is null");
        notNull(address.getCity(), "delivery.address.city is null");
        notNull(address.getStreet(), "delivery.address.street is null");
    }
}
