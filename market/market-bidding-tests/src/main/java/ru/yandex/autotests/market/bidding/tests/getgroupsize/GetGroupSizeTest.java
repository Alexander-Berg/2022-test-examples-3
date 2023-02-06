package ru.yandex.autotests.market.bidding.tests.getgroupsize;

import ch.lambdaj.function.convert.Converter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.autotests.market.bidding.storage.MarketBiddingStorageSteps;
import ru.yandex.autotests.market.bidding.storage.beans.AuctionBidGroupRow;
import ru.yandex.qatools.allure.annotations.*;

import java.util.Collection;
import java.util.List;

import static ch.lambdaj.Lambda.convert;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.ShopDescription.READ_ONLY_WITH_OFFER_TITLES;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopId;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;

/**
 * User: alkedr
 * Date: 23.09.2014
 */
@Aqua.Test(title = "Запрос GET /groups/{groupId}/size")
@Features("GET /groups/{groupId}/size")
@Stories("Сравнивание с БД")
@Issue("AUTOTESTMARKET-47")
@RunWith(Parameterized.class)
public class GetGroupSizeTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private static final long shopId = getShopId(READ_ONLY_WITH_OFFER_TITLES);
    @Parameter private final long groupId;

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    public GetGroupSizeTest(long groupId) {
        this.groupId = groupId;
    }

    @Parameterized.Parameters(name = "groupId = {0}")
    public static Collection<Object[]> parameters() {
        List<Object[]> result = convert(new MarketBiddingStorageSteps().getGroups(shopId), new Converter<AuctionBidGroupRow, Object[]>() {
            @Override
            public Object[] convert(AuctionBidGroupRow group) {
                return new Object[]{group.getId()};
            }
        });
        result.add(new Object[]{0});
        return result;
    }

    @Test
    @Title("Запрос GET /groups/{groupId}/size должен возвращать данные из БД")
    public void groupSizeRequestShouldReturnDataFromDatabase() {
        bidding.backend.group(shopId, groupId).getSizeShouldReturnDataFrom(bidding.database);
    }
}
