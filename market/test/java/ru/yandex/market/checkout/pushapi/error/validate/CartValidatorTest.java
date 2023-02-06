package ru.yandex.market.checkout.pushapi.error.validate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.AddressImpl;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.CartItem;
import ru.yandex.market.checkout.pushapi.shop.validate.ValidationException;

import static org.mockito.Mockito.mock;

public class CartValidatorTest {

    private CartValidator validator = new CartValidator();
    private AddressValidator addressValidator = mock(AddressValidator.class);
    private Cart cart;
    private CartItem offerItem;
    private CartItem offerItem1;
    private Delivery delivery;

    @BeforeEach
    public void setUp() throws Exception {
        validator.setAddressValidator(addressValidator);
        offerItem = new CartItem() {{
            setFeedId(1L);
            setOfferId("1");
            setPrice(new BigDecimal(10));
            setCount(5);
        }};
        offerItem.setFeedCategoryId("1");
        offerItem.setOfferName("1");
        offerItem1 = new CartItem() {{
            setFeedId(2L);
            setOfferId("2");
            setPrice(new BigDecimal(20));
            setCount(4);
        }};
        offerItem1.setFeedCategoryId("2");
        offerItem1.setOfferName("2");

        cart = new Cart(
                new Delivery(1234L, new AddressImpl()),
                Currency.KZT,
                null,
                Arrays.asList(offerItem, offerItem1)
        );

        delivery = cart.getDelivery();
    }

    @Test
    public void testOk() {
        validator.validate(cart);
    }

    @Test
    public void testCartIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(null);
        });
    }

    // cart params

    // cart params
    @Test
    public void testDeliveryIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cart.setDelivery(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testCurrencyIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cart.setCurrency(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemsListIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cart.setItems(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemsListIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cart.setItems(Collections.<CartItem>emptyList());
            validator.validate(cart);
        });
    }

    // cart delivery params

    // cart delivery params
    @Test
    public void testDeliveryRegionIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery.setRegionId(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testDeliveryRegionIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            delivery.setRegionId(-1l);
            validator.validate(cart);
        });
    }

    // cart items paras

    // cart items paras
    @Test
    public void testItemIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            cart.setItems(Arrays.asList(null, offerItem1));
            validator.validate(cart);
        });
    }

    @Test
    public void testItemFeedIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setFeedId(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemFeedIdIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setFeedId(0l);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemFeedIdIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setFeedId(-1l);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemOfferIdIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setOfferId(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemOfferIdIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setOfferId("");
            validator.validate(cart);
        });
    }

    @Test()
    public void testItemFeedCategoryIdIsNullOkBlue() throws Exception {
        cart.setFulfilment(true);
        offerItem.setFeedCategoryId(null);
        validator.validate(cart);
    }

    @Test()
    public void testItemFeedCategoryIdIsNullOkBlueWithoutFulfilment() throws Exception {
        cart.setRgb(Color.BLUE);
        cart.setFulfilment(false); //golden
        offerItem.setFeedCategoryId(null);
        validator.validate(cart);
    }

    @Test()
    public void testItemFeedCategoryIdIsNullOkFulfilment() throws Exception {
        cart.setRgb(Color.RED);
        cart.setFulfilment(true);
        offerItem.setFeedCategoryId(null);
        validator.validate(cart);
    }

    @Test
    public void testItemOfferNameIsNull() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setOfferName(null);
            validator.validate(cart);
        });
    }

    @Test
    public void testItemOfferNameIsEmpty() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setOfferName("");
            validator.validate(cart);
        });
    }

    @Disabled
    @Test
    public void testItemCountIsZero() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setCount(0);
            validator.validate(cart);
        });
    }

    @Disabled
    @Test
    public void testItemCountIsNegative() {
        Assertions.assertThrows(ValidationException.class, () -> {
            offerItem.setCount(-1);
            validator.validate(cart);
        });
    }

}
