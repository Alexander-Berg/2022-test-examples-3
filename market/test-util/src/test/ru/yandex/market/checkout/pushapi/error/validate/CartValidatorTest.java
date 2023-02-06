package ru.yandex.market.checkout.pushapi.error.validate;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.mock;

public class CartValidatorTest {

    private CartValidator validator = new CartValidator();
    private AddressValidator addressValidator = mock(AddressValidator.class);
    private Cart cart;
    private OrderItem offerItem;
    private OrderItem offerItem1;
    private Delivery delivery;

    @Before
    public void setUp() throws Exception {
        validator.setAddressValidator(addressValidator);
        offerItem = new OrderItem(/*1234l, "2345", "3456", "offername1", 5*/);
        offerItem1 = new OrderItem(/*2345l, "3456", "4567", "offername2", 4*/);

        cart = new Cart(
            new Delivery(1234l, new Address()),
            Currency.AUD,
            Arrays.asList(offerItem, offerItem1)
        );

        delivery = cart.getDelivery();
    }

    @Test
    public void testOk() throws Exception {
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testCartIsNull() throws Exception {
        validator.validate(null);
    }

    // cart params

    @Test(expected = ValidationException.class)
    public void testDeliveryIsNull() throws Exception {
        cart.setDelivery(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testCurrencyIsNull() throws Exception {
        cart.setCurrency(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemsListIsNull() throws Exception {
        cart.setItems(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemsListIsEmpty() throws Exception {
        cart.setItems(Collections.<OrderItem>emptyList());
        validator.validate(cart);
    }

    // cart delivery params

    @Test(expected = ValidationException.class)
    public void testDeliveryRegionIsNull() throws Exception {
        delivery.setRegionId(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testDeliveryRegionIsNegative() throws Exception {
        delivery.setRegionId(-1l);
        validator.validate(cart);
    }

    // cart items paras

    @Test(expected = ValidationException.class)
    public void testItemIsNull() throws Exception {
        cart.setItems(Arrays.asList(null, offerItem1));
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsNull() throws Exception {
        offerItem.setFeedId(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsZero() throws Exception {
        offerItem.setFeedId(0l);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedIdIsNegative() throws Exception {
        offerItem.setFeedId(-1l);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferIdIsNull() throws Exception {
        offerItem.setOfferId(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferIdIsEmpty() throws Exception {
        offerItem.setOfferId("");
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedCategoryIdIsNull() throws Exception {
        offerItem.setFeedCategoryId(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemFeedCategoryIdIsEmpty() throws Exception {
        offerItem.setFeedCategoryId("");
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferNameIsNull() throws Exception {
        offerItem.setOfferName(null);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemOfferNameIsEmpty() throws Exception {
        offerItem.setOfferName("");
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemCountIsZero() throws Exception {
        offerItem.setCount(0);
        validator.validate(cart);
    }

    @Test(expected = ValidationException.class)
    public void testItemCountIsNegative() throws Exception {
        offerItem.setCount(-1);
        validator.validate(cart);
    }

}
