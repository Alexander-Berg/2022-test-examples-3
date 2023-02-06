package ru.yandex.autotests.market.bidding.tests.postbids;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.client.main.beans.PostBidsRequestBody;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.market.bidding.beanfactories.PostBidsRequestBodyFactory.buildPostCategoryBidsRequestBody;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 06.10.2014
 */
@Aqua.Test(title = "Запрос POST /market/bidding/{shopId}/bids")
@Features("POST /market/bidding/{shopId}/bids")
@Stories("Установка ставок для категорий")
@Issue("AUTOTESTMARKET-56")
public class PostCategoryBidsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_IDS);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    private final PostBidsRequestBody postBidsBody = buildPostCategoryBidsRequestBody();

    @Before
    public void before() {
        bidding.backend.bids(shopId).post(postBidsBody);
    }

    @Test
    @Title("Ставки должны обновиться в выдаче бекенда")
    public void bidsShouldChangeInBackend() {
        bidding.backend.bids(shopId).category().shouldReturnCategoryBids(postBidsBody);
    }

    @Test
    @Title("Ставки должны обновиться в БД")
    public void bidsShouldChangeInDatabase() {
        bidding.database.categoryBids(shopId).shouldContainCategoryBids(postBidsBody);
    }
}
