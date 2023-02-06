package ru.yandex.market.checkout.carter.storage;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.model.UserIdType;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.UserContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CartServiceTest extends CarterMockedDbTestBase {

    @Autowired
    private StorageCartService cartService;
    @Autowired
    private YdbDao cartListDao;

    private UserContext userContext;

    @BeforeEach
    public void prepare() {
        userContext = UserContext.of(OwnerKey.of(
                Color.BLUE, UserIdType.UID, "" + rnd.nextLong(1, Integer.MAX_VALUE)));
    }

    @Test
    public void createCartList() {
        final Update<List<CartList>> listUpdateBefore = cartService.getListsOwnerId(userContext);
        assertNotNull(listUpdateBefore);
        // Вернутся дефолтные
        assertThat(listUpdateBefore.getResult(), hasSize(1));

        final CartList cart = cartListDao.createCartList(userContext);
        assertNotNull(cart);
        assertNotNull(cart.getCreateTime());
        assertNotNull(cart.getUpdateTime());

        final Update<List<CartList>> listUpdateAfter = cartService.getListsOwnerId(userContext);
        assertNotNull(listUpdateAfter);
        // Вернётся созданный
        assertThat(listUpdateAfter.getResult(), hasSize(1));
        assertThat(cart.getId(), equalTo(listUpdateAfter.getResult().get(0).getId()));
    }

}
