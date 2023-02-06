package ru.yandex.market.api.internal.report.parsers.json;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.CommonMarketUrls;
import ru.yandex.market.api.controller.Parameters;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.guru.ModelType;
import ru.yandex.market.api.internal.report.parsers.json.filters.v1.FilterV1Factory;
import ru.yandex.market.api.model.AbstractModelV1;
import ru.yandex.market.api.model.ClusterModelV1;
import ru.yandex.market.api.model.DefaultModelV1;
import ru.yandex.market.api.model.Photo;
import ru.yandex.market.api.model.Prices;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;
import ru.yandex.market.common.Parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by apershukov on 09.02.17.
 */
@WithContext
public class ModelV1JsonParserTest extends BaseTest {

    private CurrencyService currencyService;
    @Inject
    private CommonMarketUrls commonMarketUrls;

    @Before
    public void setUp() {
        currencyService = mock(CurrencyService.class); }

    @Test
    public void testParseGuruModel() {
        DefaultModelV1 model = (DefaultModelV1) parse("report-model.json");

        assertEquals(6229012L, model.getId());
        assertEquals("Samsung Wave 525 GT-S5250", model.getName());
        assertEquals("Model's description", model.getDescription());
        assertEquals(91491, model.getCategoryId());
        assertEquals("смартфон", model.getKind());

        Prices prices = model.getPrices();
        assertEquals("4490", prices.getMax());
        assertEquals("4490", prices.getMin());
        assertEquals("4490", prices.getAvg());
        assertEquals("RUR", prices.getCurCode());

        Photo mainPhoto = model.getMainPhoto();
        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg", mainPhoto.getUrl());
        assertEquals(0, mainPhoto.getWidth());
        assertEquals(0, mainPhoto.getHeight());

        Photo previewPhoto = model.getPreviewPhoto();
        assertEquals("http://mdata.yandex.net/i?path=b0728164104_img_id8970756987987531505.jpg&size=4", previewPhoto.getUrl());
        assertEquals(150, previewPhoto.getWidth());
        assertEquals(150, previewPhoto.getHeight());

        assertEquals(4, model.getPhotos().getPhoto().size());
        assertEquals(4, model.getPreviewPhotos().size());

        assertEquals(153061L, model.getVendorId());
        assertEquals("Samsung", model.getVendor());

        assertFalse(model.getIsGroup());

        assertEquals(123, model.getOffersCount());
        assertEquals("Full warning", model.getWarning());
        assertEquals(1, model.getIsNew());
        assertEquals(13546772, model.getParentModel().getId());
    }

    @Test
    public void shouldParseVisualModel() throws Exception {
        ClusterModelV1 model = (ClusterModelV1) parse("visual-model.json");

        assertEquals(1375220266, model.getId());
        assertEquals("Платье Wisell", model.getName());
        assertEquals("", model.getDescription());
        assertEquals(7811901, model.getCategoryId());

        Prices prices = model.getPrices();
        assertEquals("1407005", prices.getMax());
        assertEquals("898591", prices.getMin());
        assertEquals("1283701", prices.getAvg());
        assertEquals("BYR", prices.getCurCode());

        Photo mainPhoto = model.getMainPhoto();
        assertEquals("http://7.cs-ellpic01gt.yandex.ru/market_tEBYGIyM95RE76wZRUT1Zg_1x1.jpg", mainPhoto.getUrl());
        assertEquals(665, mainPhoto.getWidth());
        assertEquals(998, mainPhoto.getHeight());

        Photo previewPhoto = model.getPreviewPhoto();
        assertEquals("http://2.cs-ellpic01gt.yandex.ru/market_tEBYGIyM95RE76wZRUT1Zg_190x250.jpg", previewPhoto.getUrl());
        assertEquals(166, previewPhoto.getWidth());
        assertEquals(250, previewPhoto.getHeight());

        assertEquals(4, model.getPhotos().size());
        assertEquals(4, model.getPreviewPhotos().size());

        assertEquals(8333752, model.getVendorId());
        assertEquals("Wisell", model.getVendor());

        assertFalse(model.getIsGroup());

        assertEquals(31, model.getOffersCount());
        assertNull(model.getWarning());
        assertEquals(0, model.getIsNew());
        assertNull(model.getParentModel());
    }

    @Test
    public void testConvertCurrency() {
        AbstractModelV1 market = parse("report-model.json");
        assertNotNull(market);

        verify(currencyService, times(1)).doPriceConversions(eq(market.getPrices()), any(), any());
    }

    @Test
    public void testParseGroupModel() {
        AbstractModelV1 model = parse("report-group-model.json");
        assertTrue(model.getIsGroup());
        assertEquals(9, (int) model.getModificationsCount());
        assertEquals(0, model.getIsNew());
        assertEquals(ModelType.GROUP, model.getType());
    }

    @Test
    public void shouldNotOverflowOnBigOfferCount() {
        AbstractModelV1 modelV1 = parse("model-with-big-offer-count.json");
        assertEquals(47244640286L, modelV1.getOffersCount());
    }

    @Test
    public void shouldParseEmptyThumbnails() {
        parse("report-model-with-empty-thumbnails.json");
    }

    @Test
    public void shouldParseEmptyFullWarning() {
        parse("report-model-empty-full-warning.json");
    }

    private AbstractModelV1 parse(String file) {
        Parser<AbstractModelV1> parser = new ModelV1JsonParser(Parameters.MODEL_INFO_COMMON_FIELDS,
            currencyService, new FilterV1Factory(), commonMarketUrls);
        return parser.parse(ResourceHelpers.getResource(file));
    }

}
