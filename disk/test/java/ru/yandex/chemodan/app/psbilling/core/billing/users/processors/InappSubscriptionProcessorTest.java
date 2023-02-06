package ru.yandex.chemodan.app.psbilling.core.billing.users.processors;

import lombok.SneakyThrows;
import org.joda.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPeriodDao;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPeriodEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.entities.users.OrderType;
import ru.yandex.chemodan.app.psbilling.core.mocks.PurchaseReportingServiceMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.trust.client.responses.InappSubscription;
import ru.yandex.chemodan.trust.client.responses.InappSubscriptionState;
import ru.yandex.inside.passport.PassportUid;

@RunWith(SpringJUnit4ClassRunner.class)
public class InappSubscriptionProcessorTest extends AbstractPsBillingCoreTest {

    TaskScheduler taskSchedulerMock;
    @Autowired
    InappSubscriptionProcessor inappSubscriptionProcessor;

    @Autowired
    PsBillingProductsFactory productsFactory;

    @Autowired
    UserProductPeriodDao userProductPeriodDao;

    private UserProductPeriodEntity productPeriod;
    private final PassportUid uid = new PassportUid(1L);


    @Before
    @SneakyThrows
    public void setup() {
        taskSchedulerMock = psBillingCoreMocksConfig.getMock(TaskScheduler.class);
        productPeriod = userProductPeriodDao.findById(psBillingProductsFactory.createUserProductPrice()
                .getUserProductPeriodId());

    }


    @Test
    public void sendMailWhenActiveBecameHold() {
        sendMailTestSetup(true, InappSubscriptionState.ON_HOLD);
        Mockito.verify(taskSchedulerMock, Mockito.times(1)).scheduleOnHoldEmailTask(Mockito.any());
    }

    @Test
    public void sendMailWhenAppearsHold() {
        sendMailTestSetup(false, InappSubscriptionState.ON_HOLD);
        Mockito.verify(taskSchedulerMock, Mockito.times(1)).scheduleOnHoldEmailTask(Mockito.any());
    }

    @Test
    public void sendMailWhenActiveBecameGrace() {
        sendMailTestSetup(true, InappSubscriptionState.IN_GRACE);
        Mockito.verify(taskSchedulerMock, Mockito.times(1))
                .scheduleInGraceEmailTask(Mockito.any());

    }

    @Test
    public void sendMailWhenAppearsGrace() {
        sendMailTestSetup(false, InappSubscriptionState.IN_GRACE);
        Mockito.verify(taskSchedulerMock, Mockito.times(1))
                .scheduleInGraceEmailTask(Mockito.any());
    }

    @Test
    public void sendMailWhenActiveBecameFinished() {
        sendMailTestSetup(true, InappSubscriptionState.FINISHED);
        Mockito.verify(taskSchedulerMock, Mockito.times(1))
                .scheduleSubscriptionFinishedEmailTask(Mockito.eq(uid), Mockito.any(), Mockito.eq(OrderType.INAPP_SUBSCRIPTION), Mockito.any());
    }

    @Test
    public void sendMailWhenAppearsFinished() {
        sendMailTestSetup(false, InappSubscriptionState.FINISHED);
        Mockito.verify(taskSchedulerMock, Mockito.never()) //мы сервис никогда не видели, а он уже протух. Соответственно посылать ничего не над
                .scheduleSubscriptionFinishedEmailTask(Mockito.eq(uid), Mockito.any(), Mockito.eq(OrderType.INAPP_SUBSCRIPTION), Mockito.any());
    }

    private void sendMailTestSetup(boolean activate, InappSubscriptionState state) {
        Order order = null;
        if (activate) {
            order = processSubscription(InappSubscriptionState.ACTIVE, null);
            /* sample from actual logs
            action: buy_new, status: success, order_id: 318757, uid: 310580935,
            product_code: mail_pro_b2c_standard100_inapp_apple_for_disk, period: 1M, price: 69, currency: RUB,
            package_name: ru.yandex.disk, new_expiration_date: 2022-07-10T20:31:08.000Z
         */
            PurchaseReportingServiceMockConfiguration.assertLogLike(
                    "action: buy_new, status: success, order_id: trustId, uid: 1, " +
                            "product_code: %uuid%, period: 1M, price: 10, currency: RUB, " +
                            "new_expiration_date: %date%, package_name: test"
            );
        }
        //call twice to check extra schedule invocation
        order = processSubscription(state, order);
        processSubscription(state, order);
    }

    private Order processSubscription(InappSubscriptionState state, Order previousOrder) {
        return inappSubscriptionProcessor.processInappTrustSubscription(buildInappSubscription(state), uid, "test",
                Option.ofNullable(previousOrder));
    }

    private InappSubscription buildInappSubscription(InappSubscriptionState state) {
        int offset = state == InappSubscriptionState.FINISHED ? -100 : 100500;
        return InappSubscription.builder()
                .uid(uid.toString())
                .subscriptionId("trustId")
                .productId(productPeriod.getCode())
                .subscriptionUntil(Instant.now().plus(offset))
                .state(state)
                .build();
    }
}
