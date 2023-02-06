package ru.yandex.market.checkout.providers;

import java.util.ArrayList;

import ru.yandex.market.checkout.checkouter.sberbank.model.AdditionalJsonParams;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItem;
import ru.yandex.market.checkout.checkouter.sberbank.model.CartItems;
import ru.yandex.market.checkout.checkouter.sberbank.model.Installments;
import ru.yandex.market.checkout.checkouter.sberbank.model.OrderBundle;
import ru.yandex.market.checkout.checkouter.sberbank.model.ProductType;
import ru.yandex.market.checkout.checkouter.sberbank.model.Quantity;
import ru.yandex.market.checkout.checkouter.sberbank.model.RegisterOrderRequest;

/**
 * @author : poluektov
 * date: 2019-04-17.
 */

public final class SberApiRequestProvider {

    public static final long DEFAULT_PRICE = 100000L;
    //TODO:ENUM;
    public static final int CURRENCY_CODE = 643;

    private SberApiRequestProvider() {
    }

    public static RegisterOrderRequest getDefaultRegisterOrderRequest() {
        RegisterOrderRequest request = new RegisterOrderRequest();
        request.setAmount(DEFAULT_PRICE);
        request.setCurrency(CURRENCY_CODE);
        request.setReturnUrl("http://suchMarket.com");
        request.setOrderNumber(123L);
        AdditionalJsonParams jsonParams = new AdditionalJsonParams();
        jsonParams.setPhone("+7123456789");
        request.setJsonParams(jsonParams);
        OrderBundle orderBundle = new OrderBundle();
        orderBundle.setInstallments(getCreditProduct());
        orderBundle.setCartItems(getDefaultCartItems());
        request.setOrderBundle(orderBundle);
        return request;
    }

    public static Installments getInstallmentProduct() {
        return new Installments(ProductType.INSTALLMENT);
    }

    public static Installments getCreditProduct() {
        return new Installments(ProductType.CREDIT);
    }

    public static CartItems getDefaultCartItems() {
        CartItems cartItems = new CartItems();
        cartItems.setItems(new ArrayList<>());
        CartItem item = new CartItem();
        item.setItemAmount(DEFAULT_PRICE);
        item.setItemPrice(DEFAULT_PRICE);
        item.setItemCode("itemIDfromCheckouter");
        item.setName("BEST ITEM");
        item.setPositionId(1);
        item.setQuantity(buildCountQuantity(1));
        cartItems.getItems().add(item);
        return cartItems;
    }

    public static Quantity buildCountQuantity(long count) {
        Quantity quantity = new Quantity();
        quantity.setMeasure("count");
        quantity.setValue(count);
        return quantity;
    }
}
