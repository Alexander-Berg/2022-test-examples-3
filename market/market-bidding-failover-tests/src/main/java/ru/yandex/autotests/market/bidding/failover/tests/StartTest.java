package ru.yandex.autotests.market.bidding.failover.tests;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingFailoverTestsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Title;

import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 13.10.2014
 *
 * https://wiki.yandex-team.ru/mbi/bids/failover#zapusk1
 */
@Aqua.Test(title = "Запуск")
@Features("Запуск")
@Issue("TESTMARKET-1570")
public class StartTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_IDS);

    @Rule
    public final BiddingFailoverTestsRule bidding = new BiddingFailoverTestsRule();

    @Test
    @Title("Ноды должны стартовать правильно")
    public void nodesShouldStartCorrectly() {
        bidding.leaderNodeShouldBehaveCorrectly(shopId);
        bidding.nonLeaderNodesShouldBehaveCorrectly();
    }
}
