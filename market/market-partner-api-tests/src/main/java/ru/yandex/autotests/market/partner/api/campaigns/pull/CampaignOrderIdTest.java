package ru.yandex.autotests.market.partner.api.campaigns.pull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdRule;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersStatusSteps;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.SubStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.partner.api.steps.pullapi.CampaignsOrderIdSteps;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;
import ru.yandex.qatools.allure.annotations.Issue;
import ru.yandex.qatools.allure.annotations.Issues;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.market.checkouter.beans.Status.CANCELLED;
import static ru.yandex.autotests.market.checkouter.beans.Status.PICKUP;
import static ru.yandex.autotests.market.checkouter.beans.Status.PROCESSING;
import static ru.yandex.autotests.market.checkouter.beans.Status.RESERVED;
import static ru.yandex.autotests.market.checkouter.beans.SubStatus.RESERVATION_EXPIRED;
import static ru.yandex.autotests.market.partner.api.data.PullApiRequestsData.getCampaignOrderIdRequest;

/**
 * Created
 * by strangelet
 * on 25.06.15.
 */
@Feature("pull-api")
@Aqua.Test(title = "Тесты на ручку /campaigns/(campaignId)/orders/(orderId)")
@RunWith(Parameterized.class)
@Stories("GET /campaigns/(campaignId)/orders/(orderId)")
@Issues({
        @Issue("https://st.yandex-team.ru/AUTOTESTMARKET-3758"),
})
@Ignore("Тесты устарели. Будут переделаны в рамках https://st.yandex-team.ru/MBI-38265")
public class CampaignOrderIdTest {

    private static ShopIdRule shopIdRule = new ShopIdRule(ShopTags.PULL_API);

    @ClassRule
    public static final RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule(shopIdRule);


    private static CampaignsOrderIdSteps pullApiSteps = new CampaignsOrderIdSteps();
    private static OrdersStatusSteps ordersStatusSteps = new OrdersStatusSteps();
    private static long SHOP_ID;
    private final Status status;
    private final SubStatus subStatus;
    private PartnerApiRequestData requestData;

    @BeforeClass
    public static void init() {
        SHOP_ID = shopIdRule.getTestShop();
    }


    @Parameterized.Parameters(name = "{index} Case: status={0} and substatus={1}")
    public static Collection<Object[]> data() {
        List<Object[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new Object[]{PICKUP, null});
        listOfArrays.add(new Object[]{PROCESSING, null});
        listOfArrays.add(new Object[]{RESERVED, null});
        listOfArrays.add(new Object[]{CANCELLED, RESERVATION_EXPIRED});
        return listOfArrays;
    }


    public CampaignOrderIdTest(Status status, SubStatus subStatus) {
        this.status = status;
        this.subStatus = subStatus;
    }

    @Before
    public void createRequest() {
        TestDataOrder checkouterOrder;
        if (subStatus == null) {
            checkouterOrder = ordersStatusSteps.getOrderWithStatus(status, SHOP_ID);
        } else {
            checkouterOrder = ordersStatusSteps
                    .changeStatus(CANCELLED, subStatus, ordersStatusSteps.getOrderWithStatus(PROCESSING, SHOP_ID));
        }
        requestData = getCampaignOrderIdRequest(checkouterOrder.getId());
    }

    @Test
    public void checkResponseJSON() {
        pullApiSteps.checkCampaignOrdersResponseJSON(requestData);
    }

    @Test
    public void checkResponseXML() {
        pullApiSteps.checkCampaignOrdersResponseXML(requestData);
    }
}
