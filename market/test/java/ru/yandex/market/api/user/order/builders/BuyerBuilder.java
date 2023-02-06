package ru.yandex.market.api.user.order.builders;

import ru.yandex.market.api.user.order.Buyer;

public class BuyerBuilder extends RandomBuilder<Buyer> {
    private Buyer buyer = new Buyer();

    @Override
    public BuyerBuilder random() {
        buyer.setFirstName(random.getString());
        buyer.setLastName(random.getString());
        buyer.setEmail(random.getEmail());
        buyer.setPhone(random.getNumber());

        return this;
    }

    public BuyerBuilder withWaitingCall(boolean isWaitingCall) {
        buyer.setWaitingCall(isWaitingCall);
        return this;
    }

    @Override
    public Buyer build() {
        return buyer;
    }
}
