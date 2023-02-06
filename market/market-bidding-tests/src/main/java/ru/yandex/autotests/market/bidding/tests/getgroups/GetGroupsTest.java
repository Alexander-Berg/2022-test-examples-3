package ru.yandex.autotests.market.bidding.tests.getgroups;

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
 * Date: 22.09.2014
 */
@Aqua.Test(title = "Запрос GET /groups")
@Features("GET /groups")
@Stories("Сравнивание с БД")
@Issue("AUTOTESTMARKET-43")
public class GetGroupsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_ONLY_WITH_OFFER_IDS);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    @Test
    @Title("Запрос GET /groups должен возвращать данные из БД")
    public void getGroupsRequestShouldReturnDataFromDatabase() {
        bidding.backend.groups(shopId).getGroupsShouldReturnGroupsFrom(bidding.database);
    }
}
