package ru.yandex.market.checkout.carter.web;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.client.Carter;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.Color;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.report.ReportMockConfigurer;
import ru.yandex.market.checkout.carter.storage.StorageCartService;
import ru.yandex.market.checkout.carter.util.converter.CartConverter;

import static ru.yandex.market.checkout.carter.model.UserIdType.UID;
import static ru.yandex.market.checkout.carter.storage.StorageCartService.LIST_BASKET_ID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

public class CartResourceControllerDublicatesTest extends CarterMockedDbTestBase {

    private static final String OFFER = "some_offer";

    @Autowired
    private Carter carterClient;
    @Autowired
    private StorageCartService storageCartService;
    @Autowired
    private ReportMockConfigurer reportMockConfigurer;

    @Value("${carter.user_limits.limit_items}")
    private int limitItems;

    private UserContext uidContext;

    @BeforeEach
    public void setUp() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        uidContext = UserContext.of(OwnerKey.of(Color.BLUE, UID, "" + rnd.nextLong(1, Long.MAX_VALUE)));
    }

    @AfterEach
    public void clear() {
        reportMockConfigurer.resetMock();
    }


    @Test
    public void shouldDuplicatesShouldMergeOnBulkCreate() {
        CartList list = storageCartService.getListsOwnerId(uidContext).getResult().get(0);
        for (int i = 0; i <= limitItems; i++) {
            list.addItem(generateItem(OFFER));
        }

        carterClient.addItems(uidContext.getUserAnyId(), UID, LIST_BASKET_ID, Color.BLUE, CartConverter.convert(list),
                null);
        CartList result = CartConverter.convert(carterClient.getCart(uidContext.getUserAnyId(), UID, 456L,
                Color.BLUE, false))
                .getBasketList();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getItems().size());
    }
}
