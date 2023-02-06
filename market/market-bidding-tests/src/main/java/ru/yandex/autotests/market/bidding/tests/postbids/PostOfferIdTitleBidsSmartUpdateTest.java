package ru.yandex.autotests.market.bidding.tests.postbids;

import com.google.common.collect.Iterables;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.client.main.beans.PostBidsRequestBody;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.qatools.allure.annotations.*;

import java.util.*;

import static java.lang.String.format;
import static ru.yandex.autotests.market.bidding.beanfactories.PostBidsRequestBodyFactory.randomChangeByFeed;
import static ru.yandex.autotests.market.bidding.beanfactories.PostBidsRequestBodyFactory.randomChangeByTitle;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_WRITE_WITH_OFFER_TITLES;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 09.02.2015
 */
@Aqua.Test(title = "Запрос POST /market/bidding/{shopId}/bids")
@Features("POST /market/bidding/{shopId}/bids")
@Stories("Установка ставок для офферов по id")
@Issues({@Issue("AUTOTESTMARKET-56"), @Issue("MBI-12653"), @Issue("AUTOTESTMARKET-3653")})
@RunWith(Parameterized.class)
public class PostOfferIdTitleBidsSmartUpdateTest {
    private static final long FEED_ID_OF_OLD_BIDS = 1;
    private static final long FEED_ID_OF_NEW_BIDS = 2;
    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();
    @Parameter
    private final String shopIdWikiPage = getShopIdWikiPageUrl();
    private final PostBidsRequestBody oldTitleBidsRequestBody;
    private final PostBidsRequestBody newTitleBidsRequestBody;
    @Parameter
    private long shopId;

    public PostOfferIdTitleBidsSmartUpdateTest(String testName, PostBidsRequestBody oldIdBidsRequestBody, PostBidsRequestBody newIdBidsRequestBody,
                                               PostBidsRequestBody oldTitleBidsRequestBody, PostBidsRequestBody newTitleBidsRequestBody) {
        this.oldTitleBidsRequestBody = oldTitleBidsRequestBody;
        this.newTitleBidsRequestBody = newTitleBidsRequestBody;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        Collection<Object[]> result = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            for (int oldBidsCount = 100; oldBidsCount <= 300; oldBidsCount += 1000) {
                for (int newBidsCount = oldBidsCount - 50; newBidsCount <= oldBidsCount + 50; newBidsCount += 500) {
                    Collections.addAll(result, new Object[][]{
                            {
                                    format("%s. сначала %d ставок, в запросе на изменение %d, только новые ставки", i, oldBidsCount, newBidsCount),
                                    generatePostOfferIdBidsRequest(oldBidsCount, 0),
                                    generatePostOfferIdBidsRequest(0, newBidsCount),
                                    generatePostOfferTitleBidsRequest(oldBidsCount, 0),
                                    generatePostOfferTitleBidsRequest(0, newBidsCount),
                            }
                    });
                }
            }
        }
        return result;
    }

    private static PostBidsRequestBody generatePostOfferIdBidsRequest(int oldBidsCount, int newBidsCount) {
        PostBidsRequestBody result = new PostBidsRequestBody();
        result.changeByFeedList = new ArrayList<>();
        for (int i = 0; i < oldBidsCount; i++) {
            result.changeByFeedList.add(randomChangeByFeed(FEED_ID_OF_OLD_BIDS, String.valueOf(i + 1)));
        }
        for (int i = 0; i < newBidsCount; i++) {
            result.changeByFeedList.add(randomChangeByFeed(FEED_ID_OF_NEW_BIDS, String.valueOf(i + 1)));
        }
        Collections.shuffle(result.changeByFeedList);
        return result;
    }

    private static PostBidsRequestBody generatePostOfferTitleBidsRequest(int oldBidsCount, int newBidsCount) {
        PostBidsRequestBody result = new PostBidsRequestBody();
        result.changeByTitleList = new ArrayList<>();
        for (int i = 0; i < oldBidsCount; i++) {
            result.changeByTitleList.add(randomChangeByTitle(String.valueOf(FEED_ID_OF_OLD_BIDS) + (i + 1)));
        }
        for (int i = 0; i < newBidsCount; i++) {
            result.changeByTitleList.add(randomChangeByTitle(String.valueOf(FEED_ID_OF_NEW_BIDS) + (i + 1)));
        }
        Collections.shuffle(result.changeByTitleList);
        return result;
    }

    private static PostBidsRequestBody mergeOldAndNewOfferTitleBidRequests(PostBidsRequestBody oldBidsRequestBody, PostBidsRequestBody newBidsRequestBody) {
        PostBidsRequestBody result = new PostBidsRequestBody();
        result.changeByTitleList = new ArrayList<>();
        for (PostBidsRequestBody.ChangeByTitle changeByTitle : Iterables.concat(oldBidsRequestBody.changeByTitleList, newBidsRequestBody.changeByTitleList)) {
            Optional<PostBidsRequestBody.ChangeByTitle> existing = result.changeByTitleList.stream()
                    .filter(change -> Objects.equals(change.id, changeByTitle.id))
                    .findAny();
            if (existing.isPresent()) {
                existing.get().values = changeByTitle.values;
            } else {
                PostBidsRequestBody.ChangeByTitle copy = new PostBidsRequestBody.ChangeByTitle();
                copy.id = changeByTitle.id;
                copy.values = changeByTitle.values;
                copy.ext = changeByTitle.ext;
                result.changeByTitleList.add(copy);
            }
        }
        return result;
    }

    @Test
    public void smartUpdateOfferTitleBids() {
        shopId = getShopId(READ_WRITE_WITH_OFFER_TITLES);
        PostBidsRequestBody mergedBidsRequestBody = mergeOldAndNewOfferTitleBidRequests(oldTitleBidsRequestBody, newTitleBidsRequestBody);
        bidding.backend.bids(shopId).clearOfferTitleBids(bidding.database);
        bidding.backend.bids(shopId).post(oldTitleBidsRequestBody);
        bidding.backend.bids(shopId).offer().shouldReturnTitleBids(bidding.database, oldTitleBidsRequestBody);
        bidding.backend.bids(shopId).post(newTitleBidsRequestBody);
        bidding.backend.bids(shopId).offer().shouldReturnTitleBids(bidding.database, mergedBidsRequestBody);
    }
}
