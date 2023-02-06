package ru.yandex.market.api.internal.report.parsers.json.sku;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mock;
import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.client.rules.BlueMobileApplicationRule;
import ru.yandex.market.api.common.client.rules.BlueRule;
import ru.yandex.market.api.common.currency.CurrencyService;
import ru.yandex.market.api.common.url.params.UrlParamsFactoryImpl;
import ru.yandex.market.api.domain.Field;
import ru.yandex.market.api.domain.v1.ModelInfoField;
import ru.yandex.market.api.domain.v2.ImageWithThumbnails;
import ru.yandex.market.api.domain.v2.Sku;
import ru.yandex.market.api.domain.v2.SkuField;
import ru.yandex.market.api.domain.v2.filters.Filter;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.SkuParser;
import ru.yandex.market.api.internal.report.parsers.json.filters.FilterFactory;
import ru.yandex.market.api.matchers.FilterValueMatcher;
import ru.yandex.market.api.matchers.FiltersMatcher;
import ru.yandex.market.api.matchers.FormattedDescriptionMatcher;
import ru.yandex.market.api.matchers.ModelMatcher;
import ru.yandex.market.api.matchers.SkuMatcher;
import ru.yandex.market.api.matchers.WarningMatcher;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.common.Parser;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.ImageMatcher.image;
import static ru.yandex.market.api.matchers.ModelMatcher.id;
import static ru.yandex.market.api.matchers.OfferMatcher.offer;
import static ru.yandex.market.api.matchers.OfferMatcher.offerId;
import static ru.yandex.market.api.matchers.OfferMatcher.wareMd5;
import static ru.yandex.market.api.matchers.SkuMatcher.description;
import static ru.yandex.market.api.matchers.SkuMatcher.filters;
import static ru.yandex.market.api.matchers.SkuMatcher.formattedDescription;
import static ru.yandex.market.api.matchers.SkuMatcher.model;
import static ru.yandex.market.api.matchers.SkuMatcher.name;
import static ru.yandex.market.api.matchers.SkuMatcher.offers;
import static ru.yandex.market.api.matchers.SkuMatcher.photos;
import static ru.yandex.market.api.matchers.SkuMatcher.sku;
import static ru.yandex.market.api.matchers.SkuMatcher.warnings;

@WithContext
@WithMocks
public class SkuSingleParserTest extends BaseTest {
    @Mock
    private CurrencyService currencyService;
    @Mock
    private MarketUrls marketUrls;

    @Mock
    private UrlParamsFactoryImpl urlParamsFactoryImpl;

    @Mock
    private ClientHelper clientHelper;

    @Mock
    private GeoRegionService geoRegionService;

    private ReportParserFactory factory;

    @Mock
    private BlueRule blueRule;

    @Mock
    private BlueMobileApplicationRule blueMobileApplicationRule;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        factory = new ReportParserFactory(
            currencyService,
            null,
            geoRegionService,
            null,
            new FilterFactory(),
            marketUrls,
                urlParamsFactoryImpl,
            clientHelper,
            blueRule,
            blueMobileApplicationRule
        );

        context.setUrlSchema(UrlSchema.HTTP);
    }

    @Test
    public void shouldParseSkuData() {
        Sku sku = parse("sku-data.json", Collections.emptyList());
        assertThat(
            sku,
            sku(
                SkuMatcher.id("SecondSku"),
                SkuMatcher.deletedId("RemovedSku"),
                name("blue offer sku2"),
                description("desc")
            )
        );
    }

    @Test
    public void shouldParsePhotos() {
        Sku sku = parse("sku-photos.json", Collections.singleton(SkuField.PHOTOS));

        assertThat(
            sku,
            sku(
                photos(
                    contains(
                        cast(
                            image(
                                "http://0.cs-ellpic01gt.yandex.ru:88/market_iyC3nHslqLtqZJLygVAHeA_1x1.jpg",
                                100,
                                100
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldParseModel() {
        Sku sku = parse(
            "sku-model.json",
            Arrays.asList(SkuField.MODEL, ModelInfoField.PHOTOS)
        );

        assertThat(
            sku,
            sku(
                model(
                    id(1716412316L),
                    ModelMatcher.photos(
                        containsInAnyOrder(
                            (Matcher<ImageWithThumbnails>) image(
                                "http://avatars.mds.yandex.net/get-mpic/397397/img_id4028138173056822093.jpeg/orig",
                                467,
                                701
                            ),
                            (Matcher<ImageWithThumbnails>) image(
                                "http://avatars.mds.yandex.net/get-mpic/331398/img_id6584093594260664971.jpeg/orig",
                                701,
                                695
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldParseOffer() {
        Sku sku = parse("sku-offers.json", Collections.singleton(SkuField.OFFERS));

        assertThat(
            sku,
            sku(
                offers(
                     contains(
                        offer(
                            offerId(
                                wareMd5("Sku2Price50-iLVm1Goleg")
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldParseFitler() {
        Sku sku = parse("sku-filters.json", Collections.singleton(SkuField.FILTERS));

        Matcher<Filter> filter1 = FiltersMatcher.filter(
            FiltersMatcher.values(
                containsInAnyOrder(
                    FilterValueMatcher.sku("1"),
                    FilterValueMatcher.sku("2"),
                    FilterValueMatcher.sku("3")
                )
            )
        );

        Matcher<Filter> filter2 = FiltersMatcher.filter(
            FiltersMatcher.values(
                containsInAnyOrder(
                    FilterValueMatcher.sku("4"),
                    FilterValueMatcher.sku("5")
                )
            )
        );

        assertThat(
            sku,
            filters(
                containsInAnyOrder(
                    filter1,
                    filter2
                )
            )
        );

    }

    @Test
    public void shouldParseFormattedDescription() {
        Sku sku = parse("sku-formatted-description.json", Collections.emptyList());

        assertThat(
                sku,
                sku(
                    formattedDescription(
                            FormattedDescriptionMatcher.fullHtml("<html><b>This is test sku</b> Stub sku created by hands</html>"),
                            FormattedDescriptionMatcher.shortHtml("<html><b>This is test sku</b>"),
                            FormattedDescriptionMatcher.fullPlain("This is test sku. Stub sku created by hands"),
                            FormattedDescriptionMatcher.shortPlain("This is test sku")
                    )
                )
        );
    }

    @Test
    public void shouldParseSkuWarnings() {
        Sku sku = parse("sku-warning.json", Collections.emptyList());

        assertThat(
            sku,
            sku(
                warnings(
                    Matchers.containsInAnyOrder(
                        WarningMatcher.warnings(
                            WarningMatcher.code("adult"),
                            WarningMatcher.text("Возрастное ограничение")
                        )
                    )
                )
            )
        );
    }

    @Test
    public void shouldParseSkuVendor() {
        Sku sku = parse("sku-with-vendor.json", Collections.singleton(SkuField.VENDOR));

        assertNotNull(sku.getVendor());
    }

    @Test
    public void shouldParseSkuType() {
        Sku sku = parse("partner-sku.json", Collections.emptyList());

        assertEquals("partner", sku.getSkuType());
    }

    private Sku parse(String filename, Collection<? extends Field> fields) {
        Parser<Sku> skuParser = new SkuParser(
            factory,
            new ReportRequestContext().setFields(fields),
            marketUrls
        );
        return skuParser.parse(ResourceHelpers.getResource(filename));
    }

    private static <In, Out> Matcher<Out> cast(Matcher<? extends In> matcher) {
        return (Matcher<Out>) matcher;
    }
}
