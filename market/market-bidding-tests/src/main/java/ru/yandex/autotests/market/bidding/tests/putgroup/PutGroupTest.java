package ru.yandex.autotests.market.bidding.tests.putgroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.client.main.beans.BidGroup;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.market.bidding.beanfactories.BidGroupFactory.defaultBidGroup;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_TITLES;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 22.09.2014
 */
@Aqua.Test(title = "Запрос PUT /groups")
@Features("PUT /groups")
@Issue("AUTOTESTMARKET-44")
public class PutGroupTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_TITLES);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    private BidGroup group = null;

    @Before
    public void before() {
        bidding.backend.groups(shopId).deleteAll();
        group = bidding.backend.groups(shopId).create("SomeGroup");
    }

    @Test
    @Title("Добавленная группа должна быть в выдаче запроса GET /groups")
    public void addedGroupShouldBePresentInGetGroupsResult() {
        bidding.backend.groups(shopId).getGroupsShouldReturn(defaultBidGroup(shopId), group);
    }

    @Test
    @Title("Добавленная группа должна быть в БД")
    public void addedGroupShouldBePresentInDatabase() {
        bidding.database.groups(shopId).shouldContain(group);
    }
}
