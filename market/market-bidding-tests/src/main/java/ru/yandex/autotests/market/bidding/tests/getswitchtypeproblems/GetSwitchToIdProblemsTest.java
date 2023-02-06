package ru.yandex.autotests.market.bidding.tests.getswitchtypeproblems;

import ch.lambdaj.function.convert.Converter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.autotests.market.bidding.client.main.beans.SwitchTypeProblemsResponseBody;
import ru.yandex.autotests.market.bidding.rules.BiddingApiTestsRule;
import ru.yandex.autotests.market.bidding.wiki.SwitchTypeProblemsTestDataFromWiki;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Parameter;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ch.lambdaj.Lambda.convert;
import static ru.yandex.autotests.market.bidding.wiki.ShopIdDataFromWiki.getShopIdWikiPageUrl;
import static ru.yandex.autotests.market.bidding.wiki.SwitchTypeProblemsTestDataFromWiki.getSwitchTypeProblemsWikiPageUrl;

/**
 * User: alkedr
 * Date: 01.10.2014
 */
@Aqua.Test(title = "Запрос GET /switch/{type}/problems")
@Features("GET /switch/{type}/problems")
@Stories("Переключение с title на id")
@Issue("AUTOTESTMARKET-54")
@RunWith(Parameterized.class)
public class GetSwitchToIdProblemsTest {
    @Parameter private final String shopIdWikiPage = getShopIdWikiPageUrl();
    @Parameter private final String wikiPage = getSwitchTypeProblemsWikiPageUrl();
    @Parameter private final long shopId;

    @Rule
    public final BiddingApiTestsRule bidding = new BiddingApiTestsRule();

    private final SwitchTypeProblemsResponseBody expectedProblems;

    public GetSwitchToIdProblemsTest(String testCaseName, long shopId, SwitchTypeProblemsResponseBody expectedProblems) {
        this.shopId = shopId;
        this.expectedProblems = expectedProblems;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> parameters() {
        return convert(SwitchTypeProblemsTestDataFromWiki.DATA, new Converter<SwitchTypeProblemsTestDataFromWiki, Object[]>() {
            @Override
            public Object[] convert(SwitchTypeProblemsTestDataFromWiki from) {
                SwitchTypeProblemsResponseBody problems = new SwitchTypeProblemsResponseBody();
                problems.all = (long)from.getAll();
                problems.ok = (long)from.getOk();
                problems.noOfferId = (long)from.getNoOfferId();
                problems.notPublished = (long)from.getNotPublished();
                problems.notFound = (long)from.getNotFound();
                problems.invalidBidValue = (long)from.getInvalidBidValue();
                return new Object[]{from.getTestCaseName(), from.getShopId(), problems};
            }
        });
    }

    @Test
    public void getSwitchFromTitleToIdProblemsShouldReturnCorrectProblems() {
        bidding.backend.switchTypeProblems(shopId).switchToTitleProblemsShouldBe(expectedProblems);
    }
}
