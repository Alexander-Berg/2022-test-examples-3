package ru.yandex.market.checkout.carter.storage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;

import ru.yandex.market.checkout.carter.CarterMockedDbTestBase;
import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartList;
import ru.yandex.market.checkout.carter.model.ItemImageMeta;
import ru.yandex.market.checkout.carter.model.ItemOffer;
import ru.yandex.market.checkout.carter.model.OwnerKey;
import ru.yandex.market.checkout.carter.storage.dao.ydb.YdbDao;
import ru.yandex.market.checkout.carter.web.UserContext;
import ru.yandex.market.checkout.common.report.ColoredGenericMarketReportService;
import ru.yandex.market.common.report.model.FoundOffer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verifyNoInteractions;
import static ru.yandex.market.checkout.carter.model.Color.WHITE;
import static ru.yandex.market.checkout.carter.model.UserIdType.YANDEXUID;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpyBean(ColoredGenericMarketReportService.class)
public class StorageCartServiceImageMetaTest extends CarterMockedDbTestBase {

    private static final String PICTURE_JSON_PATTERN = "{\"entity\": \"picture\"," +
            "\"original\": {" +
            "\"width\": 701," +
            "\"height\": 343," +
            "\"namespace\": \"%s\"," +
            "\"groupId\": %d," +
            "\"key\": \"%s\"" +
            "}," +
            "\"signatures\": []" +
            "}";

    @Autowired
    private StorageCartService cartService;
    @Autowired
    private ColoredGenericMarketReportService reportService;
    @Autowired
    private YdbDao cartListDao;

    @Test
    @DisplayName("POSITIVE: Добавление товара в корзину без ImageMeta")
    void addItemWithImageMetaDisabledPropertyTest() throws IOException, InterruptedException {

        UserContext userContext = prepareData();
        commonInsertItemTest(userContext, 1, false);

        verifyNoInteractions(reportService);

        Update<List<CartList>> lists = cartService.getListsOwnerId(userContext);
        CartList cartList = lists.getResult().get(0);

        cartList.getItems().forEach(item -> assertNull(item.getImageMeta()));
    }

    @Test
    @DisplayName("POSITIVE: Добавление нескольких товаров в корзину без ImageMeta")
    void addItemsWithImageMetaDisabledPropertyTest() throws IOException, InterruptedException {

        UserContext userContext = prepareData();
        commonInsertItemTest(userContext, 3, false);

        verifyNoInteractions(reportService);

        Update<List<CartList>> lists = cartService.getListsOwnerId(userContext);
        CartList cartList = lists.getResult().get(0);

        cartList.getItems().forEach(item -> assertNull(item.getImageMeta()));
    }

    private Map<String, FoundOffer> commonInsertItemTest(UserContext userContext, int count, boolean withImageMeta)
            throws IOException, InterruptedException {

        Update<List<CartList>> lists = cartService.getListsOwnerId(userContext);
        CartList cartList = lists.getResult().get(0);

        for (int i = 0; i < count; i++) {
            if (!withImageMeta) {
                cartList.getItems().add(generateItem(RandomStringUtils.randomAlphabetic(10)));
            } else {
                cartList.getItems().add(generateItemWithImageMeta(RandomStringUtils.randomAlphabetic(10)));
            }
        }

        List<FoundOffer> foundOffers = cartList.getItems()
                .stream()
                .map(this::mapItemToFoundOffer)
                .collect(Collectors.toList());

        doReturn(foundOffers).when(reportService).executeSearchAndParse(any(), any());
        if (count > 1) {
            cartService.bulkUpdateItemsOwnerId(userContext, lists.getResult(), true);
        } else {
            cartService.createItemNoMskuCheck(userContext, cartList.getItems().get(0));
        }

        return foundOffers.stream()
                .collect(Collectors.toMap(FoundOffer::getWareMd5, Function.identity()));
    }

    @Nonnull
    private CartItem generateItemWithImageMeta(String wareMd5) {
        ItemOffer item = generateItem(wareMd5);
        item.setImageMeta(new ItemImageMeta(
                RandomStringUtils.randomAlphabetic(10),
                RandomUtils.nextLong(),
                RandomStringUtils.randomAlphabetic(10)
        ));

        return item;
    }

    @Nonnull
    private String generatePicturesRaw(int count) {
        StringBuilder picturesJson = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                picturesJson.append(",");
            }
            picturesJson.append(String.format(PICTURE_JSON_PATTERN,
                    RandomStringUtils.randomAlphabetic(10),
                    RandomUtils.nextLong(),
                    RandomStringUtils.randomAlphabetic(10)
            ));
        }
        return picturesJson.append("]").toString();
    }

    @Nonnull
    private FoundOffer mapItemToFoundOffer(CartItem item) {
        FoundOffer offer = new FoundOffer();

        offer.setWareMd5(item.getObjId());
        offer.setPicturesRaw(new JSONArray(generatePicturesRaw(2)));
        offer.setAdult(RandomUtils.nextBoolean());

        return offer;
    }

    @Nonnull
    private UserContext prepareData() {
        String userId = RandomStringUtils.randomAlphabetic(10);

        UserContext userContext = UserContext.of(OwnerKey.of(WHITE, YANDEXUID, userId));
        cartListDao.createCartList(userContext);
        return userContext;
    }
}
