package ru.yandex.autotests.market.bidding.tests.deletegroup;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.client.main.beans.BidGroup;
import ru.yandex.autotests.market.bidding.client.main.beans.PostBidsRequestBody;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.*;

import static ru.yandex.autotests.market.bidding.beanfactories.BidGroupFactory.defaultBidGroup;
import static ru.yandex.autotests.market.bidding.beanfactories.PostBidsRequestBodyFactory.buildPostTwoOfferTitleBidsWithGroupRequestBody;
import static ru.yandex.autotests.market.bidding.steps.BiddingSteps.DEFAULT_GROUP;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_TITLES;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 29.09.2014
 */
@Aqua.Test(title = "Запрос DELETE /groups/{groupId}")
@Features("DELETE /groups/{groupId}")
@Stories("Перемещение ставок в группу по умолчанию")
@Issue("AUTOTESTMARKET-46")
public class DeleteGroupTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_TITLES);

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    private PostBidsRequestBody postBidsBody = null;

    @Before
    public void before() {
        bidding.backend.groups(shopId).deleteAll();
        BidGroup groupToDelete = bidding.backend.groups(shopId).create("SomeGroup");

        postBidsBody = buildPostTwoOfferTitleBidsWithGroupRequestBody(groupToDelete.id);
        bidding.backend.bids(shopId).post(postBidsBody);

        bidding.backend.group(shopId, groupToDelete.id).delete();

        for (PostBidsRequestBody.ChangeByTitle changeByTitle : postBidsBody.changeByTitleList) {
            changeByTitle.ext.group = DEFAULT_GROUP;
        }
    }

    @Test
    @Title("Удалённой группы не должно быть в выдаче запроса GET /groups")
    public void deletedGroupShouldNotBePresentInGetGroupsResult() {
        bidding.backend.groups(shopId).getGroupsShouldReturn(defaultBidGroup(shopId));
    }

    @Test
    @Title("Удалённой группы не должно быть в БД")
    public void deletedGroupShouldNotBePresentInDatabase() {
        bidding.database.groups(shopId).shouldNotContainGroups();
    }

    @Test
    @Title("Ставки из удалённой группы должны быть в группе по умолчанию")
    public void bidsFromDeletedGroupShouldBeInDefaultGroup() {
        bidding.backend.group(shopId, DEFAULT_GROUP).shouldReturnBidsFrom(bidding.database);
    }
}
