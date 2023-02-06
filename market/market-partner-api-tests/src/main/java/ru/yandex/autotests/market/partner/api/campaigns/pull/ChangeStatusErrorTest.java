package ru.yandex.autotests.market.partner.api.campaigns.pull;

import com.google.gson.Gson;
import org.junit.Before;
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
import ru.yandex.autotests.market.partner.api.steps.pullapi.CampaignsOrderIdSteps;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Order;
import ru.yandex.autotests.market.partner.beans.api.pullapi.OrderResponse;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Status;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Substatus;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.market.partner.api.data.PullApiRequestsData.getChangeStatusRequest;


/**
 * Created
 * by strangelet
 * on 25.06.15.
 */

@Feature("pull-api")
@Aqua.Test(title = "Тесты на  невалидное изменение статуса заказа")
@RunWith(Parameterized.class)
@Stories("PUT /campaigns/(campaignId)/orders/(orderId)/status")
@Ignore("Тесты устарели. Будут переделаны в рамках https://st.yandex-team.ru/MBI-38265")
public class ChangeStatusErrorTest {

    @Rule
    public final RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule(shopIdRule);

    private static ShopIdRule shopIdRule = new ShopIdRule(ShopTags.PULL_API);

    private Long orderId;

    private static CampaignsOrderIdSteps orderIdSteps = new CampaignsOrderIdSteps();
    private Order statusRequestBody;
    private Status fromStatus;
    private Status toStatus;
    private Substatus toSubstatus;

    @Parameterized.Parameters(name = "Change status {0} to {1} and substatus to {2}")
    public static Collection<Object[]> data() {

        List<Object[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new Object[]{Status.DELIVERY, Status.DELIVERY, null});
        listOfArrays.add(new Object[]{Status.CANCELLED, Status.CANCELLED, Substatus.SHOP_FAILED});
        listOfArrays.add(new Object[]{Status.CANCELLED, Status.CANCELLED, Substatus.USER_CHANGED_MIND});
        listOfArrays.add(new Object[]{Status.PROCESSING, Status.PROCESSING, null});
        listOfArrays.add(new Object[]{Status.PROCESSING, Status.PICKUP, null});
        listOfArrays.add(new Object[]{Status.PROCESSING, Status.RESERVED, null});
        listOfArrays.add(new Object[]{Status.PROCESSING, Status.DELIVERED, null});
        listOfArrays.add(new Object[]{Status.PICKUP, Status.PICKUP, null});
        listOfArrays.add(new Object[]{Status.PICKUP, Status.CANCELLED, Substatus.REPLACING_ORDER});
        return listOfArrays;
    }

    public ChangeStatusErrorTest(Status fromStatus, Status toStatus, Substatus toSubstatus) {

        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.toSubstatus = toSubstatus;
    }

    @Before
    public void prepareBody() {
        statusRequestBody = new Order().withStatus(toStatus).withSubstatus(toSubstatus);
        orderId = orderIdSteps.takeOrderIdWithStatus(fromStatus, shopIdRule.getTestShop());
    }


    @Test
    public void changeStatusJSON() {
        String requestBody = new Gson().toJson(new OrderResponse().withOrders(statusRequestBody));
        PartnerApiRequestData request = getChangeStatusRequest(orderId, requestBody);
        OrderResponse response = orderIdSteps.takeResponseJSON(request);
        orderIdSteps.checkBadRequestError(response);
    }

    @Test
    public void checkResponseXML() {
        PartnerApiRequestData request = getChangeStatusRequest(orderId, statusRequestBody);
        OrderResponse response = orderIdSteps.takeResponseXML(request);
        orderIdSteps.checkBadRequestError(response);
    }


}
