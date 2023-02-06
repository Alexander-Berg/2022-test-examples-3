package ru.yandex.market.api.internal.guru.parser;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.common.url.MarketUrls;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.domain.v2.promo.BannerListWidget;
import ru.yandex.market.api.domain.v2.promo.BillboardWidget;
import ru.yandex.market.api.domain.v2.promo.CategoryListWidget;
import ru.yandex.market.api.domain.v2.promo.HeadlineWidget;
import ru.yandex.market.api.domain.v2.promo.PromoLink;
import ru.yandex.market.api.domain.v2.promo.PromoPage;
import ru.yandex.market.api.domain.v2.promo.VendorListWidget;
import ru.yandex.market.api.geo.GeoRegionService;
import ru.yandex.market.api.geo.domain.GeoRegion;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.matchers.ImageMatcher;
import ru.yandex.market.api.matchers.MixedDatasourceMatcher;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.version.RegionVersion;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.adPlace;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.adfoxBanner;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.image;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.imageBanner;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.link;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.p2;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.pp;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.puidName;
import static ru.yandex.market.api.matchers.MixedDatasourceMatcher.puidValue;

/**
 * Created by apershukov on 17.10.16.
 */
@WithContext
@WithMocks
public class PromoPageParserTest extends BaseTest {

    private PromoPageParser parser;

    @Inject
    private MarketUrls marketUrls;

    @Mock
    private GeoRegionService geoRegionService;


    @Before
    public void setUp() {
        ContextHolder.get().setClient(new Client() {{
            setType(Type.MOBILE);
        }});

        Mockito
            .when(
                geoRegionService.getRegion(
                    Mockito.anyInt(),
                    Mockito.anyCollection(),
                    Mockito.any(RegionVersion.class)
                )
            )
            .thenReturn(
                new GeoRegion(1, null, null, null, null)
            );
        parser = new PromoPageParser(false, UserHistoryType.MODEL, marketUrls);
    }

    @Test
    public void testParsePageWithMobileTitleForMobile() {
        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-page1.xml"));

        HeadlineWidget headlineWidget = (HeadlineWidget) page.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        assertEquals("<p>Mobile title</p>", headlineWidget.getTitle());
        assertEquals("<p>Mobile subtitle</p>", headlineWidget.getSubtitle());
    }

    @Test
    public void testParsePageWithMobileTitle() {
        ContextHolder.get().setClient(new Client());

        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-page1.xml"));

        HeadlineWidget headlineWidget = (HeadlineWidget) page.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        assertEquals("<p>Как выбрать средство от сезонной аллергии</p>", headlineWidget.getTitle());
        assertEquals("<p>Советы врача аллерголога-иммунолога</p>", headlineWidget.getSubtitle());
    }

    @Test
    public void testParsePageWithoutMobileTitle() {
        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-page2.xml"));

        HeadlineWidget headlineWidget = (HeadlineWidget) page.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        assertEquals("<p>Как выбрать средство от сезонной аллергии</p>", headlineWidget.getTitle());
        assertEquals("<p>Советы врача аллерголога-иммунолога</p>", headlineWidget.getSubtitle());
    }

    @Test
    public void testBillboardCampaign() {
        // Настройка системы
        Context context = ContextHolder.get();
        context.setClientVersionInfo(new KnownMobileClientVersionInfo(
            Platform.ANDROID,
            DeviceType.SMARTPHONE,
            new SemanticVersion(3, 0, 0)
        ));

        // Вызов системы
        PromoPage page = parser.parse(ResourceHelpers.getResource("promoPageRoot.xml"));

        // Проверка утверждений
        BillboardWidget widget = (BillboardWidget) page.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        Assert.assertEquals("Должны получить настройки для телефонов", "41540", widget.getDatasource().getCampaign());
    }

    @Test
    public void testIpadBillboardCampaign() {
        // Настройка системы
        Context context = ContextHolder.get();
        context.setClientVersionInfo(new KnownMobileClientVersionInfo(
                Platform.ANDROID,
                DeviceType.IPAD,
                new SemanticVersion(3, 0, 0)
        ));

        // Вызов системы
        PromoPage page = parser.parse(ResourceHelpers.getResource("promoPageRoot.xml"));

        // Проверка утверждений
        BillboardWidget widget = (BillboardWidget) page.getContent().getRows().get(0)
                .getColumns().get(0)
                .getWidgets().get(0);

        Assert.assertEquals("Должны получить настройки для телефонов", "44356", widget.getDatasource().getCampaign());
    }

    @Test
    public void popularBrandsTest() {
        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-popular-brands.xml"));

        VendorListWidget widget = (VendorListWidget) page.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        VendorListWidget.PopularVendorsDatasource datasource = widget.getDatasource();

        assertEquals(90401, datasource.getHid());
        assertEquals(24, datasource.getCount());
    }

    @Test
    public void childrenCategories() {
        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-categories-children.xml"));

        CategoryListWidget<CategoryListWidget.ChildrenCategoriesDatasource> widget =
            (CategoryListWidget<CategoryListWidget.ChildrenCategoriesDatasource>)
                page.getContent().getRows().get(0)
                    .getColumns().get(0)
                    .getWidgets().get(0);

        assertThat(widget.getDatasource(), Matchers.isA(CategoryListWidget.ChildrenCategoriesDatasource.class));

        assertThat(widget.getCount(), Matchers.is(5));
    }

    @Test
    public void mediaSet() {
        PromoPage page = parser.parse(ResourceHelpers.getResource("promo-media-set.xml"));
        BannerListWidget<PromoLink, BannerListWidget.MixedDatasource> widget =
                (BannerListWidget<PromoLink, BannerListWidget.MixedDatasource>) page.getContent().getRows().get(0)
                        .getColumns().get(0)
                        .getWidgets().get(0);

        Matcher<BannerListWidget.MixedDatasource.ImageBanner> imageBanner = imageBanner(
                image(
                        cast(
                                ImageMatcher.image(
                                        "http://avatars.mds.yandex.net/1.jpg/orig",
                                        853,
                                        236
                                )
                        )
                ),
                link("http://market.yandex.ru/catalog/54436?hid=91512")
        );


        Matcher<BannerListWidget.MixedDatasource.AdfoxBanner> adfoxBanner = adfoxBanner(
                adPlace("53"),
                pp("g"),
                p2("fpyt"),
                puidName("someName"),
                puidValue("12345")
        );

        assertThat(
                widget.getDatasource(),
                MixedDatasourceMatcher.banners(
                        Matchers.containsInAnyOrder(
                                cast(imageBanner),
                                cast(adfoxBanner)
                        )
                )
        );
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }
}
