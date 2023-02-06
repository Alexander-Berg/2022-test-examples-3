package ru.yandex.autotests.market.bidding.tests.getcategorybids;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_ONLY_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 17.09.2014
 */
@Aqua.Test(title = "Запрос GET /category-bids")
@Features("GET /category-bids")
@Stories("Сравнивание с БД")
@Issue("AUTOTESTMARKET-41")
public class GetCategoryBidsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_ONLY_WITH_OFFER_IDS);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    @Test
    @Title("Запрос GET /category-bids должен возвращать данные из БД")
    public void categoryBidsRequestShouldReturnDataFromDatabase() {
        bidding.backend.bids(shopId).category().shouldReturnBidsFrom(bidding.database);
    }
}
