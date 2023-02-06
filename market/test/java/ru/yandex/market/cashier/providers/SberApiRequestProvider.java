package ru.yandex.market.cashier.providers;

import ru.yandex.market.cashier.sber.api.rest.AdditionalJsonParams;
import ru.yandex.market.cashier.sber.api.rest.CartItem;
import ru.yandex.market.cashier.sber.api.rest.CartItems;
import ru.yandex.market.cashier.sber.api.rest.Installments;
import ru.yandex.market.cashier.sber.api.rest.OrderBundle;
import ru.yandex.market.cashier.sber.api.rest.Quantity;
import ru.yandex.market.cashier.sber.api.rest.RegisterOrderRequest;

import java.util.ArrayList;

/**
 * @author : poluektov
 * date: 2019-04-17.
 */

public class SberApiRequestProvider {
    public static final long DEFAULT_PRICE =100000L;
    //TODO:ENUM;
    public static final int CURRENCY_CODE = 643;

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
        Installments installments = new Installments();
        installments.setProductId("10");
        installments.setProductType("INSTALLMENT");
        return installments;
    }

    public static Installments getCreditProduct() {
        Installments installments = new Installments();
        installments.setProductId("10");
        installments.setProductType("CREDIT");
        return installments;
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
