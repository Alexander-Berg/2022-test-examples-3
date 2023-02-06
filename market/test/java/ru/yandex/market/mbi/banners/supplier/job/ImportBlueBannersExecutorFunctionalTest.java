package ru.yandex.market.mbi.banners.supplier.job;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeIntegerNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.tables.YTableEntryType;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.common.bunker.BunkerService;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.banners.FunctionalTest;
import ru.yandex.market.mbi.banners.supplier.dto.BlueBannersDto;
import ru.yandex.market.mbi.banners.supplier.dto.SupplierBannerMapper;
import ru.yandex.market.mbi.banners.supplier.model.SupplierBanner;
import ru.yandex.market.mbi.banners.supplier.service.SupplierBannerService;
import ru.yandex.market.mbi.banners.yt.YtCluster;
import ru.yandex.market.mbi.banners.yt.YtTemplate;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@Disabled
@DbUnitDataSet(
        before = "BlueBannersTest.before.csv",
        dataSource = "mbiDbDataSource"
)
class ImportBlueBannersExecutorFunctionalTest extends FunctionalTest {
    @Autowired
    private BunkerLoader bunkerLoader;
    @Autowired
    private BunkerService bunkerService;
    @Autowired
    private SupplierBannerService supplierBannerService;
    @Autowired
    private ImportBlueBannersExecutor importBlueBannersExecutor;
    @Autowired
    private YtTemplate bannerYtTemplate;

    private final YtTables ytTables = mock(YtTables.class);
    private final Cypress cypress = mock(Cypress.class);

    void initYtTemplateForYtSupplierIdsDao(int clusterIndex, Long[] supplierIds) {
        for(YtCluster cluster: bannerYtTemplate.getClusters()) {
            reset(cluster.getYt());
            when(cluster.getYt().cypress()).thenReturn(cypress);
        }

        YtCluster cluster = bannerYtTemplate.getClusters()[clusterIndex];
        Yt yt = cluster.getYt();
        when(yt.tables()).thenReturn(ytTables);

        doAnswer(answer -> {
            Consumer<YTreeMapNode> consumer = answer.getArgument(2);
            for (Long supplierId: supplierIds) {
                YTreeMapNodeImpl yTreeMapNode = new YTreeMapNodeImpl(new OpenHashMap<>());
                yTreeMapNode.put("shop_id", new YTreeIntegerNodeImpl(false, supplierId, null));
                consumer.accept(yTreeMapNode);
            }
            return null;
        }).when(ytTables).read(any(YPath.class), any(YTableEntryType.class), any(Consumer.class));
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersTest_equals.before.csv",
            dataSource = "mbiDbDataSource"
    )
    void testSupplierBannerEquals() throws IOException {
        mockBlueBunkerResponse("get_old_node_content_for_blue_banners_test.json");
        Set<SupplierBanner> dbNodes = supplierBannerService.fetchSupplierBannersBySupplierIdPageIds(1L,
                SupplierBannerService.DEFAULT_BANNER_PAGE_IDS);
        Set<SupplierBanner> jsonNodes = SupplierBannerMapper.convertFromDto(
                bunkerService.getNodeContent("/market-partner/blue-banners", "latest", BlueBannersDto.class)
                        .getContents());
        assertThat(dbNodes).isEqualTo(jsonNodes);
    }


    @Test
    @DbUnitDataSet(
            before = "getBlueBannersTest.before.csv",
            after = "getBlueBannersTest.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void blueBannersImportTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersExecutor.doJob(null);
    }


    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithDifferentPageTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.mapping.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Полный импорт - привязка к одной строанице")
    void blueBannersImportWithAnotherPageTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_another_page.test.json");
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithDifferentPageTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.partialUpdate.mapping.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Добавляем в частичном обновлении баннер")
    void blueBannersImportWithAnotherPageDiffTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_another_page.partial_update.test.json");
        importBlueBannersExecutor.doJob(null);
    }


    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Полный импорт - привязка к нескольким строаницам")
    void blueBannersImportWithPagesTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersExecutor.doJob(null);
    }


    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Обновлений нет, связки поставщик-баннер-страница не должны меняться")
    void blueBannersImportWithPagesDiffTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersExecutor.doJob(null);
    }


    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.partialUpdate2.mapping.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Обновления есть, несколько связок поставщик-баннер-страница должны удалиться, т.к. баннер исчез из" +
            " бункера")
    void blueBannersImportWitshPagesDiffTest2() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.newPage.after.csv",
            dataSource = "mbiDbDataSource"
    )
    @DisplayName("Обновления есть, одна связка поставщик-баннер-страница должна измениться")
    void blueBannersImportWithPagesDiff2Test() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_new_page.test.json");
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DisplayName("Нет списка supplierId, но есть ссылка на ыть")
    @DbUnitDataSet(
            before = "blueBannersImportFromYtWithNoSupplierIds.before.csv",
            after = "blueBannersImportFromYtWithNoSupplierIds.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void blueBannersImportFromYtWithNoSupplierIds() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_yt_only.json");
        initYtTemplateForYtSupplierIdsDao(0, new Long[] {100000000001L});
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DisplayName("Есть и список supplierId, и ссылка на ыть, которая и выигрывает")
    @DbUnitDataSet(
            before = "blueBannersImportFromYtWithSomeSupplierIds.before.csv",
            after = "blueBannersImportFromYtWithSomeSupplierIds.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void blueBannersImportFromYtWithSomeSupplierIds() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_yt_and_suppliers.json");
        initYtTemplateForYtSupplierIdsDao(0, new Long[] {100000000001L});
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DisplayName("Импорт бизнес баннеров")
    @DbUnitDataSet(
            before = "getBusinessBannersTest.before.csv",
            after = "getBusinessBannersTest.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void businessBannersImportTest() throws IOException {
        mockBunkerResponses(Map.of("/market-partner/business-marketplace-banners",
                "get_node_content_for_business_banners_test.json"));
        importBlueBannersExecutor.doJob(null);
    }

    @Test
    @DisplayName("Импорт бизнес и синих баннеров одновременно")
    @DbUnitDataSet(
            before = "getBlueAndBusinessBannersTest.before.csv",
            after = "getBlueAndBusinessBannersTest.after.csv",
            dataSource = "mbiDbDataSource"
    )
    void supplierAndBusinessBannersImportTest() throws IOException {
        mockBunkerResponses(Map.of("/market-partner/business-marketplace-banners",
                "get_node_content_for_business_banners_test.json",
                "/market-partner/blue-banners", "get_node_content_for_blue_banners_test.json"));
        importBlueBannersExecutor.doJob(null);
    }

    private void mockBunkerResponses(Map<String, String> resources) throws IOException {
        Set<String> knownNodes = new HashSet<>(
                Set.of("/market-partner/blue-banners", "/market-partner/business-marketplace-banners"));
        knownNodes.addAll(resources.values());
        for (String knownNode : knownNodes) {
            doReturn(getClass().getResourceAsStream(resources.getOrDefault(knownNode, "empty.json")))
                    .when(bunkerLoader)
                    .getNodeStream(knownNode, BunkerService.Version.LATEST);
        }
    }

    private void mockBlueBunkerResponse(String resource) throws IOException {
        mockBunkerResponses(Map.of("/market-partner/blue-banners", resource));
    }
}
