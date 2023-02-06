package ru.yandex.autotests.market.bidding.tests.getofferbids;

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
 * Date: 15.09.2014
 */
@Aqua.Test(title = "Запрос POST /id-bids")
@Features("POST /id-bids")
@Stories("Сравнивание с БД")
@Issue("AUTOTESTMARKET-40")
public class GetOfferBidsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_ONLY_WITH_OFFER_IDS);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    @Test
    @Title("Запрос POST /id-bids должен возвращать данные из БД")
    public void idBidsRequestShouldReturnDataFromDatabase() {
        bidding.backend.bids(shopId).offer().shouldReturnOfferIdBidsFrom(bidding.database);
    }
}
