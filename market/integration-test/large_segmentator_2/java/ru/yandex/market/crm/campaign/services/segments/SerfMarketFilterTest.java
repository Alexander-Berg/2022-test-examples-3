package ru.yandex.market.crm.campaign.services.segments;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.crm.campaign.test.AbstractServiceLargeTest;
import ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;
import ru.yandex.market.crm.core.domain.segment.Segment;
import ru.yandex.market.crm.core.test.utils.YtSchemaTestHelper;
import ru.yandex.market.crm.mapreduce.domain.user.Uid;
import ru.yandex.market.crm.mapreduce.domain.user.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.models.ProductView;
import ru.yandex.market.mcrm.utils.PropertiesProvider;

import static ru.yandex.market.crm.campaign.test.utils.OfflineSegmentatorTestHelper.pair;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.segment;
import static ru.yandex.market.crm.core.test.SegmentTestUtils.serfMarketFilter;

public class SerfMarketFilterTest extends AbstractServiceLargeTest {

    @Inject
    private OfflineSegmentatorTestHelper segmentatorTestHelper;

    @Inject
    private PropertiesProvider propertiesProvider;

    @Inject
    private YtSchemaTestHelper ytSchemaTestHelper;

    private YPath productViewTable;
    private YPath allModelsTable;

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

        productViewTable = YPath.simple(propertiesProvider.get("var.platform_product_view"));
        allModelsTable = YPath.simple(propertiesProvider.get("var.mbo_all_models"));

        ytSchemaTestHelper.prepareFactsTable(productViewTable, "product_view_table.yson", ProductView.getDescriptor());
        ytSchemaTestHelper.createTable(allModelsTable, "mbo_all_models_table.yson");
    }

    @Test
    public void testFilterByModelName() throws Exception {
        prepareProductViewTable(
                productView(101L, 1L),
                productView(102L, 2L),
                productView(103L, 3L),
                productView(104L, 4L)
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1001L),
                model("Автомобиль BMW", 2, 1002L),
                model("Автомобиль НИВА синий", 3, 1003L),
                model("Автомобиль Жигули красный", 4, 1004L)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(102L)),
                pair(Uid.asPuid(103L))
        );

        Segment segment = segment(
                serfMarketFilter(
                        List.of(),
                        List.of(
                                "Автомобиль И синий",
                                "BMW",
                                "Porsche И синий",
                                "Жигули синий",
                                "Автомобиль красный"
                        ),
                        List.of()
                )
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }

    @Test
    public void testFilterByVendorId() throws Exception {
        prepareProductViewTable(
                productView(101L, 1L),
                productView(102L, 2L),
                productView(103L, 3L),
                productView(104L, 4L)
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1001L),
                model("Автомобиль BMW", 2, 1002L),
                model("Автомобиль НИВА синий", 3, 1003L),
                model("Автомобиль Жигули красный", 4, 1003L)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(101L)),
                pair(Uid.asPuid(103L)),
                pair(Uid.asPuid(104L))
        );

        Segment segment = segment(
                serfMarketFilter(
                        List.of(),
                        List.of(),
                        List.of(1001L, 1003L)
                )
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }

    @Test
    public void testFilterByModelNameAndVendorId() throws Exception {
        prepareProductViewTable(
                productView(101L, 1L),
                productView(102L, 2L),
                productView(103L, 3L),
                productView(104L, 4L)
        );

        prepareModelsTable(
                model("Автомобиль Porsche", 1, 1002L),
                model("Автомобиль BMW", 2, 1002L),
                model("Автомобиль НИВА синий", 3, 1003L),
                model("Автомобиль Жигули красный", 4, 1004L)
        );

        Set<OfflineSegmentatorTestHelper.UidPair> expected = Set.of(
                pair(Uid.asPuid(102L))
        );

        Segment segment = segment(
                serfMarketFilter(
                        List.of(),
                        List.of(
                                "Автомобиль И синий",
                                "BMW",
                                "Porsche И синий",
                                "Жигули синий",
                                "Автомобиль красный"
                        ),
                        List.of(1002L)
                )
        );

        segmentatorTestHelper.assertSegmentPairs(
                expected,
                LinkingMode.NONE,
                Set.of(UidType.PUID, UidType.YUID),
                segment
        );
    }

    private void prepareModelsTable(YTreeMapNode... rows) {
        ytClient.write(allModelsTable, YTableEntryTypes.YSON, List.of(rows));
    }

    private void prepareProductViewTable(YTreeMapNode... rows) {
        ytClient.write(productViewTable, YTableEntryTypes.YSON, List.of(rows));
    }

    private YTreeMapNode productView(long puid, long modelId) {
        long creationTime = ZonedDateTime.now(ZoneId.of("Europe/Moscow"))
                .minusDays(5)
                .toInstant()
                .toEpochMilli();

        ProductView.Builder productViewBuilder = ProductView.newBuilder()
                .setId(modelId)
                .setTimestamp(creationTime)
                .setUserIds(UserIds.newBuilder()
                        .setPuid(puid)
                        .build()
                );

        return YTree.mapBuilder()
                .key("id").value(String.valueOf(creationTime))
                .key("id_type").value("puid")
                .key("fact_id").value(String.valueOf(modelId))
                .key("timestamp").value(creationTime)
                .key("fact").value(new YTreeStringNodeImpl(
                        productViewBuilder.build().toByteArray(),
                        null
                ))
                .buildMap();
    }
}
