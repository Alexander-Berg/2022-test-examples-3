package ru.yandex.market.checkout.carter.web;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.AddItemRequest;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.CartRequest;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;
import ru.yandex.market.checkout.cipher.CipherService;
import ru.yandex.market.report.MarketSearchSession;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWith;

public class CartResourceControllerDecipherPpTest extends CarterMockedDbTestBase {

    private static final String OFFER = "some_offer";
    private static final int BYTELEN = 8;

    @Autowired
    private Carter carterClient;
    @Autowired
    private CipherService reportCipherService;

    private UserContext userContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        userContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
    }

    @Test
    public void shouldDecipherAndReturnPp() {
        Integer ppForTest = 2222;
        String cipheredCpaContectRecord = generateCipheredCpaContextRecord(ppForTest);
        carterClient.addItem(AddItemRequest.builder()
                .withColor(Color.BLUE)
                .withUserIdType(userContext.getUserIdType())
                .withUserAnyId(userContext.getUserAnyId())
                .withCartItem(CartConverter.convert(generateItemWith(OFFER,
                        offer -> offer.setFee(cipheredCpaContectRecord))))
                .build());

        CartList basket = CartConverter.convert(carterClient.getCart(
                CartRequest.builder(userContext.getUserAnyId(), userContext.getUserIdType())
                        .withRgb(Color.BLUE)
                        .build())).getBasketList();

        List<CartItem> items = basket.getItems();
        assertThat(items, hasSize(1));
        assertThat(((ItemOffer) (items.iterator().next())).getPp(), is(ppForTest));
    }

    private String generateCipheredCpaContextRecord(Integer pp) {
        MarketSearchSession.CpaContextRecord cpaContextRecord = MarketSearchSession.CpaContextRecord.newBuilder()
                .setPp(pp)
                .build();
        byte[] raw = cpaContextRecord.toByteArray();
        int pad = raw.length % BYTELEN;
        if (pad != 0) {
            raw = Arrays.copyOf(raw, raw.length + BYTELEN - pad);
            for (int i = 1; i <= BYTELEN - pad; i++) {
                raw[raw.length - i] = 0;
            }
        }
        return reportCipherService.cipher(raw);
    }


}
