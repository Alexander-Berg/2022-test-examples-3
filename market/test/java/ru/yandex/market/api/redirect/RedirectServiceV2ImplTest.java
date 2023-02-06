package ru.yandex.market.api.redirect;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Predicate;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.category.FilterService;
import ru.yandex.market.api.common.MarketType;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.PagedResultWithOptions;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PageType;
import ru.yandex.market.api.domain.v2.redirect.ModelRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.RedirectV2;
import ru.yandex.market.api.domain.v2.redirect.ReportRedirectType;
import ru.yandex.market.api.domain.v2.redirect.SearchRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.content.UnsupportedRedirect;
import ru.yandex.market.api.domain.v2.redirect.parameters.EnrichParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.FilterParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.SearchQuery;
import ru.yandex.market.api.domain.v2.redirect.parameters.UrlUserParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.UserParams;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.ClckHttpClient;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.report.ReportClient;
import ru.yandex.market.api.internal.report.ReportRedirectInfo;
import ru.yandex.market.api.internal.report.ReportRedirectInfoBuilder;
import ru.yandex.market.api.internal.report.ReportSort;
import ru.yandex.market.api.internal.report.data.InternalModelOffersResult;
import ru.yandex.market.api.matchers.GetOffersByModelRequestMatcher;
import ru.yandex.market.api.model.ModelService;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.offer.GetOffersByModelRequest;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.search.SearchType;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.ModelVersion;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.service.RedirectServiceV2Impl;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.concurrent.Pipelines;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

/**
 * @author dimkarp93
 */
@WithContext
@WithMocks
public class RedirectServiceV2ImplTest extends BaseTest {
    private RedirectServiceV2Impl redirectService;

    @Mock
    private ReportClient reportClient;

    @Mock
    private ClckHttpClient clckClient;

    @Mock
    private BlueRule blueRule;

    @Mock
    private ModelService modelService;

    @Mock
    private OfferService offerService;

    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private GenericParams genericParams;

    @Mock
    private FilterService filterService;

    @Inject
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Inject
    private MarketUrls marketUrls;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        redirectService = new RedirectServiceV2Impl(
            reportClient,
            null,
            modelService,
            null,
            null,
            null,
            offerService,
            null,
            null,
            null,
            null,
            null,
            marketUrls,
            clckClient,
            filterService,
                urlParamsFactoryImpl
        );

        Mockito.when(geoRegionService.getRegion(Mockito.anyInt(), Mockito.anyCollection(), Mockito.any(RegionVersion.class)))
            .thenReturn(new GeoRegion(1, null, null, null, null));
    }

    /**
     * Проверяем, что не зацикливаемся при обработке редиректа,
     * если редирект на редирект не отличается от исходного.
     * <p>
     * В этом случае отдаем UNSUPPORTED + дополнительно проверяем,
     * что правильно отдаем queryText
     */
    @Test
    public void testNonCyclicWhenRedirectInfoNotChange() {
        String searchText = "test";

        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.REPORT_REDIRECT)
            .setText(searchText);

        SearchQuery userParams = userParams(searchText);
        FilterParams filterParams = filterParams();
        EnrichParams enrichParams = enrichParams();

        when(
            reportClient.getRedirectV2(
                any(SearchQuery.class),
                any(Map.class),
                any(IntList.class),
                anyBoolean(),
                any(PageInfo.class),
                any(ReportSort.class),
                any(Collection.class),
                any(GenericParams.class)
            )
        )
            .thenReturn(Futures.newSucceededFuture(info));

        RedirectV2 redirectV2 = Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams,
                filterParams,
                enrichParams,
                GenericParams.DEFAULT)
        ).getRedirect();

        Assert.assertEquals(PageType.SEARCH, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof SearchRedirectV2);

        Assert.assertEquals(searchText, redirectV2.getQueryText());
    }

    @Test
    public void testRedirectUnsupportedByUrlUserParamsClickSuccess() {
        String shortLink = "https://ya.cc/11111";
        String link = "https://market.yandex.ru/product/123?pp=12&clid=34&vid=56&mclid=78&distr_type=3";

        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.UNSUPPORTED);

        ContextHolder.update(
            ctx -> {
                ctx.setPpList(IntLists.singleton(12));
                ctx.setPartnerInfo(PartnerInfo.create("34", "56", 78L, 3L, null));
            }
        );


        Mockito
            .when(clckClient.clckSimple(Mockito.eq(link)))
            .thenReturn(Futures.newSucceededFuture(shortLink));

        UserParams userParams = new UrlUserParams("https://market.yandex.ru/product/123");
        FilterParams filterParams = filterParams();
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirectV2 = Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams,
                filterParams,
                enrichParams,
                GenericParams.DEFAULT
            )
        ).getRedirect();

        Assert.assertEquals(PageType.UNSUPPORTED, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof UnsupportedRedirect);

        Assert.assertEquals(
            "https://market.yandex.ru/product/123?pp=12&clid=34&vid=56&mclid=78&distr_type=3",
            redirectV2.getLink()
        );

        Assert.assertEquals(
            shortLink,
            redirectV2.getShortLink()
        );
    }

    @Test
    public void testRedirectUnsupportedByUrlUserClckNull() {
        String link = "https://market.yandex.ru/product/123?pp=12&clid=34&vid=56&mclid=78&distr_type=3";

        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.UNSUPPORTED);

        ContextHolder.update(
            ctx -> {
                ctx.setPpList(IntLists.singleton(12));
                ctx.setPartnerInfo(PartnerInfo.create("34", "56", 78L, 3L, null));
            }
        );

        Mockito
            .when(clckClient.clckSimple(Mockito.eq(link)))
            .thenReturn(Futures.newSucceededFuture(null));

        UserParams userParams = new UrlUserParams("https://market.yandex.ru/product/123");
        FilterParams filterParams = filterParams();
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirectV2 = Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams,
                filterParams,
                enrichParams,
                GenericParams.DEFAULT
            )
        ).getRedirect();

        Assert.assertEquals(PageType.UNSUPPORTED, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof UnsupportedRedirect);

        Assert.assertEquals(
            "https://market.yandex.ru/product/123?pp=12&clid=34&vid=56&mclid=78&distr_type=3",
            redirectV2.getLink()
        );

        Assert.assertNull(
            null,
            redirectV2.getShortLink()
        );
    }

    @Test
    public void testRedirectUnsupportedByUrlUserParamsClckFail() {
        String link = "https://market.yandex.ru/product/123?pp=12&clid=34&vid=56&mclid=78&distr_type=3";

        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.UNSUPPORTED);

        ContextHolder.update(
            ctx -> {
                ctx.setPpList(IntLists.singleton(12));
                ctx.setPartnerInfo(PartnerInfo.create("34", "56", 78L, 3L, null));
            }
        );

        Mockito
            .when(clckClient.clckSimple(Mockito.eq(link)))
            .thenReturn(Futures.newFailedFuture(new RuntimeException("request has failed")));

        UserParams userParams = new UrlUserParams("https://market.yandex.ru/product/123");
        FilterParams filterParams = filterParams();
        EnrichParams enrichParams = enrichParams();

        RedirectV2 redirectV2 = Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams,
                filterParams,
                enrichParams,
                GenericParams.DEFAULT
            )
        ).getRedirect();

        Assert.assertEquals(PageType.UNSUPPORTED, redirectV2.getRedirectType());
        Assert.assertTrue(redirectV2 instanceof UnsupportedRedirect);

        Assert.assertEquals(
            link,
            redirectV2.getLink()
        );

        Assert.assertEquals(
            link,
            redirectV2.getShortLink()
        );
    }

    @Test
    public void testBeruModelWithEnrichOfferLinks() {
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));
        //TODO Проще!!!!
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);
        Mockito.when(blueRule.test()).thenReturn(false);
        Mockito.when(blueRule.or(Mockito.any(Predicate.class)))
               .then(x -> x.getArgumentAt(0, Predicate.class));

        long modelId = 123L;
        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.MODEL_REDIRECT)
            .setModelId(modelId);


        Matcher<GetOffersByModelRequest> matcher = GetOffersByModelRequestMatcher.offerByModelRequest(
            GetOffersByModelRequestMatcher.modelId(modelId),
            GetOffersByModelRequestMatcher.marketType(MarketType.BLUE),
            GetOffersByModelRequestMatcher.withModel(false)
        );

        OfferV2 offer1 = new OfferV2();
        offer1.setWareMd5("abc");
        offer1.setCategoryId(45);
        offer1.setModel(new ModelV2.ModelId(123L));
        offer1.setSku("6");
        offer1.setOwnMarketPlace(true);
        OfferV2 offer2 = new OfferV2();
        offer2.setWareMd5("xyz");
        offer2.setCategoryId(45);
        offer2.setModel(new ModelV2.ModelId(123L));
        offer2.setSku("7");
        offer2.setOwnMarketPlace(true);


        InternalModelOffersResult result = new InternalModelOffersResult(
            new PagedResultWithOptions<>(Arrays.asList(offer1, offer2), PageInfo.ALL_ITEMS),
            null,
            null
        );

        Mockito
            .when(
                modelService.getModel(
                    Mockito.eq(modelId),
                    Mockito.anyMapOf(String.class, String.class),
                    Mockito.anyCollectionOf(Field.class),
                    Mockito.any(GenericParams.class),
                    Mockito.any(ModelVersion.class)
                )
            )
            .thenReturn(Pipelines.startWithValue(new ModelV2()));


        Mockito.when(offerService.getModelOffersV2(Mockito.argThat(matcher)))
               .thenReturn(Futures.newSucceededFuture(result));

        EnrichParams enrichParams = enrichParams();
        enrichParams.setEnrichOfferLinks(true);

        ModelRedirectV2 redirect = (ModelRedirectV2) Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams("text"),
                filterParams(),
                enrichParams,
                genericParams
            )
        ).getRedirect();

        Assert.assertEquals(PageType.MODEL, redirect.getRedirectType());
        Assert.assertThat(
            redirect.getOfferLinks(),
            Matchers.containsInAnyOrder(
                    Matchers.is("https://pokupki.market.yandex.ru/product/6?pp=37&hid=45&offerid=abc&lr=213"),
                    Matchers.is("https://pokupki.market.yandex.ru/product/7?pp=37&hid=45&offerid=xyz&lr=213")
            )
        );

    }

    @Test
    public void testNotBeruModelWithEnrichOfferLinks() {
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));
        //TODO Проще!!!!
        Mockito.when(blueRule.test(Mockito.any(Context.class))).thenReturn(false);
        Mockito.when(blueRule.test()).thenReturn(false);
        Mockito.when(blueRule.or(Mockito.any(Predicate.class)))
            .then(x -> x.getArgumentAt(0, Predicate.class));

        long modelId = 123L;
        ReportRedirectInfoBuilder info = ReportRedirectInfo.builder()
            .setType(ReportRedirectType.MODEL_REDIRECT)
            .setModelId(modelId);


        Matcher<GetOffersByModelRequest> matcher = GetOffersByModelRequestMatcher.offerByModelRequest(
            GetOffersByModelRequestMatcher.modelId(modelId),
            GetOffersByModelRequestMatcher.marketType(MarketType.BLUE),
            GetOffersByModelRequestMatcher.withModel(false)
        );

        Mockito
            .when(
                modelService.getModel(
                    Mockito.eq(modelId),
                    Mockito.anyMapOf(String.class, String.class),
                    Mockito.anyCollectionOf(Field.class),
                    Mockito.any(GenericParams.class),
                    Mockito.any(ModelVersion.class)
                )
            )
            .thenReturn(Pipelines.startWithValue(new ModelV2()));


        Mockito.when(offerService.getModelOffersV2(Mockito.argThat(matcher)))
            .thenReturn(Futures.newSucceededFuture(null));

        EnrichParams enrichParams = enrichParams();
        enrichParams.setEnrichOfferLinks(true);

        ModelRedirectV2 redirect = (ModelRedirectV2) Futures.waitAndGet(
            redirectService.processRedirect(
                Futures.newPromise(),
                info.build(),
                userParams("text"),
                filterParams(),
                enrichParams,
                genericParams
            )
        ).getRedirect();

        Assert.assertEquals(PageType.MODEL, redirect.getRedirectType());
        Assert.assertThat(
            redirect.getOfferLinks(),
            Matchers.nullValue()
        );
    }

    private SearchQuery userParams(String text) {
        return new SearchQuery(text, SearchType.TEXT, null);
    }

    private FilterParams filterParams() {
        FilterParams filterParams = new FilterParams();
        filterParams.setRedirectTypes(EnumSet.allOf(PageType.class));
        filterParams.setContents(Collections.emptyList());
        return filterParams;
    }

    private EnrichParams enrichParams() {
        EnrichParams enrichParams = new EnrichParams();
        enrichParams.setSort(UniversalModelSort.POPULARITY_SORT);
        enrichParams.setPageInfo(PageInfo.DEFAULT);
        enrichParams.setFields(Collections.emptyList());
        enrichParams.setUserAgent("test");
        return enrichParams;
    }
}
