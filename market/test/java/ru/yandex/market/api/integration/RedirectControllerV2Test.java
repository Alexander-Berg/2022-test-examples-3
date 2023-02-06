package ru.yandex.market.api.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntLists;
import it.unimi.dsi.fastutil.longs.LongLists;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.controller.v2.ParametersV2;
import ru.yandex.market.api.controller.v2.RedirectControllerV2;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ContentType;
import ru.yandex.market.api.domain.v2.FilterField;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PageType;
import ru.yandex.market.api.domain.v2.RedirectContentType;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.SearchPageInfo;
import ru.yandex.market.api.domain.v2.ShopInfoV2;
import ru.yandex.market.api.domain.v2.VendorV2;
import ru.yandex.market.api.domain.v2.criterion.Criterion;
import ru.yandex.market.api.domain.v2.criterion.TextCriterion;
import ru.yandex.market.api.domain.v2.filters.EnumFilter;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.domain.v2.opinion.ShopOpinionV2;
import ru.yandex.market.api.domain.v2.redirect.CatalogRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.ModelRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.RedirectV2;
import ru.yandex.market.api.domain.v2.redirect.ReportRedirectV2Result;
import ru.yandex.market.api.domain.v2.redirect.SearchRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.ShopOpinionRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.SkuRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.VendorRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.content.SearchContent;
import ru.yandex.market.api.domain.v2.redirect.content.ShopOpinionContent;
import ru.yandex.market.api.domain.v2.redirect.content.UnsupportedRedirect;
import ru.yandex.market.api.domain.v2.redirect.parameters.EnrichParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.FilterParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.matchers.CriterionMatcher;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.ApiCollections;
import ru.yandex.market.api.util.CriterionTestUtil;
import ru.yandex.market.api.util.functional.Functionals;
import ru.yandex.market.api.util.httpclient.clients.BukerTestClient;
import ru.yandex.market.api.util.httpclient.clients.CatalogerTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersStaticTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 *
 * Тесты на редиректы, во всех тестах в конце проверяем, что простваили правильный queryText
 * (который должен совпадать с поисковым текстом пользователя без литерала)
 * Created by apershukov on 12.12.16.
 */
@ActiveProfiles(RedirectControllerV2Test.PROFILE)
public class RedirectControllerV2Test extends BaseTest {
    static final String PROFILE = "RedirectControllerV2Test";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private CatalogerTestClient catalogerTestClient;

    @Inject
    private RedirectControllerV2 redirectController;

    @Inject
    private BukerTestClient bukerTestClient;

    @Inject
    private PersStaticTestClient persStaticTestClient;

    @Inject
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    private static final Client PARTNER = new Client() {{
        setType(Type.EXTERNAL);
    }};

    private static final Client MOBILE = new Client() {{
        setType(Type.MOBILE);
    }};

    private static final Client MOBILE_BLUE = new Client() {{
        setType(Type.MOBILE);
    }};

    private static final Map<Client.Type, Collection<? extends ContentType>> AVAILABLE_CONTENT_TYPES =
        ImmutableMap.<Client.Type, Collection<? extends ContentType>>builder()
            .put(
                Client.Type.EXTERNAL,
                Arrays.asList(
                    RedirectContentType.SEARCH,
                    RedirectContentType.CATALOG,
                    RedirectContentType.MODEL,
                    RedirectContentType.VENDOR,
                    RedirectContentType.SHOP_OPINION
                )
            )
            .put(
                Client.Type.INTERNAL,
                Arrays.asList(
                    RedirectContentType.SEARCH,
                    RedirectContentType.CATALOG,
                    RedirectContentType.MODEL,
                    RedirectContentType.VENDOR,
                    RedirectContentType.SHOP_OPINION,
                    RedirectContentType.PROMO_PAGE,
                    RedirectContentType.ARTICLE,
                    RedirectContentType.COLLECTION
                )
            )
            .put(
                Client.Type.MOBILE,
                Arrays.asList(
                    RedirectContentType.SEARCH,
                    RedirectContentType.CATALOG,
                    RedirectContentType.MODEL,
                    RedirectContentType.VENDOR,
                    RedirectContentType.SHOP_OPINION,
                    RedirectContentType.PROMO_PAGE,
                    RedirectContentType.ARTICLE,
                    RedirectContentType.COLLECTION,
                    RedirectContentType.SKU
                )
            )
            .build();

    private static final Map<Client.Type, Collection<PageType>> AVAILABLE_PAGE_TYPE =
        ImmutableMap.<Client.Type, Collection<PageType>>builder()
            .put(
                Client.Type.EXTERNAL,
                Arrays.asList(
                    PageType.SEARCH,
                    PageType.CATALOG,
                    PageType.MODEL,
                    PageType.VENDOR,
                    PageType.SHOP_OPINION
                )
            )
            .put(
                Client.Type.INTERNAL,
                Arrays.asList(
                    PageType.SEARCH,
                    PageType.CATALOG,
                    PageType.MODEL,
                    PageType.VENDOR,
                    PageType.SHOP_OPINION
                )
            )
            .put(
                Client.Type.MOBILE,
                Arrays.asList(
                    PageType.SEARCH,
                    PageType.CATALOG,
                    PageType.MODEL,
                    PageType.VENDOR,
                    PageType.SHOP_OPINION,
                    PageType.SKU,
                    PageType.UNSUPPORTED
                )
            )
            .build();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.setCurrencyRepresentationRule(null);
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }


    private RedirectV2 catalogSupport_reportReturnModelRedirect_prepare(Client client, Version version) {
        context.setClient(client);
        context.setVersion(version);

        String searchText = "ASUS Midas Backpack 16";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Arrays.asList(PageType.CATALOG, PageType.SEARCH), Collections.emptyList());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_asus_midas_backpack_16.json");

        return getRedirect(userParams, filterParams, enrichParams, genericParams);
    }

    @Test
    public void partner_V2_0_0_catalogSupport_reportReturnModelRedirect_waitSearchRedirect() {
        RedirectV2 result = catalogSupport_reportReturnModelRedirect_prepare(PARTNER, Version.V2_0_0);
        assertEquals(PageType.SEARCH, result.getRedirectType());
        assertTrue(result instanceof SearchRedirectV2);

        assertEquals("ASUS Midas Backpack 16", result.getQueryText());
    }

    @Test
    public void mobile_V2_0_10_catalogSupport_reportReturnModelRedirect_waitUnsupportedRedirect() {
        RedirectV2 result = catalogSupport_reportReturnModelRedirect_prepare(MOBILE, Version.V2_0_10);
        assertEquals(PageType.UNSUPPORTED, result.getRedirectType());
        assertTrue(result instanceof UnsupportedRedirect);

        assertEquals("https://m.market.yandex.ru/search?cvredirect=1&text=ASUS+Midas+Backpack+16&pp=37", result.getLink());
        assertEquals("ASUS Midas Backpack 16", result.getQueryText());
    }

    private RedirectV2 catalogSupport_reportReturnVendorRedirect_prepare(Client client, Version version) {
        context.setClient(client);
        context.setVersion(version);

        String searchText = "ASUS";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Arrays.asList(PageType.CATALOG, PageType.SEARCH), Collections.emptyList());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_vendor_asus.json");

        return getRedirect(userParams, filterParams, enrichParams, genericParams);
    }

    @Test
    public void partner_V2_0_0_catalogSupport_reportReturnVendorRedirect_waitSearchRedirect() {
        RedirectV2 result = catalogSupport_reportReturnVendorRedirect_prepare(PARTNER, Version.V2_0_0);

        assertEquals(PageType.SEARCH, result.getRedirectType());
        assertTrue(result instanceof SearchRedirectV2);

        assertEquals("ASUS", result.getQueryText());
    }

    @Test
    public void mobile_V2_0_10_catalogSupport_reportReturnVendorRedirect_waitUnsupportedRedirect() {
        RedirectV2 result = catalogSupport_reportReturnVendorRedirect_prepare(MOBILE, Version.V2_0_10);

        assertEquals(PageType.UNSUPPORTED, result.getRedirectType());
        assertTrue(result instanceof UnsupportedRedirect);

        assertEquals("https://m.market.yandex.ru/search?cvredirect=1&text=ASUS&pp=37", result.getLink());
        assertEquals("ASUS", result.getQueryText());
    }

    private RedirectV2 modelSupport_reportReturnCatalogRedirect_prepare(Client client, Version version) {
        context.setClient(client);
        context.setVersion(version);

        String searchText = "Электроника";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Arrays.asList(PageType.MODEL));
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_electronics.json");

        return getRedirect(userParams, filterParams, enrichParams, genericParams);
    }


    @Test
    public void partner_V2_0_0_modelSupport_reportReturnCatalogRedirect_waitSearchRedirect() {
        RedirectV2 redirect = modelSupport_reportReturnCatalogRedirect_prepare(PARTNER, Version.V2_0_0);

        assertEquals(PageType.SEARCH, redirect.getRedirectType());
        assertNull(redirect.getContent());
        assertEquals("Электроника", redirect.getQueryText());
    }

    @Test
    public void mobile_V2_0_10_modelSupport_reportReturnCatalogRedirect_waitUnsupportedRedirect() {
        RedirectV2 redirect = modelSupport_reportReturnCatalogRedirect_prepare(MOBILE, Version.V2_0_10);

        assertEquals(PageType.UNSUPPORTED, redirect.getRedirectType());
        assertNull(redirect.getContent());
        assertEquals("https://m.market.yandex.ru/search?cvredirect=1" +
            "&text=%D0%AD%D0%BB%D0%B5%D0%BA%D1%82%D1%80%D0%BE%D0%BD%D0%B8%D0%BA%D0%B0&pp=37", redirect.getLink());
        assertEquals("Электроника", redirect.getQueryText());
    }

    private RedirectV2 catalogSupport_reportReturnCatalogRedirectToRoot_prepare(Client client,
                                                                                Version version) {
        context.setClient(client);
        context.setVersion(version);

        String searchText = "Электроника";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Arrays.asList(PageType.CATALOG));
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_electronics.json");
        catalogerTestClient.getChildren(54440, "tree_depth_1_electronics.xml");

        return getRedirect(userParams, filterParams, enrichParams, genericParams);
    }

    @Test
    public void partner_V2_0_0_notValidSign_catalogSupport_reportReturnCatalogRedirectToRoot_waitCatalogRedirect() {
        RedirectV2 redirect = catalogSupport_reportReturnCatalogRedirectToRoot_prepare(PARTNER, Version.V2_0_0);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());
        assertNull(redirect.getContent());
        assertEquals("Электроника", redirect.getQueryText());
    }

    @Test
    public void mobile_V2_0_0_notValidSign_catalogSupport_reportReturnCatalogRedirectToRoot_waitCatalogRedirect() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        RedirectV2 redirect = catalogSupport_reportReturnCatalogRedirectToRoot_prepare(MOBILE, Version.V2_0_0);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());
        assertNull(redirect.getContent());
        assertEquals("Электроника", redirect.getQueryText());
    }

    private RedirectV2 allSupport_reportReturnUnknownRedirect_prepare(Client client, Version version) {
        context.setClient(client);
        context.setVersion(version);

        String searchText = "Неизвестность";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_unknown.json");

        return getRedirect(userParams, filterParams, enrichParams, genericParams);
    }

    @Test
    public void partner_V2_0_0_allSupport_reportReturnUnknownRedirect_waitSearchRedirect() {
        RedirectV2 redirectV2 = allSupport_reportReturnUnknownRedirect_prepare(PARTNER, Version.V2_0_0);

        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertEquals("Неизвестность", redirectV2.getQueryText());
    }

    @Test
    public void mobile_V2_0_0_allSupport_reportReturnUnknownRedirect_waitSearchRedirect() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        RedirectV2 redirectV2 = allSupport_reportReturnUnknownRedirect_prepare(MOBILE, Version.V2_0_0);

        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertEquals("Неизвестность", redirectV2.getQueryText());
    }


    @Test
    public void partner_V2_0_10_allSupport_reportReturnUnknownRedirect_waitSearchRedirect() {
        RedirectV2 redirectV2 = allSupport_reportReturnUnknownRedirect_prepare(PARTNER, Version.V2_0_10);

        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertEquals("Неизвестность", redirectV2.getQueryText());
    }

    @Test
    public void mobile_V2_0_10_allSupport_reportReturnUnknownRedirect_waitUnsupportedRedirect() {
        RedirectV2 redirectV2 = allSupport_reportReturnUnknownRedirect_prepare(MOBILE, Version.V2_0_10);

        assertEquals(PageType.UNSUPPORTED, redirectV2.getRedirectType());
        assertEquals("https://m.market.yandex.ru/search?cvredirect=1" +
            "&text=%D0%9D%D0%B5%D0%B8%D0%B7%D0%B2%D0%B5%D1%81%D1%82%D0%BD%D0%BE%D1%81%D1%82%D1%8C&pp=37", redirectV2.getLink());
        assertEquals("Неизвестность", redirectV2.getQueryText());
    }

    @Test
    public void partner_allSupport_allContent_reportReturnShopOpinionRedirect_waitShopOpinionRedirect_andContentWithShopIds() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);
        context.setClient(PARTNER);

        String searchText = "Связной отзывы";
        long shopId = 3828;

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(),
            getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_shop_opinion.json");
        persStaticTestClient.getShopOpinion(shopId, "shop_opinion_3828.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SHOP_OPINION, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof ShopOpinionRedirectV2);

        ShopInfoV2.ShopId shop = new ShopInfoV2.ShopId(3828);

        ShopOpinionRedirectV2 shopOpinionRedirectV2 = (ShopOpinionRedirectV2) redirectV2;

        assertEquals(shop, shopOpinionRedirectV2.getShop());

        ShopOpinionContent content = shopOpinionRedirectV2.getContent();
        assertEquals(PageInfo.DEFAULT, content.getPagedResult().getPageInfo());

        Collection<Long> shopIds = Arrays.asList(58021335L,
                58022499L,
                58025223L,
                58028519L,
                58028941L,
                58030120L,
                58030467L,
                58032034L,
                58032886L,
                58033612L
        );
        assertTrue(
            Iterables.elementsEqual(
                shopIds,
                Collections2.transform(content.getItems(), ShopOpinionV2::getId)
            )
        );

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void parnter_allSupport_reportReturnCatalogRedirectWithRsAndGlFilter_waitCatalogRedirect_andRsInTextCriteria_andGlFilterInCriteria() {
        context.setClient(PARTNER);

        String searchText = "красное платье";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_parametric_red_dress.json");
        catalogerTestClient.getChildren(57297, "tree_depth_1_woman_dresses.xml");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        Assert.assertEquals(PageType.CATALOG, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof CatalogRedirectV2);

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirectV2;

        Criterion parametricCriterion = new Criterion("7925349", "7925352",
            Criterion.CriterionType.GLFILTER);
        TextCriterion textCriterion = new TextCriterion(searchText, searchText,
            "eJxT0uKSvrDrYsOFDRcbL-y9sO_CVoUL-y_sBnKbLvZc2Crw9M5jZiUWDgYBdg0GADw4FtI,");

        CriterionTestUtil.assertCriterionEquals(
            Arrays.asList(parametricCriterion, textCriterion),
            catalogRedirect.getCriteria());
        assertEquals(searchText, redirectV2.getQueryText());

    }


    @Test
    public void mobile_allSupport_allContent_reportReturnCatalogRedirectWithHighlightedText_waitCatalogRedirect_andHighlightedText() {
        context.setClient(MOBILE);
        String searchText = "красное платье";
        String highlightedText = "[красное] платье";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(EnumSet.allOf(PageType.class),
            EnumSet.allOf(RedirectContentType.class));
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_parametric_red_dress.json");
        catalogerTestClient.getChildren(57297, "tree_depth_1_woman_dresses.xml");
        reportTestClient.searchV2withoutCvRedirect(searchText, "search_red_dress.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        Assert.assertEquals(PageType.CATALOG, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof CatalogRedirectV2);

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirectV2;

        Criterion parametricCriterion = new Criterion("7925349", "7925352",
            Criterion.CriterionType.GLFILTER);
        TextCriterion textCriterion = new TextCriterion(searchText, highlightedText,
            "eJxT0uKSvrDrYsOFDRcbL-y9sO_CVoUL-y_sBnKbLvZc2Crw9M5jZiUWDgYBdg0GADw4FtI,");

        CriterionTestUtil.assertCriterionEquals(
            Arrays.asList(parametricCriterion, textCriterion),
            catalogRedirect.getCriteria());

        SearchContent searchContent = catalogRedirect.getContent();
        assertNotNull(searchContent);

        assertTrue(searchContent.getPagedResult().getPageInfo() instanceof SearchPageInfo);
        SearchPageInfo searchPageInfo = (SearchPageInfo) searchContent
            .getPagedResult().getPageInfo();
        assertEquals(898, searchPageInfo.getTotalItems());

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void mobile_allSupport_allContent_reportReturnCatalogRedirectWithHighlightedText_waitCatalogRedirect_andIdsInContent() {
        context.setClient(MOBILE);
        String searchText = "красное платье";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(),
            getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_parametric_red_dress.json");
        catalogerTestClient.getChildren(57297, "tree_depth_1_woman_dresses.xml");
        reportTestClient.searchV2withoutCvRedirect(searchText, "search_red_dress.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        Assert.assertEquals(PageType.CATALOG, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof CatalogRedirectV2);

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirectV2;

        SearchContent searchContent = catalogRedirect.getContent();
        assertNotNull(searchContent);

        assertTrue(searchContent.getPagedResult().getPageInfo() instanceof SearchPageInfo);
        SearchPageInfo searchPageInfo = (SearchPageInfo) searchContent
            .getPagedResult().getPageInfo();
        assertEquals(898, searchPageInfo.getTotalItems());

        Collection<Long> modelIds = Arrays.asList(1359986293L, 1365239103L, 1375396294L,
            1376801755L, 1377071643L, 1380176787L, 1381077098L, 1378965498L);

        Collection<OfferId> offerIds = Arrays.asList(
            new OfferId("id1", "fee1"),
            new OfferId("id2", "fee2")
        );

        Assert.assertTrue(Iterables.elementsEqual(
            modelIds,
            Collections2.transform(
                ApiCollections.filter(
                    searchContent.getPagedResult().getElements(),
                    ModelV2.class),
                ModelV2::getId
            )
            )
        );

        Assert.assertTrue(Iterables.elementsEqual(
            offerIds,
            Collections2.transform(
                ApiCollections.filter(
                    searchContent.getPagedResult().getElements(),
                    OfferV2.class
                ),
                OfferV2::getId
            )
        ));


        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void partner_allSupport_allContent_reportReturnModelRedirect_waitModelRedirect_andNameAndDescriptionInContent() {
        context.setClient(PARTNER);

        String searchText = "ASUS Midas Backpack 16";
        long modelId = 10573708;

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(),
            getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(
            Collections.singleton(ModelInfoField.CATEGORY));

        reportTestClient.redirect(searchText, "redirect_asus_midas_backpack_16.json");
        reportTestClient.getModelInfoById(modelId, "modelinfo10573708.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.MODEL, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof ModelRedirectV2);

        ModelV2.ModelId model = new ModelV2.ModelId(10573708);

        ModelRedirectV2 modelRedirectV2 = (ModelRedirectV2) redirectV2;
        assertEquals(model, modelRedirectV2.getModel());

        ModelV2 modelV2 = modelRedirectV2.getContent().getModel();


        assertEquals(modelId, modelV2.getId());
        assertEquals("ASUS Midas Backpack 16", modelV2.getName());
        assertEquals("рюкзак, макс. размер экрана 16\", материал: синтетический",
            modelV2.getDescription());
        assertEquals(91076, modelV2.getCategory().getId());

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void partner_allSupport_allContent_reportReturnVendorRedirect_waitVendorRedirect_andNameInContent() {
        context.setClient(PARTNER);

        String searchText = "ASUS";
        long vendorId = 152863L;

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(),
            getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_vendor_asus.json");
        catalogerTestClient.getVendors(LongLists.singleton(vendorId), "cataloger_vendor_152863.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.VENDOR, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof VendorRedirectV2);

        VendorV2.VendorId vendor = new VendorV2.VendorId(vendorId);

        VendorRedirectV2 vendorRedirectV2 = (VendorRedirectV2) redirectV2;

        assertEquals(vendor, vendorRedirectV2.getVendor());

        VendorV2 vendorV2 = vendorRedirectV2.getContent().getVendor();

        assertEquals(vendorId, vendorV2.getId());
        assertEquals("ASUS", vendorV2.getName());

        assertEquals(searchText, redirectV2.getQueryText());
    }


    @Test
    public void allSupport_reportReturnSearchRedirectWithTextButWithoutHid_waitSearchRedirectWithSameText() {
        String searchText = "триммер";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_search_without_hid.json");
        reportTestClient.searchV2(searchText, "search_trimmer.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, searchText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria());

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void partner_searchSupport_allContent_reportReturnTextSearch_waitSearchRedirect_andIdsInContent_andValidPageInfo_andTextCriteria() {
        context.setClient(PARTNER);

        String searchText = "нечто";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Arrays.asList(PageType.SEARCH),
            getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_search_nechto.json");


        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);

        SearchRedirectV2 searchRedirectV2 = (SearchRedirectV2) redirectV2;
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(
                new TextCriterion(searchText, searchText, null)
            ),
            searchRedirectV2.getCriteria()
        );

        SearchContent content = ((SearchRedirectV2) redirectV2).getContent();
        assertNotNull(content);

        assertTrue(content.getPagedResult().getPageInfo() instanceof SearchPageInfo);
        SearchPageInfo searchPageInfo = (SearchPageInfo) content.getPagedResult().getPageInfo();
        assertEquals(235, searchPageInfo.getTotalItems());

        Collection<Long> modelIds = Arrays.asList(1366363313L, 1366357951L);
        Collection<OfferId> offerIds = Arrays.asList(
            new OfferId("id1", "fee1"),
            new OfferId("id2", null),
            new OfferId("id3", "fee3"),
            new OfferId("id4", "fee4"),
            new OfferId("id5", "fee5"),
            new OfferId("id6", "fee6"),
            new OfferId("id7", "fee7"),
            new OfferId("id8", "fee8")
        );

        assertTrue(
            Iterables.elementsEqual(
                modelIds,
                Collections2.transform(
                    ApiCollections.filter(
                        content.getItems(),
                        ModelV2.class
                    ),
                    ModelV2::getId
                )
            )
        );

        assertTrue(
            Iterables.elementsEqual(
                offerIds,
                Collections2.transform(
                    ApiCollections.filter(
                        content.getItems(),
                        OfferV2.class
                    ),
                    OfferV2::getId
                )
            )
        );

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void partner_nothingSupport_reportReturnVendorRedirect_waitSearchRedirectByOriginalText() {
        context.setClient(PARTNER);

        String searchText = "asus";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Collections.emptyList());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "redirect_vendor_asus.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);

        assertEquals(searchText, redirectV2.getQueryText());

    }

    @Test
    public void partner_allSupport_reportReturnEmptyTextSearch_waitSearchRedirect_andOriginalTextInTextCriterion() {
        String searchText = "asdfd";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "empty_search.json");

        context.setPpList(IntLists.EMPTY_LIST);
        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, searchText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria()
        );
        assertEquals("https://market.yandex.ru/search?text=" + searchText, redirectV2.getLink());

        assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void barcodeSearch_reportReturnEmptyTextSearch_waitSearchRedirect_andTextCriterionValueContainsBarcode_andTextCriterionTextAndQueryTextNotContainsBarcode() {
        String showText = "123";
        String searchText = SearchType.BARCODE.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.BARCODE);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "empty_search.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, showText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria()
        );

        assertEquals(showText, redirectV2.getQueryText());
    }

    @Test
    public void barcodeSearch_reportReturnModelRedirect_waitModelRedirect_andQueryTextNotContainsBarcode() {
        String showText = "5099206041271";
        String searchText = SearchType.BARCODE.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.BARCODE);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "model_barcode_5099206041271.json");
        reportTestClient.getModelInfoById(7896561, "modelinfo7896561.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.MODEL, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof ModelRedirectV2);
        assertEquals(7896561, ((ModelRedirectV2) redirectV2).getModel().getId());

        assertEquals(showText, redirectV2.getQueryText());

    }

    @Test
    public void searchBarcode_reportReturnTextSearchWithEmptyContent_waitSearchRedirect_andQueryTextDoesNotContainBarcode() {
        String showText = "9785496008938";
        String searchText = SearchType.BARCODE.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.BARCODE);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "search_barcode_9785496008938.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, showText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria()
        );

        assertEquals(showText, redirectV2.getQueryText());
    }

    @Test
    public void isbnSearch_reportReturnEmptyTextSearch_waitSearchRedirect_andTextCriterionValueContainsIsbn_andTextCriterionTextAndQueryTextNotContainsIsbn() {
        String showText = "9785496008938";
        String searchText = SearchType.ISBN.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.ISBN);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "search_barcode_9785496008938.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, showText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria()
        );

        assertEquals(showText, redirectV2.getQueryText());
    }

    @Test
    public void isbnSearch_reportReturnModelRedirect_waitModelRedirect_andQueryTextNotContainsIsbn() {
        String showText = "9785496008938";
        String searchText = SearchType.ISBN.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.ISBN);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "model_barcode_5099206041271.json");
        reportTestClient.getModelInfoById(7896561, "modelinfo7896561.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.MODEL, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof ModelRedirectV2);
        assertEquals(7896561, ((ModelRedirectV2) redirectV2).getModel().getId());

        assertEquals(showText, redirectV2.getQueryText());

    }


    @Test
    public void isbnSearch_reportReturnTextSearchWithEmptyContent_waitSearchRedirect_andQueryTextDoesNotContainIsbn() {
        String showText = "123";
        String searchText = SearchType.ISBN.getLiteral(showText);

        SearchQuery userParams = userParams(showText, SearchType.ISBN);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        reportTestClient.redirect(searchText, "empty_search.json");

        RedirectV2 redirectV2 = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        assertTrue(redirectV2 instanceof SearchRedirectV2);
        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion(searchText, showText, null)),
            ((SearchRedirectV2) redirectV2).getCriteria()
        );

        assertEquals(showText, redirectV2.getQueryText());
    }

    @Test
    public void partner_noAdult_allSupport_searchContent_reportReturnRedirectToAdultCategory_waitSearchRedirect_andValidPageInfo_andOriginalTextInTextCriterion() {
        context.setClient(PARTNER);
        context.setSections(Collections.singleton(Parameters.Section.MEDICINE));

        String searchText = "Презервативы";

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(),
            Collections.singleton(RedirectContentType.SEARCH));
        EnrichParams enrichParams = enrichParams(
            ParametersV2.REDIRECT_FIELDS_2.getItems()
                .stream()
                .flatMap(item -> item.getValues().stream())
                .collect(Collectors.toList())
        );

        reportTestClient.redirect(searchText, "redirect_to_adult_category.json");
        reportTestClient.searchV2(searchText, "search_adult.json");

        ReportRedirectV2Result result = getResult(userParams, filterParams, enrichParams, genericParams);

        RedirectV2 redirectV2 = result.getRedirect();

        assertEquals(redirectV2.getRedirectType(), PageType.SEARCH);
        assertTrue(redirectV2 instanceof SearchRedirectV2);

        SearchRedirectV2 searchRedirectV2 = (SearchRedirectV2) redirectV2;

        CriterionTestUtil.assertCriterionEquals(
            Collections.singleton(new TextCriterion("Презервативы", "Презервативы", null)),
            searchRedirectV2.getCriteria()
        );

        assertNotNull(searchRedirectV2.getContent());
        assertTrue(searchRedirectV2.getContent() instanceof SearchContent);

        ResultContextV2 contextV2 = (ResultContextV2) result.getContext();
        assertTrue(contextV2.getPage() instanceof SearchPageInfo);
        SearchPageInfo pageInfo = (SearchPageInfo) contextV2.getPage();
        assertEquals(221, pageInfo.getTotalItems());

        assertEquals(redirectV2.getQueryText(), "Презервативы");
    }

    @Test
    public void catalogRedirectHidInsteadNid_waitCorrectCatalogRedirect() {
        String searchText = "Утюги Philips";

        reportTestClient.redirect(searchText, "redirect_to_iron_hid_instead_nid.json");
        catalogerTestClient.getChildren(54915, "tree_depth_1_irons.xml");
        catalogerTestClient.getPath(90568, "path_hid_90568.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertCatalogRedirectIsCorrect_whenReportRedirectWithoutNidOrHid(searchText, redirect);
    }

    @Test
    public void catalogRedirectWithoutHidButHaveNid_waitCorrectCatalogRedirect() {
        String searchText = "Утюги Philips";

        reportTestClient.redirect(searchText, "redirect_to_iron_without_hid.json");
        catalogerTestClient.getChildren(54915, "tree_depth_1_irons.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertCatalogRedirectIsCorrect_whenReportRedirectWithoutNidOrHid(searchText, redirect);
    }

    private void assertCatalogRedirectIsCorrect_whenReportRedirectWithoutNidOrHid(
        String searchText, RedirectV2 redirect
    ) {
        assertNotNull(redirect);
        assertTrue(redirect instanceof CatalogRedirectV2);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirect;
        assertEquals(54915, catalogRedirect.getNavigationNodeId().getId());
        assertEquals(90568, catalogRedirect.getCategory().getId());

        assertEquals(redirect.getQueryText(), searchText);
    }

    @Test
    public void catalogRedirectWithoutNidButHaveHid_waitCorrectCatalogRedirect() {
        String searchText = "Утюги Philips";

        reportTestClient.redirect(searchText, "redirect_to_iron_without_nid.json");
        catalogerTestClient.getChildren(54915, "tree_depth_1_irons.xml");
        catalogerTestClient.getPath(90568, "path_hid_90568.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);
        assertCatalogRedirectIsCorrect_whenReportRedirectWithoutNidOrHid(searchText, redirect);
    }

    @Test
    public void catalogRedirectButCatalogerNotFoundNid_waitUnsupportedRedirect() {
        String searchText = "настолка";

        reportTestClient.redirect(searchText, "redirect_nastolki.json");
        catalogerTestClient.getChildren(68325, "tree_depth_1_nastolki_empty.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertEquals(PageType.SEARCH, redirect.getRedirectType());
        assertTrue(redirect instanceof SearchRedirectV2);

        SearchRedirectV2 searchRedirect = (SearchRedirectV2) redirect;
        assertEquals(searchText, searchRedirect.getQueryText());
    }

    @Test
    public void catalogRedirectWithoutText() {
        String searchText = "lego star wars";

        reportTestClient.redirect(searchText, "redirect_without_text.json");
        catalogerTestClient.getChildren(59749, "tree_lego_star_wars.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof CatalogRedirectV2);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirect;

        assertThat(
            catalogRedirect.getCriteria(),
            Matchers.contains(
                cast(CriterionMatcher.textCriterion(null, null, "eJwzilHS4OLLSU3PV")
            ))
        );
    }

    @Test
    public void catalogRedirectWithContentAndWasRedir() {
        String searchText = "lego star wars";

        reportTestClient.redirect(searchText, "redirect_was_redir.json");
        reportTestClient.categorySearch(IntLists.singleton(10470548), x -> x.param("was_redir", "1"),  "redirect_was_redir.json");
        catalogerTestClient.getChildren(59749, "tree_lego_star_wars_was_redir.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), Collections.singleton(RedirectContentType.CATALOG));
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
    }

    @Test
    public void testCatalogRedirectWithoutCategory() {
        String searchText = "Утюги Philips";

        reportTestClient.redirect(searchText, "redirect_without_hid.json");
        catalogerTestClient.getPathNotFound(58898);
        catalogerTestClient.getChildren(58898, "tree_without_hid.xml");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof CatalogRedirectV2);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirect;

        assertNull(catalogRedirect.getCategory());
        assertEquals(58898, catalogRedirect.getNavigationNodeId().getId());
    }

    @Test
    public void sendCvRedirect3WithHids() {
        SearchQuery query = new SearchQuery("Apple", SearchType.TEXT, Collections.emptyMap(), new IntArrayList(Arrays.asList(6427100, 123)), null, null, null);

        FilterParams filterParams = filterParams(getPageTypesForClient(),
            Collections.singleton(RedirectContentType.SEARCH));
        EnrichParams enrichParams = enrichParams(
            ParametersV2.REDIRECT_FIELDS_2.getItems()
                .stream()
                .flatMap(item -> item.getValues().stream())
                .collect(Collectors.toList())
        );

        reportTestClient.redirect3("Apple", "redirect_search_apple.json");

        ReportRedirectV2Result result = getResult(query, filterParams, enrichParams, genericParams);

        SearchRedirectV2 redirect = (SearchRedirectV2)result.getRedirect();

        EnumFilter filter = (EnumFilter)redirect.getFilters().get(1);

        assertEquals("Apple", filter.getValues().get(0).getName());

        assertTrue(filter.getValues().get(0).getChecked());
    }

    @Test
    public void testSkuRedirect() {
        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        context.setClient(MOBILE_BLUE);
        context.setClientVersionInfo(
            new KnownMobileClientVersionInfo(
                Platform.IOS,
                DeviceType.TABLET,
                new SemanticVersion(2, 0, 0)
            )
        );

        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_sku.json");
        reportTestClient.sku("100131946398", "sku_result.json");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SkuRedirectV2);
        assertEquals(PageType.SKU, redirect.getRedirectType());

        SkuRedirectV2 skuRedirect = (SkuRedirectV2) redirect;
        assertEquals("100131946398", skuRedirect.getContent().getSku().getId());
    }

    @Test
    public void testSkuRedirectNotFound() {
        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        context.setClient(MOBILE_BLUE);
        context.setClientVersionInfo(
            new KnownMobileClientVersionInfo(
                Platform.IOS,
                DeviceType.TABLET,
                new SemanticVersion(2, 0, 0)
            )
        );

        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_sku.json");
        reportTestClient.sku("100131946398", "sku_not_found.json");
        reportTestClient.searchV2("apple iphone 8 plus 64gb", "search_sku_unsupported_redirect.json");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SearchRedirectV2);
        assertEquals(PageType.SEARCH, redirect.getRedirectType());

        SearchRedirectV2 searchRedirect = (SearchRedirectV2) redirect;
        ModelV2 model = (ModelV2) searchRedirect.getContent().getItems().get(0);
        assertEquals(1759344591L, model.getId());
    }

    @Test
    public void testCatalogRedirectFilterDescriptionForMobile() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, true);
        context.setClient(MOBILE);

        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_catalog_filter_description.json");
        reportTestClient.categorySearch(IntLists.singleton(91491), "redirect_catalog_filter_description__search.json");

        bukerTestClient.getGurulightFilterDescription(
                Collections.singleton(91491L),
                "redirect_catalog_filter_description__buker_gurulight_filter_description.xml"
        );

        catalogerTestClient.getTree(
                54726,
                1,
                213,
                "redirect_catalog_filter_description__cataloger_tree.xml"
        );

        bukerTestClient.getFilterDescription(
                Arrays.asList(
                        13887626L,
                        15156912L,
                        15164148L,
                        15161366L,
                        7808633L,
                        7013269L,
                        13443198L,
                        4925675L,
                        12616441L,
                        12782797L,
                        12565550L,
                        16230465L
                ),
                "redirect_catalog_filter_description__buker_filter_description.json"
        );

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof CatalogRedirectV2);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirect;

        Map<String, String> expectedDescription = ImmutableMap.<String, String>builder()
                .put("16230465", "Количество основных (тыловых) камер")
                .put("15164148", "Большой объем оперативной памяти")
                .put("7013269", "NFC (Near Field Communication)")
                .put("7808633", "LTE (3GPP Long Term Evolution)")
                .build();

        List<Filter> filters = catalogRedirect.getFilters()
                .stream()
                .filter(f -> expectedDescription.containsKey(f.getId()))
                .collect(Collectors.toList());

        assertEquals(filters.size(), expectedDescription.size());
        for (Filter filter: filters) {
            assertEquals(expectedDescription.get(filter.getId()), filter.getDescription());
        }

    }

    @Test
    public void testCatalogRedirectFilterDescriptionForExternal() {
        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_catalog_filter_description.json");
        reportTestClient.categorySearch(IntLists.singleton(91491), "redirect_catalog_filter_description__search.json");

        catalogerTestClient.getTree(
                54726,
                1,
                213,
                "redirect_catalog_filter_description__cataloger_tree.xml"
        );

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof CatalogRedirectV2);
        assertEquals(PageType.CATALOG, redirect.getRedirectType());

        CatalogRedirectV2 catalogRedirect = (CatalogRedirectV2) redirect;
        List<Filter> filters = catalogRedirect.getFilters();
        List<Filter> filterWithEmptyDescription = filters.stream()
                .filter(f -> null == f.getDescription())
                .collect(Collectors.toList());
        assertEquals(filters.size(), filterWithEmptyDescription.size());

    }

    @Test
    public void testSearchRedirectFilterDescriptionForMobile() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, true);
        context.setClient(MOBILE);

        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_search_filter_description.json");

        bukerTestClient.getGurulightFilterDescription(
                Collections.singleton(91491L),
                "redirect_catalog_filter_description__buker_gurulight_filter_description.xml"
        );

        bukerTestClient.getFilterDescription(
                Arrays.asList(
                        13887626L,
                        15156912L,
                        15164148L,
                        15161366L,
                        7808633L,
                        7013269L,
                        13443198L,
                        4925675L,
                        12616441L,
                        12782797L,
                        12565550L,
                        16230465L
                ),
                "redirect_catalog_filter_description__buker_filter_description.json"
        );

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SearchRedirectV2);
        assertEquals(PageType.SEARCH, redirect.getRedirectType());

        SearchRedirectV2 searchRedirect = (SearchRedirectV2) redirect;
        Map<String, String> expectedDescription = ImmutableMap.<String, String>builder()
                .put("16230465", "Количество основных (тыловых) камер")
                .put("15164148", "Большой объем оперативной памяти")
                .put("7013269", "NFC (Near Field Communication)")
                .put("7808633", "LTE (3GPP Long Term Evolution)")
                .build();

        List<Filter> filters = searchRedirect.getFilters()
                .stream()
                .filter(f -> expectedDescription.containsKey(f.getId()))
                .collect(Collectors.toList());

        assertEquals(filters.size(), expectedDescription.size());
        for (Filter filter: filters) {
            assertEquals(expectedDescription.get(filter.getId()), filter.getDescription());
        }

    }

    @Test
    public void testSearchRedirectFilterDescriptionForExternal() {
        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_search_filter_description.json");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(getPageTypesForClient(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SearchRedirectV2);
        assertEquals(PageType.SEARCH, redirect.getRedirectType());

        SearchRedirectV2 searchRedirect = (SearchRedirectV2) redirect;

        List<Filter> filters = searchRedirect.getFilters();
        List<Filter> filterWithEmptyDescription = filters.stream()
                .filter(f -> null == f.getDescription())
                .collect(Collectors.toList());
        assertEquals(filters.size(), filterWithEmptyDescription.size());

    }

    @Test
    public void testUnsupportedRedirectFilterDescriptionForMobile() {
        mockClientHelper.is(ClientHelper.Type.WHITE_APP_NEW, true);
        context.setClient(MOBILE);

        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_catalog_filter_description.json");
        reportTestClient.searchV2(searchText, "redirect_catalog_filter_description__search.json");

        bukerTestClient.getGurulightFilterDescription(
                Collections.singleton(91491L),
                "redirect_catalog_filter_description__buker_gurulight_filter_description.xml"
        );

        bukerTestClient.getFilterDescription(
                Arrays.asList(
                        13887626L,
                        15156912L,
                        15164148L,
                        15161366L,
                        7808633L,
                        7013269L,
                        13443198L,
                        4925675L,
                        12616441L,
                        12782797L,
                        12565550L,
                        16230465L
                ),
                "redirect_catalog_filter_description__buker_filter_description.json"
        );

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Collections.emptyList(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SearchRedirectV2);
        assertEquals(PageType.SEARCH, redirect.getRedirectType());

        SearchRedirectV2 catalogRedirect = (SearchRedirectV2) redirect;

        Map<String, String> expectedDescription = ImmutableMap.<String, String>builder()
                .put("16230465", "Количество основных (тыловых) камер")
                .put("15164148", "Большой объем оперативной памяти")
                .put("7013269", "NFC (Near Field Communication)")
                .put("7808633", "LTE (3GPP Long Term Evolution)")
                .build();

        List<Filter> filters = catalogRedirect.getFilters()
                .stream()
                .filter(f -> expectedDescription.containsKey(f.getId()))
                .collect(Collectors.toList());

        assertEquals(filters.size(), expectedDescription.size());
        for (Filter filter: filters) {
            assertEquals(expectedDescription.get(filter.getId()), filter.getDescription());
        }

    }

    @Test
    public void testUnsupportedRedirectFilterDescriptionForExternal() {
        String searchText = "apple iphone 8 plus 64gb";

        reportTestClient.redirect(searchText, "redirect_catalog_filter_description.json");
        reportTestClient.searchV2(searchText, "redirect_catalog_filter_description__search.json");

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams(Collections.emptyList(), getContentTypesForClient());
        EnrichParams enrichParams = enrichParams(Collections.singletonList(FilterField.DESCRIPTION));

        RedirectV2 redirect = getRedirect(userParams, filterParams, enrichParams, genericParams);

        assertNotNull(redirect);
        assertTrue(redirect instanceof SearchRedirectV2);
        assertEquals(PageType.SEARCH, redirect.getRedirectType());

        SearchRedirectV2 catalogRedirect = (SearchRedirectV2) redirect;
        List<Filter> filters = catalogRedirect.getFilters();
        List<Filter> filterWithEmptyDescription = filters.stream()
                .filter(f -> null == f.getDescription())
                .collect(Collectors.toList());
        assertEquals(filters.size(), filterWithEmptyDescription.size());

    }

    private RedirectV2 getRedirect(SearchQuery userParams,
                                   FilterParams filterParams,
                                   EnrichParams enrichParams,
                                   GenericParams genericParams) {
        return getResult(userParams, filterParams, enrichParams, genericParams)
            .getRedirect();
    }

    private ReportRedirectV2Result getResult(SearchQuery userParams,
                                             FilterParams filterParams,
                                             EnrichParams enrichParams,
                                             GenericParams genericParams) {
        return redirectController.redirect(userParams, filterParams, null, enrichParams, genericParams).waitResult();
    }

    private SearchQuery userParams(String text, SearchType type) {
        return new SearchQuery(text, type, null);
    }

    private SearchQuery userParams(String text) {
        return new SearchQuery(text, SearchType.TEXT, null);
    }

    private FilterParams filterParams(Collection<PageType> pageTypes) {
        return filterParams(pageTypes, Collections.emptyList());
    }

    private FilterParams filterParams(Collection<PageType> pageTypes, Collection<? extends ContentType> contents) {
        FilterParams filterParams = new FilterParams();
        filterParams.setRedirectTypes(pageTypes);
        filterParams.setContents(contents);
        return filterParams;
    }

    private EnrichParams enrichParams(Collection<Field> fields) {
        EnrichParams enrichParams = new EnrichParams();
        enrichParams.setFields(fields);
        enrichParams.setUserAgent("inttest");
        enrichParams.setPageInfo(PageInfo.DEFAULT);
        enrichParams.setSort(UniversalModelSort.POPULARITY_SORT);
        return enrichParams;
    }

    private EnrichParams enrichParams() {
        return enrichParams(Collections.emptyList());
    }

    private Client.Type getType() {
        return Functionals.getOrDefault(
            ContextHolder.get(),
            Context::getClient,
            Client::getType,
            Client.Type.EXTERNAL
        );
    }

    private Collection<? extends ContentType> getContentTypesForClient() {
        return AVAILABLE_CONTENT_TYPES.get(getType());
    }

    private Collection<PageType> getPageTypesForClient() {
        return AVAILABLE_PAGE_TYPE.get(getType());
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
