package ru.yandex.market.partner.mvc.controller.banner;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.core.banner.model.BannerDisplayType;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

class PartnerBannersControllerFunctionalTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "shopHasBannersTest.before.csv")
    @DisplayName("У магазина есть баннеры")
    void shopHasBannersTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(10001L);
        JsonTestUtil.assertEquals(response, this.getClass(), "shop-has-banners-response.json");
    }

    @Test
    @DbUnitDataSet(before = "shopHasBannersTest.before.csv")
    @DisplayName("У магазина есть баннеры с типом отображения на сводке")
    void shopHasBannersWithDisplayTypesTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(10001L, EnumSet.of(BannerDisplayType.ADVERTISING));
        JsonTestUtil.assertEquals(response, this.getClass(), "shop-has-banners-with-display-types.json");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("У поставщика есть баннеры с типом отображения на сводке")
    void supplierHasBannersWithDisplayTypesTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(10001L, EnumSet.of(BannerDisplayType.ADVERTISING));
        JsonTestUtil.assertEquals(response, this.getClass(), "supplier-has-banners-with-display-types.json");
    }

    @Test
    @DbUnitDataSet(before = "shopHasNoBannersTest.before.csv")
    @DisplayName("У магазина нет баннеров")
    void shopHasNoBannersTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(20002L);
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("У поставщика есть баннеры по страницам по умолчанию (сводка, цены)")
    void supplierHasBannersByPageDefaultTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(1001L);
        JsonTestUtil.assertEquals(response, this.getClass(), "supplier-has-banners-response-by-page-default.json");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("У поставщика есть баннеры по заданной странице")
    void supplierHasBannersByPageIdTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(1001L, "page2");
        JsonTestUtil.assertEquals(response, this.getClass(), "supplier-has-banners-response-by-page-id.json");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasNoBanners.before.csv")
    @DisplayName("У поставщика нет баннеров")
    void supplierHasNoBannersBySupplierTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(2002L);
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasNoBanners.before.csv")
    @DisplayName("У поставщика нет баннеров по заданной странице")
    void supplierHasNoBannersByPageTest() {
        ResponseEntity<String> response = getPartnerBannersInfo(1001L, "page1");
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("Не возвращаем баннеры, т.е. не указан тип партнёра и сама компания")
    void noBannersByPageIdWithoutCampaignType() {
        ResponseEntity<String> response = getPartnerBannersInfo(-1L, "page2");
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("Возвращаем все баннеры для заданной страницы")
    void bannersByPageIdWithCampaignTypeNone() {
        ResponseEntity<String> response = getPartnerBannersInfo(-1L, "supplier", "page2");
        JsonTestUtil.assertEquals(response, "{\"banners\":[]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("Возвращаем все баннеры для заданной страницы")
    void bannersByPageIdWithCampaignTypeFound() {
        ResponseEntity<String> response = getPartnerBannersInfo(-1L, "supplier", "page4");
        JsonTestUtil.assertEquals(response, "{\"banners\":[{\"id\":\"banner-4\",\"appearance\":{\"isPermanent\":true," +
                "\"text\":\"text-4\",\"severity\":1},\"isPermanent\":true,\"text\":\"text-4\",\"severity\":1}]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("Возвращаем все баннеры для страниц по умолчанию (сводка, цены)")
    void noBannersByPageIdWithCampaignTypeAndWithoutPageId() {
        ResponseEntity<String> response = getPartnerBannersInfo(-1L, "supplier", "");
        JsonTestUtil.assertEquals(response, "{\"banners\":[{\"id\":\"banner-5\",\"appearance\":{\"isPermanent\":true," +
                "\"text\":\"text-5\",\"severity\":1},\"isPermanent\":true,\"text\":\"text-5\",\"severity\":1}]}");
    }

    @Test
    @DbUnitDataSet(before = "supplierHasBannersTest.before.csv")
    @DisplayName("У бизнеса есть баннеры")
    void businessHasBannersTest() {
        ResponseEntity<String> response = getPartnerBannersInfoByBusiness(3L);
        JsonTestUtil.assertEquals(response, this.getClass(), "business-has-banners.json");
    }

    private ResponseEntity<String> getPartnerBannersInfo(long campaignId) {
        return FunctionalTestHelper.get(baseUrl + "/partner/banners?campaign_id=" + campaignId);
    }

    private ResponseEntity<String> getPartnerBannersInfo(long campaignId, String pageId) {
        return FunctionalTestHelper.get(
                String.format(baseUrl + "/partner/banners?campaign_id=%s&page_id=%s", campaignId, pageId));
    }

    private ResponseEntity<String> getPartnerBannersInfo(long campaignId, String campaignType, String pageId) {
        return FunctionalTestHelper.get(
                String.format(baseUrl + "/partner/banners?campaign_id=%s&campaign_type=%s&page_id=%s", campaignId, campaignType, pageId));
    }

    private ResponseEntity<String> getPartnerBannersInfo(long campaignId, Set<BannerDisplayType> displayTypes) {
        return FunctionalTestHelper.get(baseUrl + "/partner/banners?campaign_id=" + campaignId + "&display_types=" +
                displayTypes.stream().map(Enum::name).collect(Collectors.joining(",")));
    }

    private ResponseEntity<String> getPartnerBannersInfoByBusiness(long businessId) {
        return FunctionalTestHelper.get(baseUrl + "/partner/banners?businessId=" + businessId);
    }
}
