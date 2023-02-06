package ru.yandex.autotests.market.partner.api.campaigns.pull;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdRule;
import ru.yandex.autotests.market.checkouter.api.steps.CheckoutSteps;
import ru.yandex.autotests.market.partner.api.data.b2b.BaseRequestData;
import ru.yandex.autotests.market.partner.api.steps.pullapi.CampaignsOrderSteps;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.PullApiRequestsData.getCampaignOrdersRequestsFromWiki;


/**
 * Created
 * by strangelet
 * on 22.06.15.
 */
@Feature("pull-api")
@Aqua.Test(title = "Тесты на ручку /campaigns/(campaignId)/orders")
@RunWith(Parameterized.class)
@Stories("GET /campaigns/(campaignId)/orders")
@Ignore("Тесты устарели. Будут переделаны в рамках https://st.yandex-team.ru/MBI-38265")
public class CampaignsOrdersTest {

    private static PartnerApiRequestData request;
    private static CampaignsOrderSteps pullApiSteps = new CampaignsOrderSteps();
    private static final CheckoutSteps checkouter = new CheckoutSteps();
    private static ShopIdRule shopIdRule = new ShopIdRule(ShopTags.PULL_API);

    @ClassRule
    public static RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule(shopIdRule);

    @BeforeClass
    public static void getTestShopId() {
        long shopId = shopIdRule.getTestShop();
        checkouter.createReservedOrder(shopId); //(RESERVED);
        checkouter.createOrderForShop(shopId);  //(PROCESSING);
    }


    @Parameterized.Parameters(name = "Case: {1}")
    public static Collection<Object[]> data() {
        return BaseRequestData.asRequestWithCase(
                getCampaignOrdersRequestsFromWiki()
        );
    }

    public CampaignsOrdersTest(PartnerApiRequestData request, String caseName) {
        this.request = request;
    }


    @Test
    public void checkResponseJSON() {
        pullApiSteps.checkCampaignOrdersResponseJSON(request);
    }

    @Test
    public void checkResponseXML() {
        pullApiSteps.checkCampaignOrdersResponseXML(request);
    }
}