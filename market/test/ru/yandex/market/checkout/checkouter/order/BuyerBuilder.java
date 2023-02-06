package ru.yandex.market.checkout.checkouter.order;

import ru.yandex.market.checkout.pushapi.client.entity.BaseBuilder;

/**
 * @author msavelyev
 */
public class BuyerBuilder extends BaseBuilder<Buyer, BuyerBuilder> {

    public BuyerBuilder() {
        super(new Buyer());

        object.setId("1234");
        object.setPhone("+7 123 456 7890");
        object.setEmail("hello@localhost");
        object.setLastName("Иванов");
        object.setFirstName("Иван");
        object.setMiddleName("Иваныч");
    }

    public BuyerBuilder withId(String id) {
        return withField("id", id);
    }

    public BuyerBuilder withLastName(String lastName) {
        return withField("lastName", lastName);
    }

    public BuyerBuilder withFirstName(String firstName) {
        return withField("firstName", firstName);
    }

    public BuyerBuilder withMiddleName(String middleName) {
        return withField("middleName", middleName);
    }

    public BuyerBuilder withPhone(String phone) {
        return withField("phone", phone);
    }

    public BuyerBuilder withEmail(String email) {
        return withField("email", email);
    }

}
