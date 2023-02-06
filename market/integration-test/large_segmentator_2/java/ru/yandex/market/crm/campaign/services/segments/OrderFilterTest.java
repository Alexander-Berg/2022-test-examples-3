package ru.yandex.market.crm.campaign.services.segments;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.ordersFilter;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper.order;
import static ru.yandex.market.crm.core.test.utils.OrderFactsTestHelper.orderItem;

/**
 * @author apershukov
 */
@Disabled("Перевести тесты на chyt, когда будет возможность")
public class OrderFilterTest extends AbstractServiceLargeTest {

    private static final String EMAIL_1 = "user1@yandex.ru";
    private static final String EMAIL_2 = "user2@yandex.ru";
    private static final String EMAIL_3 = "user3@yandex.ru";
    private static final String EMAIL_4 = "user4@yandex.ru";
    private static final String EMAIL_5 = "user5@yandex.ru";
    private static final String EMAIL_6 = "user6@yandex.ru";
    private static final long PUID = 111;

    @Inject
    private YtSchemaTestHelper schemaTestHelper;

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private OrderFactsTestHelper orderFactsTestHelper;

    @Inject
    private PropertiesProvider propertiesProvider;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    private YPath categoriesTable;
    private YPath allModelsTable;
    private YPath skuTable;

    private static YTreeMapNode category(int hid) {
        return YTree.mapBuilder()
                .key("hyper_id").value(hid)
                .key("hierarchy_hyper_ids").value(List.of(hid))
                .buildMap();
    }

    private static YTreeMapNode model(String title, long modelId) {
        return YTree.mapBuilder()
                .key("title").value(title)
                .key("model_id").value(modelId)
                .buildMap();
    }

    private static YTreeMapNode model(String title, long modelId, long vendorId) {
        return YTree.mapBuilder()
                .key("title").value(title)
                .key("model_id").value(modelId)
                .key("vendor_id").value(vendorId)
                .buildMap();
    }

    @BeforeEach
    public void setUp() {
        ytSchemaTestHelper.prepareUserTables();
        categoriesTable = YPath.simple(propertiesProvider.get("var.categories_table"));
        allModelsTable = YPath.simple(propertiesProvider.get("var.mbo_all_models"));
        skuTable = YPath.simple(propertiesProvider.get("var.mbo_sku"));
        ytSchemaTestHelper.createTable(categoriesTable, "categories_table.yson");
        ytSchemaTestHelper.createTable(allModelsTable, "mbo_all_models_table.yson");
        ytSchemaTestHelper.createTable(skuTable, "mbo_sku_table.yson");
    }

    @Test
    public void testFilterWithMultipleIdsOrder() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(EMAIL_1, PUID)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1)),
                pair(Uid.asPuid(PUID))
        );

        Segment segment = segment(
                ordersFilter()
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL, UidType.PUID),
                segment
        );
    }

    /**
     * Если у пользователя, совершившего заказ, есть типы идентификаторов, с которыми мы не работаем,
     * то они не попадают в результат подсчёта сегмента
     */
    @Test
    public void testSegmentResultNotContainsUnusedIdsTypes() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.MUID, "111111122222")
                )
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );

        Segment segment = segment(
                ordersFilter()
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Если были указаны выходные идентификаторы, то только они попадают в результат подсчёта сегмента
     */
    @Test
    public void testFilterWithIdTypesCustomization() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(EMAIL_1, PUID)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );

        Segment segment = segment(
                ordersFilter().setUidTypes(Set.of(UidType.EMAIL))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL, UidType.PUID),
                segment
        );
    }

    /**
     * Если включен режим схлопывания мультизаказа в один, то при подсчете сегмента количество всех подзаказов
     * в мультизаказе считается за 1
     */
    @Test
    public void testFilterWithCollapseOrdersAndCounts() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(
                        1, "1", 100,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(
                        2, "1", 100,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(
                        3, "", 100,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(
                        4, "", 200,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                )
        );
        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_2))
        );
        Segment segment = segment(
                ordersFilter(true, 1, 0, Collections.emptyList()).setUidTypes(Set.of(UidType.EMAIL))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Если включен режим схлопывания мультизаказа в один, то при подсчете сегмента стоимость всех подзаказов
     * в мультизаказе суммируется, суммируется, делится на 100 (так как с копейками)
     * и сравнивается с указанной стоимостью в конфиге сегмента
     */
    @Test
    public void testFilterWithCollapseOrdersAndPrice() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(
                        0, "", 100,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(
                        1, "1", 50000,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(
                        2, "1", 50000,
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                )
        );
        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1))
        );
        Segment segment = segment(
                ordersFilter(true, 0, 999, Collections.emptyList()).setUidTypes(Set.of(UidType.EMAIL))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    /**
     * Если включен режим схлопывания мультизаказа в один, и указана какая-либо категория товарного дерева,
     * то в сегмент попадает пользователь с мультизаказом, если хотя бы один из подзаказов мультизаказа соответствует
     * указанной категории. При этом пользователи с обычными заказами с моделями из выбранной категории также
     * попадают в сегмент.
     */
    @Test
    public void testFilterWithCollapseOrdersAndHids() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(
                        1, "", List.of(orderItem(1, 1, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(
                        2, "", List.of(orderItem(2, 2, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_3)
                ),
                order(
                        3, "123", List.of(orderItem(1, 3, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(
                        4, "123", List.of(orderItem(2, 4, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                )
        );
        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1)),
                pair(Uid.asEmail(EMAIL_2))
        );
        Segment segment = segment(
                ordersFilter(true, 0, 0, Collections.emptyList(), 1)
                        .setUidTypes(Set.of(UidType.EMAIL))
        );

        prepareCategories(category(1), category(2));

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    @Test
    public void testFilterByModelName() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(1, "", List.of(orderItem(1, 1, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(2, "", List.of(orderItem(1, 2, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(3, "", List.of(orderItem(1, 3, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_3)
                ),
                order(4, "", List.of(orderItem(1, 4, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_4)
                )
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1001L),
                model("Автомобиль BMW", 2, 1002L)
        );

        prepareSkuTable(
                model("Автомобиль НИВА синий", 3, 1003L),
                model("Автомобиль Жигули красный", 4, 1004L)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_2)),
                pair(Uid.asEmail(EMAIL_3))
        );

        Segment segment = segment(
                ordersFilter(false, 0, 0, List.of("Автомобиль И синий", "BMW",
                        "Porsche И синий", "Жигули синий", "Автомобиль красный"))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    @Test
    public void testFilterByVendorId() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(1, "", List.of(orderItem(1, 1, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(2, "", List.of(orderItem(1, 2, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(3, "", List.of(orderItem(1, 3, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_3)
                ),
                order(4, "", List.of(orderItem(1, 4, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_4)
                ),
                order(5, "", List.of(orderItem(1, 5, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_5)
                ),
                order(6, "", List.of(orderItem(1, 6, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_6)
                )
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1001L),
                model("Автомобиль BMW", 2, 1002L),
                model("Автомобиль Skoda", 3, 1003L),
                model("Автомобиль Volvo", 4, 1003L)
        );

        prepareSkuTable(
                model("Автомобиль НИВА синий", 5, 1004L),
                model("Автомобиль Жигули красный", 6, 1005L)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_1)),
                pair(Uid.asEmail(EMAIL_3)),
                pair(Uid.asEmail(EMAIL_4)),
                pair(Uid.asEmail(EMAIL_6))
        );

        Segment segment = segment(
                ordersFilter(false, 0, 0, List.of(), List.of(1001L, 1003L, 1005L))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    @Test
    public void testFilterByModelNameAndVendorId() throws Exception {
        orderFactsTestHelper.prepareOrders(
                order(1, "", List.of(orderItem(1, 1, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_1)
                ),
                order(2, "", List.of(orderItem(1, 2, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_2)
                ),
                order(3, "", List.of(orderItem(1, 3, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_3)
                ),
                order(4, "", List.of(orderItem(1, 4, 100, 1)),
                        Uids.create(ru.yandex.market.crm.platform.commons.UidType.EMAIL, EMAIL_4)
                )
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1001L),
                model("Автомобиль BMW", 2, 1002L)
        );

        prepareSkuTable(
                model("Автомобиль НИВА синий", 3, 1003L),
                model("Автомобиль Жигули красный", 4, 1004L)
        );

        Set<UidPair> expected = Set.of(
                pair(Uid.asEmail(EMAIL_3))
        );

        Segment segment = segment(
                ordersFilter(false, 0, 0, List.of("Автомобиль И синий", "BMW",
                        "Porsche И синий", "Жигули синий", "Автомобиль красный"), List.of(1001L, 1003L, 1005L))
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.EMAIL),
                segment
        );
    }

    private void prepareCategories(YTreeMapNode... rows) {
        ytClient.write(categoriesTable, YTableEntryTypes.YSON, List.of(rows));
    }

    private void prepareModelsTable(YTreeMapNode... rows) {
        ytClient.write(allModelsTable, YTableEntryTypes.YSON, List.of(rows));
    }

    private void prepareSkuTable(YTreeMapNode... rows) {
        ytClient.write(skuTable, YTableEntryTypes.YSON, List.of(rows));
    }

}
