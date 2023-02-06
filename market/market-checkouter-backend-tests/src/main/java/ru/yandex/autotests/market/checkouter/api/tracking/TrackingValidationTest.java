package ru.yandex.autotests.market.checkouter.api.tracking;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.market.checkouter.api.data.providers.tracker.DeliveryServiceIdProvider;
import ru.yandex.autotests.market.checkouter.api.data.requests.orders.delivery.DeliveryChangeRequests;
import ru.yandex.autotests.market.checkouter.api.data.requests.orders.delivery.DeliveryValidationRequests;
import ru.yandex.autotests.market.checkouter.api.rule.CheckouterRuleFactory;
import ru.yandex.autotests.market.checkouter.api.rule.ShopIdRule;
import ru.yandex.autotests.market.checkouter.api.steps.OrderDeliverySteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersStatusSteps;
import ru.yandex.autotests.market.checkouter.api.steps.OrdersSteps;
import ru.yandex.autotests.market.checkouter.api.steps.TrackingSteps;
import ru.yandex.autotests.market.checkouter.api.utils.TimeoutUtils;
import ru.yandex.autotests.market.checkouter.beans.Status;
import ru.yandex.autotests.market.checkouter.beans.TrackStatus;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataOrder;
import ru.yandex.autotests.market.checkouter.beans.testdata.TestDataTrack;
import ru.yandex.autotests.market.common.steps.AssertSteps;
import ru.yandex.autotests.market.pushapi.data.wiki.ShopTags;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.autotests.market.common.steps.AssertSteps.assertStep;

@RunWith(Parameterized.class)
public class TrackingValidationTest {
    @ClassRule
    public static final ShopIdRule shopIdRule = new ShopIdRule(ShopTags.CHECKOUTER_AUTOTEST_GLOBAL);
    @Rule
    public RuleChain chain = CheckouterRuleFactory.defaultCheckouterRule();

    private OrdersStatusSteps ordersStatusSteps = new OrdersStatusSteps();
    private OrderDeliverySteps orderDeliverySteps = new OrderDeliverySteps();
    private TrackingSteps trackingSteps = new TrackingSteps();
    private OrdersSteps ordersSteps = new OrdersSteps();

    private long orderId;

    @Parameterized.Parameter
    public long deliveryServiceId;
    @Parameterized.Parameter(1)
    public TrackStatus trackStatus;

    @Parameterized.Parameters(name = "Case deliveryServiceId={0} - {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[] { DeliveryServiceIdProvider.getSupportedDeliveryServiceId(), TrackStatus.STARTED },
                new Object[] { DeliveryServiceIdProvider.getUnsupportedDeliveryServiceId(), TrackStatus.NEW }
        );
    }

    @Before
    public void makeTestOrder() {
        TestDataOrder orderWithStatus = ordersStatusSteps.getOrderWithStatus(Status.PROCESSING, shopIdRule.getTestShop());
        orderId = orderWithStatus.getId();
    }

    @Test
    public void testSupportedDeliveryServiceId() {
        TestDataOrder orderAfterTrackAdded = orderDeliverySteps.changeDeliveryByRequest(
                new DeliveryChangeRequests(orderId).addTrack(
                        DeliveryValidationRequests.track().withDeliveryServiceId(deliveryServiceId).withTrackerId(null))).toEntityBean();

        trackingSteps.pushTracks();
        TimeoutUtils.waitFor(15, TimeUnit.SECONDS);

        TestDataOrder order = ordersSteps.getOrderById(orderId).toEntityBean();

        assertStep("Проверяем, что в ответе есть треки", order.getDelivery().getTracks(), notNullValue(), not(empty()));

        TestDataTrack testDataTrack = order.getDelivery().getTracks().get(0);
        TrackStatus status = testDataTrack.getStatus();
        assertStep("Провряем, что трек в нужном статусе", status, Matchers.is(trackStatus));
    }
}
