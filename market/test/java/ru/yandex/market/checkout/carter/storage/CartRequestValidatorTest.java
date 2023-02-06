package ru.yandex.market.checkout.carter.storage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartItem.Type;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.util.CartRequestValidator;
import ru.yandex.market.checkout.carter.web.UserContext;

/**
 * @author imelnikov
 */
public class CartRequestValidatorTest {

    private final CartRequestValidator requestValidator = new CartRequestValidator();

    @Test
    public void testItemValidation() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            UserContext userContext = UserContext.of(Color.UNKNOWN, "1", UserIdType.UID);
            CartItem item = new CartItem(Type.OFFER, "offer", "iPhone7");
            item.setCount(-3);
            requestValidator.validateBeforeCreate(userContext, item);
        });
    }

    @Test
    public void testOfferValidation() {
        UserContext userContext = UserContext.of(Color.UNKNOWN, "1", UserIdType.UID);
        ItemOffer o = new ItemOffer("ware", "iPhone7");
        o.setShopId(3L);
        o.setModelId(5L);
        o.setHid(10L);
        requestValidator.validateBeforeCreate(userContext, o);
    }
}
