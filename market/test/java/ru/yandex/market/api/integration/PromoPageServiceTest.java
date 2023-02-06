package ru.yandex.market.api.integration;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Inject;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.hamcrest.Matcher;
import org.junit.Test;

import ru.yandex.market.api.domain.NavigationNode;
import ru.yandex.market.api.domain.v2.CategoryV2;
import ru.yandex.market.api.domain.v2.VendorV2;
import ru.yandex.market.api.domain.v2.promo.BannerListWidget;
import ru.yandex.market.api.domain.v2.promo.CategoryListWidget;
import ru.yandex.market.api.domain.v2.promo.PromoLink;
import ru.yandex.market.api.domain.v2.promo.PromoPage;
import ru.yandex.market.api.domain.v2.promo.VendorListWidget;
import ru.yandex.market.api.geo.GeoUtils;
import ru.yandex.market.api.internal.cataloger.RootNidResolver;
import ru.yandex.market.api.matchers.CategoryMatcher;
import ru.yandex.market.api.matchers.ImageMatcher;
import ru.yandex.market.api.service.PromoPageService;
import ru.yandex.market.api.util.concurrent.Futures;
import ru.yandex.market.api.util.httpclient.clients.CatalogerTestClient;
import ru.yandex.market.api.util.httpclient.clients.TarantinoTestClient;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.map;
import static ru.yandex.market.api.matchers.PromoLinkMatcher.image;
import static ru.yandex.market.api.matchers.PromoLinkMatcher.promoLink;
import static ru.yandex.market.api.matchers.PromoLinkMatcher.url;

/**
 * Created by tesseract on 10.03.17.
 */
public class PromoPageServiceTest extends BaseTest {

    @Inject
    TarantinoTestClient tarantinoTestClient;
    @Inject
    PromoPageService promoPageService;
    @Inject
    CatalogerTestClient catalogerTestClient;
    @Inject
    RootNidResolver rootNidResolver;

    /**
     * Проверяем правильность установки домена в запросах к Тарантино для Российских регионов
     */
    @Test
    public void checkDomainRu() {
        // настройка системы
        context.getRegionInfo().setRawRegionId(GeoUtils.Region.MOSCOW);

        tarantinoTestClient.navigationPage("58602", "ru", "tarantinoContextPage_58602.xml");
        // вызов системы
        PromoPage result = Futures.waitAndGet(promoPageService.getNavigationPage("58602", "test", Collections.emptyList(), null, genericParams));
        // проверка утверждений
        assertNotNull(result);
    }

    /**
     * Проверяем правильность установки домена в запросах к Тарантино для Белорусских регионов
     */
    @Test
    public void checkDomainBy() {
        // настройка системы
        context.getRegionInfo().setRawRegionId(GeoUtils.Region.MINSK);

        tarantinoTestClient.navigationPage("58602", "by", "tarantinoContextPage_58602.xml");
        // вызов системы
        PromoPage result = Futures.waitAndGet(promoPageService.getNavigationPage("58602", "test", Collections.emptyList(), null, genericParams));
        // проверка утверждений
        assertNotNull(result);
    }

    @Test
    public void checkPopularVendorsEnrich() {
        tarantinoTestClient.navigationPage(
            String.valueOf(rootNidResolver.getRootNid()),
            "tarantino-popular-brands.xml"
        );

        catalogerTestClient.getPopularBrands(
            90401,
            2,
            "cataloger_popular_brands_90401.json"
        );

        catalogerTestClient.getVendors(
            new LongArrayList(Arrays.asList(153061L, 153043L)),
            "cataloger_brand_info_153061_153043.json"
        );

        PromoPage result = Futures.waitAndGet(
            promoPageService.getNavigationPage(
                String.valueOf(rootNidResolver.getRootNid()),
                "test",
                Collections.emptyList(),
                null,
                genericParams
            )
        );

        VendorListWidget widget = (VendorListWidget) result.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        assertThat(
            widget.getVendors(),
            containsInAnyOrder(
                vendor(153061L),
                vendor(153043L)
            )
        );

        assertThat(
            (CategoryV2) widget.getDatasource().getCategory(),
            CategoryMatcher.id(90401)
        );
    }

    @Test
    public void checkChildrenCategoriesEnrich() {
        tarantinoTestClient.navigationPage(
            String.valueOf(rootNidResolver.getRootNid()),
            "tarantino-categories-children.xml"
        );

        catalogerTestClient.getTree(
            rootNidResolver.getRootNid(),
            1,
            213,
            "cataloger_categories_children.xml"
        );

        PromoPage result = Futures.waitAndGet(
            promoPageService.getNavigationPage(
                String.valueOf(rootNidResolver.getRootNid()),
                "test",
                Collections.emptyList(),
                null,
                genericParams
            )
        );

        CategoryListWidget widget = (CategoryListWidget) result.getContent().getRows().get(0)
            .getColumns().get(0)
            .getWidgets().get(0);

        assertThat(
            (Collection<NavigationNode>)widget.getCategories(),
            containsInAnyOrder(
                navigationNode(54440),
                navigationNode(54425),
                navigationNode(54419),
                navigationNode(54432),
                navigationNode(54422)
            )
        );

        CategoryListWidget.ChildrenCategoriesDatasource datasource =
            (CategoryListWidget.ChildrenCategoriesDatasource) widget.getDatasource();

        assertThat(
            datasource.getNid(),
            is(rootNidResolver.getRootNid())
        );
    }

    @Test
    public void mediaSet() {
        tarantinoTestClient.navigationPage(
            String.valueOf(rootNidResolver.getRootNid()),
            "tarantino-media-set.xml"
        );

        PromoPage result = Futures.waitAndGet(
            promoPageService.getNavigationPage(
                String.valueOf(rootNidResolver.getRootNid()),
                "test",
                Collections.emptyList(),
                null,
                genericParams
            )
        );

        BannerListWidget<PromoLink, BannerListWidget.MixedDatasource> widget =
            (BannerListWidget<PromoLink, BannerListWidget.MixedDatasource>) result.getContent()
                .getRows().get(0)
                .getColumns().get(0)
                .getWidgets().get(0);
        assertThat(
            widget.getBanners(),
            hasItem(
                promoLink(
                    image(
                        cast(
                            ImageMatcher.image(
                                "https://avatars.mds.yandex.net/1.jpg/orig",
                                853,
                                236
                            )
                        )
                    ),
                    url("https://market.yandex.ru/catalog/54436?hid=91512")
                )
            )
        );

    }

    private static Matcher<NavigationNode> navigationNode(int id) {
        return map(
            NavigationNode::getId,
            "'id'",
            is(id),
            n -> String.format("NavigationNode{id='%d'}", n.getId())
        );
    }

    private static Matcher<VendorV2> vendor(long id) {
        return map(
            VendorV2::getId,
            "'id'",
            is(id),
            v -> String.format("Vendor{id='%d'}", v.getId())
        );
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }

}
