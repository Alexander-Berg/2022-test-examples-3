package ru.yandex.market.global.checkout.cart;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.cart.CartRepository;
import ru.yandex.market.global.checkout.domain.user.UserRepository;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.tables.pojos.Cart;
import ru.yandex.market.global.db.jooq.tables.pojos.User;

public class CartRepositoryTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(CartRepositoryTest.class).build();
    private static final long USER_ID = 10L;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    public void setup() {
        userRepository.insert(RANDOM.nextObject(User.class)
                .setId(USER_ID)
                .setMergedId(null)
                .setDeleted(false)
        );
    }

    @Test
    public void testVersionDoNotHandledByJooq() {
        Cart cart = RANDOM.nextObject(Cart.class).setUserId(USER_ID);

        cartRepository.insert(cart.setVersion(1L));
        cartRepository.update(cart.setVersion(1L));
    }
}
