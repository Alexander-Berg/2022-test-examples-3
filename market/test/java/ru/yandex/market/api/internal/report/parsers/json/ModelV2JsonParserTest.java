package ru.yandex.market.api.internal.report.parsers.json;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.currency.Currency;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.Offer;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.Image;
import ru.yandex.market.api.domain.v2.ModelPriceV2;
import ru.yandex.market.api.domain.v2.ModelV2;
import ru.yandex.market.api.domain.v2.SpecificationGroup;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.guru.ModelType;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.matchers.ModelSkuStatsMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.common.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.matchers.ModelMatcher.model;
import static ru.yandex.market.api.matchers.ModelMatcher.offer;
import static ru.yandex.market.api.matchers.ModelMatcher.skuStats;
import static ru.yandex.market.api.matchers.OfferMatcher.offerId;
import static ru.yandex.market.api.matchers.OfferMatcher.wareMd5;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
@WithMocks
public class ModelV2JsonParserTest extends BaseTest {
    private static final String RETURNED_URL = "https://market-click2.yandex.ru/redir/C-RM8";
    @Mock
    private CurrencyService currencyService;

    private FilterFactory filterFactory;

    @Mock
    private ClientHelper clientHelper;

    @Mock
    private GeoRegionService geoRegionService;

    @Mock
    private BlueMobileApplicationRule blueMobileApplicationRule;

    private MockClientHelper mockClientHelper;

    @Before
    public void setUp() {
        filterFactory = new FilterFactory();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void shouldConvertPrices() {
        ContextHolder.get().setCurrency(Currency.RUR);
        ContextHolder.get().setAlternateCurrency(Currency.BYR);

        ModelV2 model = parse(EnumSet.allOf(ModelInfoField.class), "report-model.json");
        verify(currencyService, times(1)).doPriceConversions(eq(model.getPrice()), any(), any());
    }

    @Test
    public void shouldParseModel() {
        ModelV2 model = parse(EnumSet.allOf(ModelInfoField.class), "report-model.json");

        assertNotNull(model);
        assertEquals(6229012, model.getId());
        assertEquals("Samsung Wave 525 GT-S5250", model.getName());
        assertEquals(ModelType.MODEL, model.getType());
        assertEquals(null, model.getModificationCount());

        assertEquals("Model's description", model.getDescription());
        assertEquals("Model's full description", model.getFullDescription());
        assertEquals("смартфон", model.getKind());

        assertTrue(model.getIsNew());

        assertEquals(91491, model.getCategoryId());
        assertEquals(null, model.getCategory());
        assertEquals(13546772L, (long) model.getParentId());
        assertEquals("Full warning", model.getWarning());
        assertEquals("age", model.getWarnings().get(0).getCode());

        assertEquals(new ModelPriceV2("4490", "4490", "4490", null, null), model.getPrice());

        assertEquals(153061, model.getVendorId());

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", model.getPhoto().getUrl());

        assertEquals(RETURNED_URL, model.getUrl());
    }

    @Test
    public void shouldParseVisualModel() {
        ModelV2 model = parse(EnumSet.allOf(ModelInfoField.class), "visual-model.json");

        assertNotNull(model);
        assertEquals(1375220266, model.getId());
        assertEquals("Платье Wisell", model.getName());
        assertEquals(ModelType.CLUSTER, model.getType());
        assertEquals(null, model.getModificationCount());

        assertEquals(7811901, model.getCategoryId());
        assertEquals(null, model.getCategory());
        assertEquals(null, model.getParentId());
        assertEquals(null, model.getWarning());
        assertEquals(null, model.getWarnings());

        assertEquals(new ModelPriceV2("1407005", "898591", "1283701", "30", "1272704"), model.getPrice());

        assertEquals(8333752, model.getVendorId());

        assertEquals("http://7.cs-ellpic01gt.yandex.ru/market_tEBYGIyM95RE76wZRUT1Zg_1x1.jpg", model.getPhoto().getUrl());

        assertEquals(4, model.getPhotos().size());

        assertEquals("http://7.cs-ellpic01gt.yandex.ru/market_tEBYGIyM95RE76wZRUT1Zg_1x1.jpg", model.getPhotos().get(0).getUrl());
        assertEquals("http://2.cs-ellpic01gt.yandex.ru/market_0A3u-ZvrxxBrE4sQ8VbRHA_1x1.jpg", model.getPhotos().get(1).getUrl());
        assertEquals("http://7.cs-ellpic01gt.yandex.ru/market_qesiVFc-fVnkTGMxCnjEjQ_1x1.jpg", model.getPhotos().get(2).getUrl());
        assertEquals("http://2.cs-ellpic01gt.yandex.ru/market__4M0LJmUCyoJRDGApUFNjQ_1x1.jpg", model.getPhotos().get(3).getUrl());
    }

    /**
     * Тестирование того что в случае если у модели есть несколько изображений массив photos не содержит изображение
     * из photo
     */
    @Test
    public void testPhotosOfParsedModel() {
        ModelV2 model = parse(EnumSet.allOf(ModelInfoField.class), "report-model.json");

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", model.getPhoto().getUrl());

        List<? extends Image> photos = model.getPhotos();
        assertEquals(4, photos.size());

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", photos.get(0).getUrl());
        assertEquals("http://mdata.yandex.net/i?path=b0728164109_img_id1560594168971184691.jpg", photos.get(1).getUrl());
        assertEquals("http://mdata.yandex.net/i?path=b0728164210_img_id3432400108193051634.jpg", photos.get(2).getUrl());
        assertEquals("http://mdata.yandex.net/i?path=b0728164304_img_id7450042585455629481.jpg", photos.get(3).getUrl());
    }

    @Test
    public void testNotEmptyPhotosIfModelHasOnlyPicture() {
        ModelV2 model = parse(EnumSet.allOf(ModelInfoField.class), "model-with-one-pic.json");

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", model.getPhoto().getUrl());

        List<? extends Image> photos = model.getPhotos();
        assertEquals(1, photos.size());

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", photos.get(0).getUrl());
    }

    @Test
    public void testParseModelWithOnlyPhotoField() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.PHOTO), "report-model.json");

        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", model.getPhoto().getUrl());
        assertNull(model.getPhotos());
    }

    @Test
    public void testParseModelSpecification() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.SPECIFICATION), "report-model.json");

        List<SpecificationGroup> groups = model.getSpecificationGroups();
        assertNotNull(groups);
        assertEquals(1, groups.size());
        assertEquals("Общие характеристики", groups.get(0).getName());
        assertEquals(12, groups.get(0).getFeatures().size());
    }

    @Test
    public void shouldNotOverflowOnBigOfferCount() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.OFFERS), "model-with-big-offer-count.json");
        assertEquals(47244640286L, model.getOfferCount().longValue());
    }

    @Test
    public void blueOfferCount() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.OFFERS), "model-with-big-offer-count.json", true);
        assertEquals(12468L, model.getOfferCount().longValue());
    }

    @Test
    public void shouldParseDefaultOffer() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.DEFAULT_OFFER), "model-with-default-offer.json");
        assertThat(model,
            model(
                offer(
                    cast(
                        offerId(
                            wareMd5("Sku4Price55-iLVm1Goleg")
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldParseSkuStats() {
        ModelV2 model = parse(Collections.emptyList(), "model-with-sku-stats.json");
        assertThat(
            model,
            model(
                skuStats(
                    ModelSkuStatsMatcher.beforeFilters(1),
                    ModelSkuStatsMatcher.afterFilters(1)
                )
            )
        );
    }

    @Test
    public void shouldParseReasonsToBuy() {
        ModelV2 model = parse(Collections.singleton(ModelInfoField.REASONS_TO_BUY), "model-with-reasons-to-buy.json");
        String reasonsToBuy = model.getReasonsToBuy().toString();
        assertEquals("[{\"value\":5413.220703,\"type\":\"statFactor\",\"id\":\"bought_n_times\"},{\"value\":0.84375,\"type\":\"consumerFactor\",\"id\":\"customers_choice\"}]",
            reasonsToBuy);
    }

    @Test
    public void shouldNotParseTraceFactorsByDefault() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, false);
        mockClientHelper.is(ClientHelper.Type.SOVETNIK_FOR_SITE, false);

        ModelV2 model = parse(Collections.emptyList(), "model-with-trace.json");
        assertNull(model.getTrace());
    }

    @Test
    public void shouldParseTraceFactorsForSovetnik() {
        mockClientHelper.is(ClientHelper.Type.SOVETNIK, true);

        ModelV2 model = parse(Collections.emptyList(), "model-with-trace.json");
        assertNotNull(model.getTrace());
    }

    private ModelV2 parse(Collection<? extends Field> fields, String filename) {
        return parse(fields, filename, false);
    }

    private ModelV2 parse(Collection<? extends Field> fields, String filename, boolean isBlue) {
        ReportRequestContext context = new ReportRequestContext();
        context.setFields(fields);

        OfferV2JsonParser offerParser = mock(OfferV2JsonParser.class);
        when(offerParser.getParsed()).thenReturn(null);

        MarketUrls marketUrls = mock(MarketUrls.class);
        when(marketUrls.getClickUrl("/redir/C-RM8")).thenReturn(RETURNED_URL);

        UrlParamsFactoryImpl urlParamsFactoryImpl = mock(UrlParamsFactoryImpl.class);

        ReportParserFactory factory = new ReportParserFactory(
            currencyService,
            null,
            geoRegionService,
            null,
            filterFactory,
            marketUrls,
                urlParamsFactoryImpl,
            clientHelper,
            null,
                blueMobileApplicationRule
        );


        Parser<ModelV2> parser = new ModelV2JsonParser(
                factory,
                context,
                currencyService,
                filterFactory,
                marketUrls,
                false,
                isBlue,
                isBlue,
                clientHelper
        );
        return parser.parse(ResourceHelpers.getResource(filename));
    }

    private Matcher<Offer> cast(Matcher<? extends Offer> matcher) {
        return (Matcher<Offer>) matcher;
    }
}
