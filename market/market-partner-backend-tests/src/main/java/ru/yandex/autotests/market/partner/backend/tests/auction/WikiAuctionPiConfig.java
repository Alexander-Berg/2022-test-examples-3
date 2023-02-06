package ru.yandex.autotests.market.partner.backend.tests.auction;

import ru.yandex.autotests.market.common.wiki.WikiGridLoadingUtils;
import ru.yandex.autotests.market.common.wiki.WikiGridRow;
import ru.yandex.qatools.allure.Allure;
import ru.yandex.qatools.allure.events.AddParameterEvent;

import static ru.yandex.autotests.market.common.wiki.WikiProperties.WIKI_BASE_URL;

/**
 * @author vbudnev
 */
public class WikiAuctionPiConfig implements AuctionPiConfig {
    private static final String WIKI_TEST_CLASS_DATA_MAPPING_URL
            = "/users/vbudnev/autionpiregresstestwikidata/testclassmapping/";

    private static final String WIKI_TABLE_COL_PAGE_URL = "dataUrl";
    private static final String WIKI_TABLE_COL_OFFERS_URL = "offersUrl";
    private static final String WIKI_TABLE_COL_USER_ID = "userId";
    private static final String WIKI_TABLE_COL_CAMPAIGN_ID = "campaignId";
    private static final String WIKI_TABLE_COL_CLASS_NAME = "className";
    private static final String WIKI_TABLE_COL_DISABLED = "disabled";

    private long managerUid;
    private long campaignId;

    public WikiAuctionPiConfig() {
        this("DEFAULT");
    }

    public WikiAuctionPiConfig(String className) {
        String testShopDatUrl = getClassTestDataPageByClassName(className);
        loadCampaignAndOffers(testShopDatUrl);
    }

    /**
     * Получаем url, для данных по имени теста.
     * <br>Если notfound, упадет с exception.
     */
    private String getClassTestDataPageByClassName(String className) {
        Allure.LIFECYCLE.fire(new AddParameterEvent("testCasesClassesMappingUrl", WIKI_BASE_URL + WIKI_TEST_CLASS_DATA_MAPPING_URL));

        return WikiGridLoadingUtils.loadWikiGrid(WIKI_TEST_CLASS_DATA_MAPPING_URL).stream()
                .filter(row -> !Boolean.parseBoolean(row.get(WIKI_TABLE_COL_DISABLED)))
                .filter(row -> className.equals(row.get(WIKI_TABLE_COL_CLASS_NAME)))
                .findFirst().get().get(WIKI_TABLE_COL_PAGE_URL);
    }

    /**
     * Получаем данные необходимые для формирования запоросов:
     * id кампании, авторизованного пользователя, офферы, посковый ключ для офферовб и тд.
     */
    private void loadCampaignAndOffers(String offersMetaWikiUrl) {
        Allure.LIFECYCLE.fire(new AddParameterEvent("testCasesGeneralQueryDataUrl", WIKI_BASE_URL + offersMetaWikiUrl));

        WikiGridRow ownerInfo = WikiGridLoadingUtils.loadWikiGrid(offersMetaWikiUrl).stream()
                .filter(row -> !Boolean.parseBoolean(row.get(WIKI_TABLE_COL_DISABLED)))
                .findFirst().get();

        managerUid = Long.parseLong(ownerInfo.get(WIKI_TABLE_COL_USER_ID));
        campaignId = Long.parseLong(ownerInfo.get(WIKI_TABLE_COL_CAMPAIGN_ID));
        loadOffers(ownerInfo.get(WIKI_TABLE_COL_OFFERS_URL));
    }

    /**
     * Загружаем офферы: гибридные, cpc_only, cpa_only.
     */
    private void loadOffers(String offersWikiTableUrl) {
        Allure.LIFECYCLE.fire(new AddParameterEvent("testCasesOffersUrl", WIKI_BASE_URL + offersWikiTableUrl));
    }


    @Override
    public long getUserId() {
        return managerUid;
    }

    @Override
    public long getCampaignId() {
        return campaignId;
    }

}
