package ru.yandex.market.partner.mvc.controller.offer.mapping;

import java.util.Collections;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import ru.yandex.market.partner.test.context.FunctionalTest;

@ParametersAreNonnullByDefault
class AbstractMappingControllerFunctionalTest extends FunctionalTest {

    static final long CAMPAIGN_ID = 10774L;
    static final long SUPPLIER_ID = 774L;

    @Nonnull
    static HttpEntity<?> json(String requestText) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        return new HttpEntity<>(requestText, headers);
    }

    String mappedMarketSkuUrl(long campaignId) {
        return String.format("%s/market-skus", mappingUrl(campaignId));
    }

    String mappingUrl(long campaignId) {
        return String.format("%s/campaigns/%d/offer-mapping", baseUrl, campaignId);
    }

    String shopSkusUrl(long campaignId) {
        return String.format("%s/shop-skus", mappingUrl(campaignId));
    }

    String getShopSkusCount(long campaignId) {
        return String.format("%s/shop-skus-count", mappingUrl(campaignId));
    }

    String listShopSkusUrl(long campaignId) {
        return String.format("%s/list-shop-skus", mappingUrl(campaignId));
    }

    String shopSkusAsXlsUrl(long campaignId) {
        return String.format("%s/shop-skus/xls", mappingUrl(campaignId));
    }

    String shopSkuCategoriesUrl(long campaignId) {
        return String.format("%s/categories", shopSkusUrl(campaignId));
    }

    String shopSkuOfferIntegralStatusesUrl(long campaignId) {
        return String.format("%s/offer-integral-statuses", shopSkusUrl(campaignId));
    }

    String shopSkuOfferProcessingStatusesUrl(long campaignId) {
        return String.format("%s/offer-processing-statuses", shopSkusUrl(campaignId));
    }

    private String byShopSkuUrl(long campaignId) {
        return String.format("%s/by-shop-sku", shopSkusUrl(campaignId));
    }

    String byShopSkuOfferProcessingStatusesUrl(long campaignId) {
        return String.format("%s/offer-processing-status", byShopSkuUrl(campaignId));
    }

    String shopSkuCategoryContentTemplateUrl(long campaignId, int categoryId) {
        return String.format("%s/%d/content-template", shopSkuCategoriesUrl(campaignId), categoryId);
    }

    String searchVendorsUrl(long campaignId) {
        return String.format("%s/vendors", mappingUrl(campaignId));
    }
}
