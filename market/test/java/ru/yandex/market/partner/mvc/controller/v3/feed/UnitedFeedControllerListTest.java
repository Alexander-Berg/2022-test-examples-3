package ru.yandex.market.partner.mvc.controller.v3.feed;

import java.net.URISyntaxException;

import javax.annotation.Nonnull;

import org.apache.http.client.utils.URIBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.partner.mvc.controller.feed.model.FeedContentTypeDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

/**
 * Тесты списка фидов для {@link UnitedFeedController}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class UnitedFeedControllerListTest extends FunctionalTest {

    @Test
    @DisplayName("Получить все ассортиментные фиды бизнеса")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBusinessAll.before.csv")
    void testBusinessAll() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildBusinessListUrl(3001L, null));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBusinessAll.response.json");
    }

    @Test
    @DisplayName("Получить все фиды бизнеса")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBusinessAll.before.csv")
    void testBusinessAllFeeds() throws URISyntaxException {
        ResponseEntity<String> response =
                FunctionalTestHelper.get(buildBusinessListUrl(3001L, null, FeedContentTypeDTO.values()));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBusinessAllFeeds.response.json");
    }

    @Test
    @DisplayName("Получить все фиды бизнеса по ссылке")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBusinessAll.before.csv")
    void testBusinessUrl() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildBusinessListUrl(3001L, "URL"));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBusinessUrl.response.json");
    }

    @Test
    @DisplayName("Получить все белые фиды")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteAll.before.csv")
    void testWhiteAll() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, null));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testWhiteAll.response.json");
    }

    @Test
    @DisplayName("Получить белые аплоадные фиды")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteAll.before.csv")
    void testUploadFeeds() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, "UPLOAD"));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testUploadFeeds.response.json");
    }

    @Test
    @DisplayName("Получить белые фиды по ссылке")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteAll.before.csv")
    void testUrlFeeds() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, "URL"));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testUrlFeeds.response.json");
    }

    @Test
    @DisplayName("У синего только дефолтный фид. Пустой ответ")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefault.before.csv")
    void testBlueDefault() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, null));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueDefault.response.json");
    }

    @Test
    @DisplayName("У синего фид по ссылке")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUrl.before.csv")
    void testBlueUrl() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, null));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueUrl.response.json");
    }

    @Test
    @DisplayName("У синего аплоадный фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUpload.before.csv")
    void testBlueUpload() throws URISyntaxException {
        ResponseEntity<String> response = FunctionalTestHelper.get(buildCampaignListUrl(1001L, null));
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueUpload.response.json");
    }

    @Test
    @DisplayName("Получить синий стоковый фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUtilityFeeds.before.csv")
    void getLatestFeedInfo_stockType_stockFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.STOCK);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueStock.response.json");
    }

    @Test
    @DisplayName("Получить синий ценовой фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUtilityFeeds.before.csv")
    void getLatestFeedInfo_priceType_priceFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.PRICE);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBluePrice.response.json");
    }

    @Test
    @DisplayName("Получить белый стоковый фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteUtilityFeeds.before.csv")
    void getLatestFeedInfo_stockType_shopStockFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.STOCK);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testWhiteStock.response.json");
    }

    @Test
    @DisplayName("Получить белый ценовой фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteUtilityFeeds.before.csv")
    void getLatestFeedInfo_priceType_shopPriceFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.PRICE);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testWhitePrice.response.json");
    }

    @Test
    @DisplayName("Получить синий стоковый и ассортиментный фиды")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUtilityFeeds.before.csv")
    void getLatestFeedInfo_blueStockAndAssortment_stockAndAssortmentFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.STOCK,
                FeedContentTypeDTO.ASSORTMENT_WITH_PRICES);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response,
                "UnitedFeedController/json/getLatestFeedInfo/testBlueStockAndAssortment.response.json");
    }

    @Test
    @DisplayName("Получить синий ценовой и ассортиментный фиды")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueUtilityFeeds.before.csv")
    void getLatestFeedInfo_bluePriceAndAssortment_priceAndAssortmentFeedInfo() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, FeedContentTypeDTO.PRICE,
                FeedContentTypeDTO.ASSORTMENT_WITH_PRICES);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response,
                "UnitedFeedController/json/getLatestFeedInfo/testBluePriceAndAssortment.response.json");
    }

    @Test
    @DisplayName("Получить все фиды бизнеса, вместе с дефолтным")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBusinessAll.before.csv")
    void testBusinessAllWithDefault() throws URISyntaxException {
        String url = buildBusinessListUrl(3001L, null, true, FeedContentTypeDTO.values());
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response,
                "UnitedFeedController/json/getLatestFeedInfo/testBusinessAllWithDefault.response.json");
    }

    @Test
    @DisplayName("Получить все белые фиды, вместе с дефолтным")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testWhiteAll.before.csv")
    void testWhiteAllWithDefault() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true, FeedContentTypeDTO.values());
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testWhiteAllWithDefault.response.json");
    }

    @Test
    @DisplayName("Получить дефолтный синий фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefaultWithUtility.before.csv")
    void testBlueDefaultAssortment() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueDefaultReturned.response.json");
    }

    @Test
    @DisplayName("Получить дефолтный синий фид и ценовой фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefaultWithUtility.before.csv")
    void testBlueDefaultAssortmentAndPrices() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true,
                FeedContentTypeDTO.ASSORTMENT_WITH_PRICES, FeedContentTypeDTO.PRICE);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBluePriceAndDefault.response.json");
    }

    @Test
    @DisplayName("Получить дефолтный синий фид и стоковый фид")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefaultWithUtility.before.csv")
    void testBlueDefaultAssortmentAndStocks() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true,
                FeedContentTypeDTO.ASSORTMENT_WITH_PRICES, FeedContentTypeDTO.STOCK);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueStockAndDefault.response.json");
    }

    @Test
    @DisplayName("Получить синий ценовой фид с флагом with_default (дефолтный фид не ценовой - не возвращается)")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefaultWithUtility.before.csv")
    void testBlueOnlyPricesWithDefaultFlag() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true, FeedContentTypeDTO.PRICE);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBluePrice.response.json");
    }

    @Test
    @DisplayName("Получить синий стоковый фид с флагом with_default (дефолтный фид не стоковый - не возвращается)")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testBlueDefaultWithUtility.before.csv")
    void testBlueOnlyStocksWithDefaultFlag() throws URISyntaxException {
        String url = buildCampaignListUrl(1001L, null, true, FeedContentTypeDTO.STOCK);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testBlueStock.response.json");
    }

    @Test
    @DisplayName("Получить фиды (включая дефолтные) supplier и shop с корректными значениями в indexerFeedIds")
    @DbUnitDataSet(before = "UnitedFeedController/csv/getLatestFeedInfo/testIndexerFeedIds.before.csv")
    void testIndexerFeedIds() throws URISyntaxException {
        String url = buildBusinessListUrl(3001L, null, true);
        ResponseEntity<String> response = FunctionalTestHelper.get(url);
        assertResponse(response, "UnitedFeedController/json/getLatestFeedInfo/testIndexerFeedIds.response.json");
    }

    @SuppressWarnings("SameParameterValue")
    private String buildCampaignListUrl(long campaignId,
                                        String resourceType,
                                        FeedContentTypeDTO... feedContentTypes) throws URISyntaxException {
        return buildUrl("%s/v3/%d/feed/list", campaignId, resourceType, false, feedContentTypes);
    }

    @SuppressWarnings("SameParameterValue")
    private String buildBusinessListUrl(long businessId,
                                        String resourceType,
                                        FeedContentTypeDTO... feedContentTypes) throws URISyntaxException {
        return buildUrl("%s/v3/business/%d/feed/list", businessId, resourceType, false, feedContentTypes);
    }

    private String buildCampaignListUrl(long campaignId,
                                        String resourceType,
                                        boolean withDefault,
                                        FeedContentTypeDTO... feedContentTypes) throws URISyntaxException {
        return buildUrl("%s/v3/%d/feed/list", campaignId, resourceType, withDefault, feedContentTypes);
    }

    private String buildBusinessListUrl(long businessId,
                                        String resourceType,
                                        boolean withDefault,
                                        FeedContentTypeDTO... feedContentTypes) throws URISyntaxException {
        return buildUrl("%s/v3/business/%d/feed/list", businessId, resourceType, withDefault, feedContentTypes);
    }

    private String buildUrl(String templateUrl,
                            long id,
                            String resourceType,
                            boolean withDefault,
                            FeedContentTypeDTO... feedContentTypes) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(String.format(templateUrl, baseUrl, id));
        if (resourceType != null) {
            builder.addParameter("resource_type", resourceType);
        }

        for (FeedContentTypeDTO type : feedContentTypes) {
            builder.addParameter("feed_type", type.name());
        }

        if(withDefault) {
            builder.addParameter("with_default", Boolean.toString(withDefault));
        }

        return builder.build().toString();
    }

    private void assertResponse(@Nonnull ResponseEntity<String> response, @Nonnull String expectedFile) {
        JsonTestUtil.assertEquals(expectedFile, getClass(), response);
    }
}
