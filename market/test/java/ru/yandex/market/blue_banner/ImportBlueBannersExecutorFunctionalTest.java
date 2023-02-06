package ru.yandex.market.blue_banner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
import ru.yandex.market.blue_banner.dto.BlueBannersDto;
import ru.yandex.market.blue_banner.dto.SupplierBannerMapper;
import ru.yandex.market.common.bunker.BunkerService;
import ru.yandex.market.common.bunker.loader.BunkerLoader;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.supplier.banner.model.SupplierBanner;
import ru.yandex.market.core.supplier.banner.service.SupplierBannerService;
import ru.yandex.market.mbi.yt.YtCluster;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.shop.FunctionalTest;
import ru.yandex.market.tms.quartz2.model.Executor;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static ru.yandex.market.common.bunker.BunkerService.Version.LATEST;

@DbUnitDataSet(
        before = "BlueBannersTest.before.csv"
)
class ImportBlueBannersExecutorFunctionalTest extends FunctionalTest {

    private static final String TEMPLATE_BANNERS_ROOT_NODE = "/market-mbi/template-banners-functional-test";

    @Autowired
    private BunkerLoader bunkerLoader;
    @Autowired
    private BunkerService bunkerService;
    @Autowired
    private SupplierBannerService supplierBannerService;
    @Autowired
    private Executor importBlueBannersPgExecutor;
    @Autowired
    private YtTemplate bannerYtTemplate;
    @Autowired
    private SupplierBannerMapper supplierBannerMapper;
    @Autowired
    private Clock clock;

    private final YtTables ytTables = mock(YtTables.class);
    private final Cypress cypress = mock(Cypress.class);

    void initYtTemplateForYtSupplierIdsDao(int clusterIndex, Long[] supplierIds) {
        for (YtCluster cluster : bannerYtTemplate.getClusters()) {
            reset(cluster.getYt());
            when(cluster.getYt().cypress()).thenReturn(cypress);
        }

        YtCluster cluster = bannerYtTemplate.getClusters()[clusterIndex];
        Yt yt = cluster.getYt();
        when(yt.tables()).thenReturn(ytTables);

        doAnswer(answer -> {
            Consumer<YTreeMapNode> consumer = answer.getArgument(2);
            for (Long supplierId : supplierIds) {
                YTreeMapNodeImpl yTreeMapNode = new YTreeMapNodeImpl(new OpenHashMap<>());
                yTreeMapNode.put("shop_id", new YTreeIntegerNodeImpl(false, supplierId, null));
                consumer.accept(yTreeMapNode);
            }
            return null;
        }).when(ytTables).read(any(YPath.class), any(YTableEntryType.class), any(Consumer.class));
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersTest_equals.before.csv"
    )
    void testSupplierBannerEquals() throws IOException {
        mockBlueBunkerResponse("get_old_node_content_for_blue_banners_test.json");
        Set<SupplierBanner> dbNodes = supplierBannerService.fetchSupplierBannersBySupplierIdPageIds(1L,
                SupplierBannerService.DEFAULT_BANNER_PAGE_IDS);
        Set<SupplierBanner> jsonNodes = supplierBannerMapper.convertFromDto(List.of(
                bunkerService.getNodeContent("/market-partner/blue-banners", "latest", BlueBannersDto.class)
                        .getContents()));
        assertThat(dbNodes).isEqualTo(jsonNodes);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersTest.before.csv",
            after = "getBlueBannersTest.after.csv"
    )
    void blueBannersImportTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithDifferentPageTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.mapping.after.csv"
    )
    @DisplayName("Полный импорт - привязка к одной строанице")
    void blueBannersImportWithAnotherPageTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_another_page.test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithDifferentPageTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.partialUpdate.mapping.after.csv"
    )
    @DisplayName("Добавляем в частичном обновлении баннер")
    void blueBannersImportWithAnotherPageDiffTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersPgExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_another_page.partial_update.test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.after.csv"
    )
    @DisplayName("Полный импорт - привязка к нескольким строаницам")
    void blueBannersImportWithPagesTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.after.csv"
    )
    @DisplayName("Обновлений нет, связки поставщик-баннер-страница не должны меняться")
    void blueBannersImportWithPagesDiffTest() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersPgExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithDifferentPageTest.partialUpdate2.mapping.after.csv"
    )
    @DisplayName("Обновления есть, несколько связок поставщик-баннер-страница должны удалиться, т.к. баннер исчез из" +
            " бункера")
    void blueBannersImportWitshPagesDiffTest2() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersPgExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DbUnitDataSet(
            before = "getBlueBannersWithPagesTest.mapping.before.csv",
            after = "getBlueBannersWithPagesTest.mapping.newPage.after.csv"
    )
    @DisplayName("Обновления есть, одна связка поставщик-баннер-страница должна измениться")
    void blueBannersImportWithPagesDiff2Test() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_pages.test.json");
        importBlueBannersPgExecutor.doJob(null);

        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_new_page.test.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Нет списка supplierId, но есть ссылка на ыть")
    @DbUnitDataSet(
            before = "blueBannersImportFromYtWithNoSupplierIds.before.csv",
            after = "blueBannersImportFromYtWithNoSupplierIds.after.csv"
    )
    void blueBannersImportFromYtWithNoSupplierIds() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_yt_only.json");
        initYtTemplateForYtSupplierIdsDao(0, new Long[]{100000000001L});
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Есть и список supplierId, и ссылка на ыть, которая и выигрывает")
    @DbUnitDataSet(
            before = "blueBannersImportFromYtWithSomeSupplierIds.before.csv",
            after = "blueBannersImportFromYtWithSomeSupplierIds.after.csv"
    )
    void blueBannersImportFromYtWithSomeSupplierIds() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_yt_and_suppliers.json");
        initYtTemplateForYtSupplierIdsDao(0, new Long[]{100000000001L});
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Ссылка на ыть дает 0 записей, в этом случае не должны ничего показывать")
    @DbUnitDataSet(
            before = "blueBannersImportFromYtWithSomeSupplierIds.before.csv",
            after = "emptyBanners.after.csv"
    )
    void blueBannersImportFromYtWithoutRows() throws IOException {
        mockBlueBunkerResponse("get_node_content_for_blue_banners_with_yt_and_suppliers.json");
        initYtTemplateForYtSupplierIdsDao(0, new Long[]{});
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Импорт бизнес баннеров")
    @DbUnitDataSet(
            before = "getBusinessBannersTest.before.csv",
            after = "getBusinessBannersTest.after.csv"
    )
    void businessBannersImportTest() throws IOException {
        mockBunkerResponses(Map.of("/market-partner/business-marketplace-banners",
                "get_node_content_for_business_banners_test.json"));
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Импорт бизнес и синих баннеров одновременно")
    @DbUnitDataSet(
            before = "getBlueAndBusinessBannersTest.before.csv",
            after = "getBlueAndBusinessBannersTest.after.csv"
    )
    void supplierAndBusinessBannersImportTest() throws IOException {
        mockBunkerResponses(Map.of("/market-partner/business-marketplace-banners",
                "get_node_content_for_business_banners_test.json",
                "/market-partner/blue-banners", "get_node_content_for_blue_banners_test.json"));
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Проверка импорта баннеров с учетом времени показа")
    @DbUnitDataSet(after = "checkImportByVisibleTime.after.csv")
    void checkImportByVisibleTime() throws IOException {
        when(clock.instant()).thenReturn(Instant.parse("2022-01-15T00:00:00.000Z"));
        mockBlueBunkerResponse("get_node_content_for_blue_banners_check_import_visible_time.json");
        importBlueBannersPgExecutor.doJob(null);
    }

    @Test
    @DisplayName("Импорт шаблонизированных баннеров")
    @DbUnitDataSet(
            before = "templateBannersImportTest.before.csv",
            after = "templateBannersImportTest.after.csv"
    )
    void templateBannersImportTest() throws IOException {
        mockTemplateBannersBunkerResponse(Map.of(
                "banner-dropoff-off", "get_node_content_for_template_banners_dropoff-off.json",
                "banner-dropoff-off-new", "get_node_content_for_template_banners_dropoff-off-new.json"
        ));
        importBlueBannersPgExecutor.doJob(null);
    }

    private void mockBunkerResponses(Map<String, String> resources) throws IOException {
        Set<String> knownNodes = new HashSet<>(
                Set.of("/market-partner/blue-banners", "/market-partner/business-marketplace-banners"));
        when(bunkerLoader.getListOfChildNodesStream(TEMPLATE_BANNERS_ROOT_NODE, LATEST))
                .thenReturn(new ByteArrayInputStream("[]".getBytes()));
        knownNodes.addAll(resources.values());
        for (String knownNode : knownNodes) {
            doReturn(getClass().getResourceAsStream(resources.getOrDefault(knownNode, "empty.json")))
                    .when(bunkerLoader)
                    .getNodeStream(knownNode, LATEST);
        }
    }

    private void mockBlueBunkerResponse(String resource) throws IOException {
        mockBunkerResponses(Map.of("/market-partner/blue-banners", resource));
    }

    private void mockTemplateBannersBunkerResponse(Map<String, String> resources) throws IOException {
        mockBunkerResponses(Collections.emptyMap());

        String template = "{\"name\":\"{nodeName}\"," +
                "\"fullName\":\"" + TEMPLATE_BANNERS_ROOT_NODE + "/{nodeName}\"," +
                "\"version\":1," +
                "\"mime\":null," +
                "\"saveDate\":\"2022-01-20T06:57:08.000Z\"," +
                "\"publishDate\":\"2022-01-20T06:57:08.000Z\"," +
                "\"flushDate\":\"2022-01-20T06:57:08.000Z\"," +
                "\"deleted\":false," +
                "\"isDeleted\":false}";
        String bunkerNodesList = resources.keySet().stream()
                .map(nodeName -> template.replace("{nodeName}", nodeName))
                .collect(Collectors.joining(",", "[", "]"));
        when(bunkerLoader.getListOfChildNodesStream(TEMPLATE_BANNERS_ROOT_NODE, LATEST))
                .thenReturn(new ByteArrayInputStream(bunkerNodesList.getBytes(StandardCharsets.UTF_8)));

        for (Map.Entry<String, String> entry : resources.entrySet()) {
            String node = entry.getKey();
            String contentFile = entry.getValue();
            when(bunkerLoader.getNodeStream(TEMPLATE_BANNERS_ROOT_NODE + "/" + node, LATEST))
                    .thenReturn(getClass().getResourceAsStream(contentFile));

        }
    }
}
