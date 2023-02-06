package ru.yandex.market.api.integration;

import org.junit.Test;
import ru.yandex.market.api.controller.v2.url.TouchUrlController;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.ContentType;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PageType;
import ru.yandex.market.api.domain.v2.RedirectContentType;
import ru.yandex.market.api.domain.v2.redirect.OfferRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.RedirectV2;
import ru.yandex.market.api.error.NotFoundException;
import ru.yandex.market.api.domain.v2.redirect.parameters.EnrichParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.FilterParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.UrlUserParams;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.user.order.ShopOfferId;
import ru.yandex.market.api.util.httpclient.clients.ClckTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 */
public class TouchUrlControllerTest extends BaseTest {
    @Inject
    private TouchUrlController urlController;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private ClckTestClient clckTestClient;

    private static final Client MOBILE = new Client() {{
        setType(Type.MOBILE);
        setShowShopUrl(true);
    }};

    /**
     * Проверяем, что корректно обрабатываем урлы на чекаут
     * (На данный момент приложение хочет получать редиректы на чекаут в видре редиректа на оффер)
     */
    @Test
    public void testCheckoutRedirect() {
        context.setClient(MOBILE);

        UrlUserParams userParams = userParams("url");
        FilterParams filterParams = filterParams(EnumSet.allOf(RedirectContentType.class));
        EnrichParams enrichParams = enrichParams();

        ShopOfferId shopOfferId = new ShopOfferId(1, "id1");

        reportTestClient.getOfferByShopOfferId(shopOfferId, "checkout_for_touch_url.json");
        reportTestClient.getOfferInfo(OfferId.fromWareMd5("ware1"), "checkout_for_touch_url.json");
        clckTestClient.clck("https://clck.ru/Q1");

        RedirectV2 redirect = urlController
                              .checkout(shopOfferId, userParams, filterParams, enrichParams, genericParams)
                              .waitResult()
                              .getRedirect();

        assertTrue(redirect instanceof OfferRedirectV2);

        OfferRedirectV2 offerRedirect = (OfferRedirectV2) redirect;

        assertEquals("ware1", offerRedirect.getOfferId().getWareMd5());
        assertEquals("fee1", offerRedirect.getOfferId().getFeeShow());

        OfferV2 offerV2 = offerRedirect.getContent().getOffer();

        assertEquals("https://market-click2.yandex.ru/redir/enc", offerV2.getUrl());
        assertEquals("https://market-click2.yandex.ru/redir/cpa", offerV2.getCpaUrl());
    }

    @Test(expected = NotFoundException.class)
    public void testCheckoutRedirectWhenOfferNotFound() {
        context.setClient(MOBILE);

        UrlUserParams userParams = userParams("url");
        FilterParams filterParams = filterParams(EnumSet.allOf(RedirectContentType.class));
        EnrichParams enrichParams = enrichParams();

        ShopOfferId shopOfferId = new ShopOfferId(1, "id1");

        reportTestClient.getOfferByShopOfferId(shopOfferId, "checkout_for_touch_url_empty.json");

        urlController
            .checkout(shopOfferId, userParams, filterParams, enrichParams, genericParams)
            .waitResult();
    }

    private UrlUserParams userParams(String url) {
        UrlUserParams userParams = new UrlUserParams();
        userParams.setUrl(url);
        return userParams;
    }

    private FilterParams filterParams(Collection<? extends ContentType> contents) {
        FilterParams filterParams = new FilterParams();
        filterParams.setRedirectTypes(EnumSet.allOf(PageType.class));
        filterParams.setContents(contents);
        return filterParams;
    }

    private EnrichParams enrichParams() {
        EnrichParams enrichParams = new EnrichParams();
        enrichParams.setSort(UniversalModelSort.POPULARITY_SORT);
        enrichParams.setUserAgent("inttest");
        enrichParams.setPageInfo(PageInfo.DEFAULT);
        enrichParams.setFields(Collections.emptyList());
        return enrichParams;
    }

}
