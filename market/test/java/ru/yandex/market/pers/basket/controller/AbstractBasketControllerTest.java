package ru.yandex.market.pers.basket.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.pers.basket.PersBasketTest;
import ru.yandex.market.pers.basket.model.BasketReferenceItem;
import ru.yandex.market.pers.basket.model.ItemReference;
import ru.yandex.market.pers.basket.model.ResultLimit;
import ru.yandex.market.pers.basket.service.BasketService;
import ru.yandex.market.pers.list.model.BasketItemType;
import ru.yandex.market.pers.list.model.BasketOwner;
import ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor;
import ru.yandex.market.pers.list.model.v2.enums.ReferenceType;
import ru.yandex.market.pers.list.model.v2.enums.SecondaryReferenceType;
import ru.yandex.market.util.ListUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.RED;
import static ru.yandex.market.pers.list.model.v2.enums.MarketplaceColor.WHITE;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.OFFER;
import static ru.yandex.market.pers.list.model.v2.enums.ReferenceType.PRODUCT;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 28.09.2020
 */
public abstract class AbstractBasketControllerTest extends PersBasketTest {
    public static final int UID = 141341;
    public static final String YANDEX_UID = "1189374918374";
    public static final String UUID = "wkelidj239203";
    public static final BasketOwner OWNER_YUID = BasketOwner.fromYandexUid(YANDEX_UID);
    public static final BasketOwner OWNER_UUID = BasketOwner.fromUuid(UUID);
    public static final BasketOwner OWNER_UID = BasketOwner.fromUid(UID);
    public static final BasketOwner OWNER_UID_2 = BasketOwner.fromUid(UID + 1);
    public static final String MODEL_ID = "45245235";
    public static final int FIRST_PAGE = ResultLimit.FIRST_PAGE_NUM;
    public static final int DEF_PAGE_SIZE = 20;
    public static final String SKU_ID = "123123";
    public static final String OFFER_ID = "qd23948h";
    public static final String RED_ID = "dfwer32323";
    public static final ResultMatcher ERR_4XX = status().is4xxClientError();
    public static final String DEFAULT_IMAGE = "https://avatars.mds.yandex.net/get-marketpic/901531/market_Aneq8fbtwGYb8Swr-l4z_A";

    @Autowired
    protected BasketService basketService;

    public static void assertContainsAll(List<BasketReferenceItem> items, List<BasketReferenceItem> expected) {
        assertEquals(expected.size(), items.size());
        assertTrue(ListUtils.toSet(items, BasketReferenceItem::buildSimpleItemKey).containsAll(
            ListUtils.toSet(expected, BasketReferenceItem::buildSimpleItemKey)
        ));
    }

    public static void assertContainsAllRef(List<ItemReference> items, List<BasketReferenceItem> expected) {
        assertEquals(expected.size(), items.size());
        assertTrue(ListUtils.toSet(items, ItemReference::buildSimpleItemKey).containsAll(
            ListUtils.toSet(expected, BasketReferenceItem::buildSimpleItemKey)
        ));
    }

    protected void assertItem(BasketReferenceItem item, ReferenceType referenceType, String referenceId) {
        assertEquals(referenceType, item.getReferenceType());
        assertEquals(referenceId, item.getReferenceId());
    }

    public static BasketReferenceItem generateItem(MarketplaceColor color, ReferenceType referenceType, String referenceId) {
        BasketReferenceItem item = new BasketReferenceItem();
        item.setColor(color);
        item.setReferenceType(referenceType);
        item.setReferenceId(referenceId);
        item.setImageBaseUrl(DEFAULT_IMAGE);
        item.setTitle(anyStr());

        Map<String, String> data = new HashMap<>();
        if (color == WHITE) {
            data.put(SecondaryReferenceType.HID.getName(), "123");
            if (referenceType == PRODUCT) {
                data.put(SecondaryReferenceType.MODEL_TYPE.getName(), BasketItemType.MODEL.name());
            } else if(referenceType == OFFER){
                data.put(SecondaryReferenceType.MODEL_ID.getName(), "666666");
            }
        }
        if (color == RED) {
            data.put(SecondaryReferenceType.WARE_MD5.getName(), "123");
        }
        item.setData(data);
        return item;
    }

    public static String anyStr() {
        return java.util.UUID.randomUUID().toString();
    }
}
