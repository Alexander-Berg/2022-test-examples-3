package ru.yandex.chemodan.app.psbilling.worker;

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.users.AbstractPaymentTest;
import ru.yandex.chemodan.app.psbilling.core.billing.users.CheckUserServiceTask;
import ru.yandex.chemodan.app.psbilling.core.billing.users.processors.PromocodeOrderProcessor;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriod;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.Order;
import ru.yandex.chemodan.app.psbilling.core.mocks.FeaturesSynchronizeRestTemplateMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductFeature;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.SynchronizationStatus;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.synchronization.feature.UserFeatureActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationTask;
import ru.yandex.chemodan.app.psbilling.core.users.UserService;
import ru.yandex.chemodan.app.psbilling.core.util.RequestTemplate;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.worker.actualization.userservice.UserServicesSyncStateActualizeCroneTask;
import ru.yandex.chemodan.app.psbilling.worker.billing.users.CheckUserServiceCrone;
import ru.yandex.chemodan.app.psbilling.worker.config.ActualizationWorkerConfiguration;
import ru.yandex.chemodan.app.psbilling.worker.config.UserBillingWorkerConfiguration;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;
import ru.yandex.misc.test.Assert;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UserBillingWorkerConfiguration.class, ActualizationWorkerConfiguration.class})
public class FreeServiceActualizationTest extends AbstractPaymentTest {
    @Autowired
    CheckUserServiceCrone checkUserServiceCrone;

    @Autowired
    UserServicesSyncStateActualizeCroneTask userServicesSyncStateActualizeCroneTask;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    PromocodeOrderProcessor promocodeOrderProcessor;

    @Autowired
    FeaturesSynchronizeRestTemplateMockConfiguration fsrstmc;

    @Autowired
    UserProductPricesDao userProductPricesDao;

    @Autowired
    JdbcTemplate3 jdbcTemplate;

    @Autowired
    FeatureDao featureDao;

    protected Option<RequestTemplate> postUrlEncoded(String template) {
        return Option.of(new RequestTemplate(HttpMethod.POST, template, Option.empty(),
                Option.of(MediaType.APPLICATION_FORM_URLENCODED)));
    }

    private UUID createFreeProduct(CustomPeriod cp) {
        jdbcTemplate.execute("insert into public.product_owners (id, code, created_at, default_group_product_id) " +
                "values ('4911da84-d505-4880-a0ef-20eb09f2f946', 'yandex_mail', '2020-04-09 13:21:33.631635', " +
                "'9afdd1f5-c313-4d4e-86f9-03ccaac30199')");
        jdbcTemplate.execute("select create_mpfs_space_feature(\n" +
                "    'some_free_feature_not_in_catalog',\n" +
                "    'не важно',\n" +
                "    'partner',\n" +
                "    'yandex_b2c_mail_pro_promo',\n" +
                "    true,\n" +
                "    true\n" +
                "    )");
        String priceId = jdbcTemplate.queryForObject("select create_free_space_product(\n" +
                        "    'mail_pro_b2c_test_promocode_131_GB',\n" +
                        "    (select tanker_key_id('disk-ps-billing', 'promos', 'mail_pro_b2c_test_promocode_131_GB')" +
                        "),\n" +
                        "    ?,\n" +
                        "    ?,\n" +
                        "    147102629888, --137 GB\n" +
                        "    'some_free_feature_not_in_catalog'\n" +
                        "    )", String.class,
                cp.getUnit().value(),
                cp.getValue()
        );
        return UUID.fromString(priceId);
    }

    private void mockRestActivations(UUID productPriceId) {
        UserProductFeature upf = userProductManager.findPrice(productPriceId)
                .getPeriod().getUserProduct().getFeatures().get(0);
        FeatureEntity fe = featureDao.findById(upf.getFeatureId().get());

        String activationUrl = getUrlStartsWith(fe.getActivationRequestTemplate().get());
        String setAmountUrl = getUrlStartsWith(fe.getSetAmountRequestTemplate().get());
        String deactivationUrl = getUrlStartsWith(fe.getDeactivationRequestTemplate().get());

        fsrstmc.mockGetOk(
                Mockito.startsWith(activationUrl),
                "{\"wrapper\": { \"id\": 1234567 } }"
        );
        fsrstmc.mockGetOk(Mockito.startsWith(setAmountUrl), "");
        fsrstmc.mockGetOk(Mockito.startsWith(deactivationUrl), "");
    }

    private String getUrlStartsWith(RequestTemplate rt) {
        return rt.getUrlTemplate().substring(0, rt.getUrlTemplate().indexOf("?"));
    }

    @Test
    public void testCreateFreeOrder() {

        CustomPeriodUnit toTickPeriod = CustomPeriodUnit.TEN_MINUTES;
        CustomPeriod toTick = new CustomPeriod(toTickPeriod, 1);
        UUID productPriceId = createFreeProduct(toTick);
        mockRestActivations(productPriceId);

        String periodCode = userProductManager.findPrice(productPriceId).getPeriod().getCode();

        DateTime initial = DateTime.now();
        DateUtils.freezeTime(initial);
        UUID orderId = billingService.createFreeOrder(uid, periodCode);
        Order o = orderDao.findById(orderId);
        UUID userServiceId = o.getUserServiceId().orElseThrow();

        assertAdditionalProcessDoesntHurt(orderId);

        //UserServiceActialization task fires in 10 seconds
        DateUtils.freezeTime(initial.plus(Duration.standardSeconds(11)));
        pushTasksActualization();
        //assert that service was enabled
        assertStatuses(userServiceId, Target.ENABLED);
        assertAdditionalProcessDoesntHurt(orderId);

        DateUtils.freezeTime(initial.plus(toTick.toJodaPeriod()).minus(Duration.standardSeconds(1)));
        pushTasksActualization();
        //assert that service will be not be disabled if checked before 1 tick of the period
        assertStatuses(userServiceId, Target.ENABLED);
        assertAdditionalProcessDoesntHurt(orderId);

        DateUtils.freezeTime(initial.plus(toTick.toJodaPeriod()).plus(Duration.standardSeconds(1)));
        pushTasksActualization();
        //assert that service will be disabled after 1 tick of the period
        assertStatuses(userServiceId, Target.DISABLED);
        assertAdditionalProcessDoesntHurt(orderId);

        //assert that trust client is never called
        Mockito.verifyNoMoreInteractions(trustClient);
    }

    private void assertAdditionalProcessDoesntHurt(UUID orderId) {
        Order oBefore = orderDao.findById(orderId);
        UUID userServiceIdBefore = oBefore.getUserServiceId().orElseThrow();
        UserService usBefore = userServiceManager.findById(userServiceIdBefore);
        SynchronizationStatus ssBefore = usBefore.getStatus();
        Target targetBefore = usBefore.getTarget();

        //test that 1 extra tick to process doesn't hurt
        promocodeOrderProcessor.processOrder(oBefore);

        Order o = orderDao.findById(orderId);
        UUID userServiceId = o.getUserServiceId().orElseThrow();
        UserService us = userServiceManager.findById(userServiceIdBefore);
        SynchronizationStatus ss = us.getStatus();
        Target target = us.getTarget();

        Assert.equals(userServiceIdBefore, userServiceId);
        Assert.equals(ssBefore, ss);
        Assert.equals(targetBefore, target);
    }

    private void pushTasksActualization() {
        transactionTemplate.execute(status -> {
            checkUserServiceCrone.execute(null);
            return null;
        });
        transactionTemplate.execute(status -> {
            bazingaTaskManagerStub.executeTasks(CheckUserServiceTask.class);
            return null;
        });
        transactionTemplate.execute(status -> {
            bazingaTaskManagerStub.executeTasks(UserServiceActualizationTask.class);
            return null;
        });
        transactionTemplate.execute(status -> {
            bazingaTaskManagerStub.executeTasks(UserFeatureActualizationTask.class);
            return null;
        });
        transactionTemplate.execute(status -> {
            userServicesSyncStateActualizeCroneTask.execute(null);
            return null;
        });
    }

    private void assertStatuses(UUID userServiceId, Target target) {
        transactionTemplate.execute(status -> {
            UserService us = userServiceManager.findById(userServiceId);
            Assert.equals(target, us.getTarget());
            Assert.equals(SynchronizationStatus.ACTUAL, us.getStatus());
            return null;
        });
    }


}
