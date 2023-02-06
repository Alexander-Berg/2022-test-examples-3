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
import ru.yandex.autotests.market.partner.api.steps.pullapi.ChangeDeliverySteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Delivery;
import ru.yandex.autotests.market.partner.beans.api.pullapi.DeliveryRequestBody;
import ru.yandex.autotests.market.partner.beans.api.pullapi.DeliveryType;
import ru.yandex.autotests.market.partner.beans.api.pullapi.OrderResponse;
import ru.yandex.autotests.market.partner.beans.api.pullapi.Status;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;
import ru.yandex.qatools.allure.annotations.Stories;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static ru.yandex.autotests.market.partner.api.data.PullApiRequestsData.getChangeDeliveryRequest;

/**
 * Created
 * by strangelet
 * on 25.06.15.
 */

@Feature("pull-api")
@Aqua.Test(title = "Тесты  на изменение условий доставки")
@RunWith(Parameterized.class)
@Stories("PUT /campaigns/(campaignId)/orders/(orderId)/delivery")
@Ignore("Тесты устарели. Будут переделаны в рамках https://st.yandex-team.ru/MBI-38265")
public class ChangeDeliveryTest {

    @Rule
    public final RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule(shopIdRule);

    private static ShopIdRule shopIdRule = new ShopIdRule(ShopTags.PULL_API);

    private static CampaignsOrderIdSteps statusSteps = new CampaignsOrderIdSteps();
    private static ChangeDeliverySteps deliverySteps = new ChangeDeliverySteps();
    private Delivery delivery;
    private long orderId;


    @Parameterized.Parameters(name = "Change  delivery {1}")
    public static Collection<Object[]> data() {
        List<Object[]> listOfArrays = new ArrayList<>();
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangePrice(), "price"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeDates(), "dates"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeTimes(), "times"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeServiceName(), "serviceName"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeAddress(), "address"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeType(DeliveryType.POST), "type:POST"});
        listOfArrays.add(new Object[]{deliverySteps.bodyForChangeType(DeliveryType.PICKUP), "type:PICKUP"});
        return listOfArrays;
    }

    public ChangeDeliveryTest(Delivery delivery, String caseName) {

        this.delivery = delivery;
    }


    @Before
    public void createRequest() {
        orderId = statusSteps.takeOrderIdWithStatus(Status.PROCESSING, shopIdRule.getTestShop());
    }


    @Test
    public void changeDeliveryJSON() {
        DeliveryRequestBody body = new DeliveryRequestBody().withDelivery(delivery);
        String requestBody = new Gson().toJson(body);
        PartnerApiRequestData changeDeliveryRequest = getChangeDeliveryRequest(orderId,
                requestBody).withFormat(Format.JSON).withApplicationJsonContentType();
        OrderResponse orderResponse = deliverySteps.checkCampaignOrdersResponseJSON(changeDeliveryRequest);
        deliverySteps.compareWithRequestBody(delivery, orderResponse.getOrder().getDelivery());
    }

    @Test
    public void changeDeliveryXML() {
        PartnerApiRequestData changeDeliveryRequest = getChangeDeliveryRequest(orderId,
                delivery).withFormat(Format.XML).withApplicationXmlContentType();
        OrderResponse orderResponse = deliverySteps.checkCampaignOrdersResponseXML(changeDeliveryRequest);
        deliverySteps.compareWithRequestBody(delivery, orderResponse.getOrder().getDelivery());

    }


}
