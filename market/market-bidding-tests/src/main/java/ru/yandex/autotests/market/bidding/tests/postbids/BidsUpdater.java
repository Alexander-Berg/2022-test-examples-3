package ru.yandex.autotests.market.bidding.tests.postbids;

import org.junit.Rule;
import org.junit.Test;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.beanfactories.PostBidsRequestBodyFactory;
import ru.yandex.autotests.market.bidding.client.main.beans.PostBidsRequestBody;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Stories;
import ru.yandex.qatools.allure.annotations.Title;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_IDS;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * Обновлялка ставок в тестинге. Нужна для проверок быстроставок в индексаторе.
 */
@Aqua.Test(title = "Обновление ставок для индексатора")
@Issue("MBI-31146")
public class BidsUpdater {
    private static final long EXECUTION_TIME = TimeUnit.MINUTES.toMillis(30);
    private static final long BIDS_UPDATE_INTERVAL = TimeUnit.MINUTES.toMillis(3);

    @Rule public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final long shopId = getShopId(READ_WRITE_WITH_OFFER_IDS);


    @Test
    @Title("Периодическое обновление ставок по offer_id")
    public void changeBids() {
        long startTime = System.currentTimeMillis();
        while (true) {
            long timePassed = System.currentTimeMillis() - startTime;
            if (timePassed >= EXECUTION_TIME) {
                break;
            }
            try {
                PostBidsRequestBody postBidsBody = buildPostOfferIdBidsRequestBody();
                bidding.backend.bids(shopId).post(postBidsBody);
                Thread.sleep(BIDS_UPDATE_INTERVAL);
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }

    private PostBidsRequestBody buildPostOfferIdBidsRequestBody() {
        PostBidsRequestBody result = new PostBidsRequestBody();
        result.isApi = false;
        result.changeByFeedList = asList(
                PostBidsRequestBodyFactory.randomChangeByFeed(383889, "1364110"),
                PostBidsRequestBodyFactory.randomChangeByFeed(383889, "1392943")
        );
        return result;
    }
}
