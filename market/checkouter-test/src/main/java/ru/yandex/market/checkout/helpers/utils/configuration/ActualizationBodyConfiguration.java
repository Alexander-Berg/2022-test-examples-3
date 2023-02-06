package ru.yandex.market.checkout.helpers.utils.configuration;

import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;

public class ActualizationBodyConfiguration {

    private Buyer buyer;
    private MultiCart builtMultiCart;

    private boolean skipShowInfoAdjusting;

    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }

    public MultiCart multiCart() {
        return builtMultiCart;
    }

    public void setMultiCart(MultiCart builtMultiCart) {
        this.builtMultiCart = builtMultiCart;
    }

    public void skipShowInfoAdjusting(boolean skip) {
        this.skipShowInfoAdjusting = skip;
    }

    public boolean isSkipShowInfoAdjusting() {
        return skipShowInfoAdjusting;
    }
}
