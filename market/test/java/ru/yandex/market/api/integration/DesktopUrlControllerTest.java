package ru.yandex.market.api.integration;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.controller.v2.url.DesktopUrlController;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.ContentType;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.PageType;
import ru.yandex.market.api.domain.v2.RedirectContentType;
import ru.yandex.market.api.domain.v2.redirect.OfferRedirectV2;
import ru.yandex.market.api.domain.v2.redirect.RedirectV2;
import ru.yandex.market.api.domain.v2.redirect.parameters.EnrichParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.FilterParams;
import ru.yandex.market.api.domain.v2.redirect.parameters.UrlUserParams;
import ru.yandex.market.api.model.UniversalModelSort;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.util.httpclient.clients.ClckTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author dimkarp93
 */
public class DesktopUrlControllerTest extends BaseTest {
    @Inject
    DesktopUrlController urlController;

    @Inject
    ReportTestClient reportTestClient;

    @Inject
    ClckTestClient clckTestClient;

    private static final Client MOBILE = new Client() {{
        setType(Type.MOBILE);
        setShowShopUrl(true);
    }};

    /**
     * Проверяем, что корректно обрабатыавем урл на оффер (по wareMd5)
     */
    @Test
    public void testOfferRedirectByWareMd5() {
        context.setClient(MOBILE);

        String wareMd5 = "id1";
        OfferId offerId = OfferId.fromWareMd5(wareMd5);

        reportTestClient.getOfferInfo(offerId, "offer_for_url.json");
        clckTestClient.clck("https://clck.ru/Q1");

        RedirectV2 redirectV2 = urlController
            .getOffer(wareMd5,
                userParams("url"),
                filterParams(EnumSet.allOf(RedirectContentType.class)),
                enrichParams(),
                genericParams)
            .waitResult()
            .getRedirect();


        assertTrue(redirectV2 instanceof OfferRedirectV2);

        OfferRedirectV2 offerRedirectV2 = (OfferRedirectV2) redirectV2;

        assertEquals(wareMd5, offerRedirectV2.getOfferId().getWareMd5());
        assertEquals("fee1", offerRedirectV2.getOfferId().getFeeShow());

        OfferV2 offerV2 = offerRedirectV2.getContent().getOffer();

        assertEquals("https://market-click2.yandex.ru/redir/enc", offerV2.getUrl());
        assertEquals("https://market-click2.yandex.ru/redir/cpa", offerV2.getCpaUrl());
    }

    /**
     * Проверяем, что корректно заполняем параметр shortLink
     */
    @Test
    public void testShortLink() {
        String wareMd5 = "id1";
        OfferId offerId = OfferId.fromWareMd5(wareMd5);
        String shortLink = UUID.randomUUID().toString();

        // настройка системы
        context.setClient(MOBILE);
        reportTestClient.getOfferInfo(offerId, "offer_for_url.json");
        clckTestClient.clck("https://market.yandex.ru/offer/id1?pp=37&hid=90409&cpc=FFEc8TSg6uxkltjVntoIUQ_14-2E-aob26YXMmwuBona_r098vr9EjMSHQ45H1j7zpiI4r4c7D1v4ne_g4olRA%2C%2C&lr=213", shortLink);

        // вызов системы
        RedirectV2 redirectV2 = urlController
            .getOffer(wareMd5,
                userParams("url"),
                filterParams(EnumSet.allOf(RedirectContentType.class)),
                enrichParams(),
                genericParams)
            .waitResult()
            .getRedirect();

        // проверка утверждений
        Assert.assertEquals(shortLink, redirectV2.getShortLink());
    }

    /**
     * Проверяем, что корректно обрабатыавем урл на оффер (по offerId и не идем по этому случаю в репорт,
     * когда не нужен контекст)
     */
    @Test
    public void testOfferRedirectByOfferId() {
        context.setClient(MOBILE);

        String wareMd5 = "id1";
        String feeShow = "fee1";
        OfferId offerId = new OfferId(wareMd5, feeShow);

        reportTestClient.getOfferInfo(offerId, "offer_for_url.json");
        clckTestClient.clck("https://clck.ru/Q1");

        RedirectV2 redirectV2 = urlController
            .getOffer(wareMd5,
                      userParams("url"),
                      filterParams(EnumSet.allOf(RedirectContentType.class)),
                      enrichParams(),
                      genericParams)
            .waitResult()
            .getRedirect();


        assertTrue(redirectV2 instanceof OfferRedirectV2);

        OfferRedirectV2 offerRedirectV2 = (OfferRedirectV2) redirectV2;

        assertEquals(wareMd5, offerRedirectV2.getOfferId().getWareMd5());
        assertEquals(feeShow, offerRedirectV2.getOfferId().getFeeShow());

        OfferV2 offerV2 = offerRedirectV2.getContent().getOffer();

        assertEquals("https://market-click2.yandex.ru/redir/enc", offerV2.getUrl());
        assertEquals("https://market-click2.yandex.ru/redir/cpa", offerV2.getCpaUrl());
    };

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
        enrichParams.setFields(Collections.emptyList());
        enrichParams.setUserAgent("inttest");
        enrichParams.setSort(UniversalModelSort.POPULARITY_SORT);
        enrichParams.setPageInfo(PageInfo.DEFAULT);
        return enrichParams;
    }


}
