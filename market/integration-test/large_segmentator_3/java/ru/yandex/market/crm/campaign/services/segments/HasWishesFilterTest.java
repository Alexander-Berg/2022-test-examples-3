package ru.yandex.market.crm.campaign.services.segments;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.UidPair;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.core.test.utils.YtTestTables;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.yt.client.YtClient;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.hasWishesFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;

/**
 * @author apershukov
 */
public class HasWishesFilterTest extends AbstractServiceLargeTest {

    private enum ItemType {
        SKU(0),
        PRODUCT(2),
        OFFER(3);

        private final int code;

        ItemType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    private static class Item {

        private final String itemId;
        private final ItemType itemType;
        private final Uid userId;
        private final int hid;

        Item(String itemId, ItemType itemType, Uid userId, int hid) {
            this.itemId = itemId;
            this.itemType = itemType;
            this.userId = userId;
            this.hid = hid;
        }

        String getItemId() {
            return itemId;
        }

        ItemType getItemType() {
            return itemType;
        }

        Uid getUserId() {
            return userId;
        }

        public int getHid() {
            return hid;
        }
    }

    private static String userType(Uid userId) {
        UidType type = userId.getType();
        if (type == UidType.PUID) {
            return "UID";
        } else if (type == UidType.YUID) {
            return "YANDEXUID";
        } else if (type == UidType.UUID) {
            return "UUID";
        } else {
            throw new IllegalArgumentException("Unsupported id type: " + type);
        }
    }

    private static Item item(String itemId, ItemType itemType, Uid userId, int hid) {
        return new Item(itemId, itemType, userId, hid);
    }

    private static YTreeMapNode category(int hid, int... parentHids) {
        List<Integer> hierarchyHids = Lists.newArrayList(hid);
        for (int parentHid : parentHids) {
            hierarchyHids.add(parentHid);
        }

        return YTree.mapBuilder()
                .key("hyper_id").value(hid)
                .key("hierarchy_hyper_ids").value(hierarchyHids)
                .buildMap();
    }

    private static YTreeMapNode offer(String wareMd5, int categoryId) {
        return YTree.mapBuilder()
                .key("ware_md5").value(wareMd5)
                .key("category_id").value(categoryId)
                .buildMap();
    }

    private static YTreeMapNode product(String id, int categoryId) {
        return YTree.mapBuilder()
                .key("id").value(Long.parseLong(id))
                .key("category_id").value(categoryId)
                .buildMap();
    }


    private static YTreeMapNode model(String id, int categoryId) {
        return YTree.mapBuilder()
                .key("model_id").value(Long.parseLong(id))
                .key("category_id").value(categoryId)
                .buildMap();
    }

    private static final Uid PUID_1 = Uid.asPuid(111L);
    private static final Uid PUID_2 = Uid.asPuid(222L);

    private static final Uid UUID_1 = Uid.asUuid("iddqd");
    private static final Uid UUID_2 = Uid.asUuid("idkfa");

    private static final Uid YUID_1 = Uid.asYuid("333");
    private static final Uid YUID_2 = Uid.asYuid("444");
    private static final Uid YUID_3 = Uid.asYuid("555");

    private static final int CATEGORY_1 = 1234;
    private static final int CATEGORY_1_1 = 12345;
    private static final int CATEGORY_2 = 2345;

    @Inject
    private YtClient ytClient;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private PropertiesProvider propertiesProvider;

    @Inject
    private YtTestTables ytTestTables;

    private YPath basketItemsTable;
    private YPath offersTable;
    private YPath skuTable;
    private YPath categoriesTable;

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareMobileAppInfoFactsTable();
        ytSchemaTestHelper.prepareMetrikaAppFactsTable();
        ytSchemaTestHelper.prepareModelInfoTable();
        ytSchemaTestHelper.preparePushTokenStatusesTable();

        basketItemsTable = YPath.simple(propertiesProvider.get("var.basket_items"));
        ytSchemaTestHelper.createTable(basketItemsTable, "basket_items.yson");

        offersTable = YPath.simple(propertiesProvider.get("var.indexer_offers"));
        ytSchemaTestHelper.createTable(offersTable, "indexer_offers.yson");

        skuTable = YPath.simple(propertiesProvider.get("var.mbo_sku"));
        ytSchemaTestHelper.createTable(skuTable, "mbo_sku_table.yson");

        categoriesTable = YPath.simple(propertiesProvider.get("var.categories_table"));
        ytSchemaTestHelper.createTable(categoriesTable, "categories_table.yson");
    }

    /**
     * Если в условии не указаны категории товаров в сегмент попадают идентификаторы
     * всех пользователей с отложенными
     */
    @Test
    public void testSelectAllIdsWithWishlist() throws Exception {
        prepareBasketItems(
                item("111", ItemType.PRODUCT, PUID_1, CATEGORY_1),
                item("222", ItemType.SKU, UUID_1, CATEGORY_1),
                item("333", ItemType.SKU, PUID_2, CATEGORY_2),
                item("444", ItemType.OFFER, YUID_1, CATEGORY_1_1)
                );

        Segment segment = segment(
                hasWishesFilter()
        );

        Set<UidPair> expected = Set.of(
                pair(PUID_1),
                pair(PUID_2),
                pair(YUID_1),
                pair(UUID_1)
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID, UidType.UUID),
                segment
        );
    }

    /**
     * Если в настройках условиях указана категория в результат попадают только пользователи
     * с избранными, принадлежащие к этой категории либо к одной из её подкатегорий
     */
    @Test
    public void testFilterWishlistByCategories() throws Exception {
        prepareCategories(
                category(CATEGORY_1),
                category(CATEGORY_1_1, CATEGORY_1),
                category(CATEGORY_2)
        );

        prepareBasketItems(
                item("111", ItemType.OFFER, PUID_1, CATEGORY_1),
                item("222", ItemType.OFFER, PUID_2, CATEGORY_2),
                item("333", ItemType.OFFER, YUID_1, CATEGORY_1_1),
                item("444", ItemType.SKU, UUID_1, CATEGORY_2),
                item("555", ItemType.SKU, YUID_2, CATEGORY_1_1),
                item("666", ItemType.PRODUCT, UUID_2, CATEGORY_1),
                item("777", ItemType.PRODUCT, YUID_3, CATEGORY_2)
        );

        Segment segment = segment(
                hasWishesFilter(1234)
        );

        segmentatorTestHelper.assertSegmentPairs(
                Set.of(pair(PUID_1), pair(YUID_1), pair(YUID_2), pair(UUID_2)),
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID, UidType.UUID),
                segment
        );
    }

    private void prepareBasketItems(Item... items) {
        List<YTreeMapNode> itemRows = new ArrayList<>(items.length);
        List<YTreeMapNode> offerRows = new ArrayList<>();
        List<YTreeMapNode> modelRows = new ArrayList<>();
        List<YTreeMapNode> skuRows = new ArrayList<>();

        for (Item item : items) {
            YTreeMapNode itemRow = YTree.mapBuilder()
                    .key("refId").value(item.getItemId())
                    .key("refType").value(item.getItemType().name().toLowerCase())
                    .key("userId").value(item.getUserId().getValue())
                    .key("userType").value(userType(item.getUserId()))
                    .buildMap();

            itemRows.add(itemRow);

            ItemType itemType = item.getItemType();
            if (itemType == ItemType.SKU) {
                skuRows.add(model(item.getItemId(), item.getHid()));
            } else if (itemType == ItemType.PRODUCT) {
                modelRows.add(product(item.getItemId(), item.getHid()));
            } else if (itemType == ItemType.OFFER) {
                offerRows.add(offer(item.getItemId(), item.getHid()));
            }
        }

        ytClient.write(basketItemsTable, YTableEntryTypes.YSON, itemRows);
        ytClient.write(skuTable, YTableEntryTypes.YSON, skuRows);
        ytClient.write(ytTestTables.getModelInfo(), YTableEntryTypes.YSON, modelRows);
        ytClient.write(offersTable, YTableEntryTypes.YSON, offerRows);
    }

    private void prepareCategories(YTreeMapNode... rows) {
        ytClient.write(categoriesTable, YTableEntryTypes.YSON, List.of(rows));
    }
}
