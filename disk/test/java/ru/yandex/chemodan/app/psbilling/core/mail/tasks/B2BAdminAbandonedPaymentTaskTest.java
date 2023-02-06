package ru.yandex.chemodan.app.psbilling.core.mail.tasks;

import java.math.BigDecimal;
import java.util.Currency;

import org.apache.http.client.methods.HttpPost;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.Money;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupTrustPaymentRequestDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentInitiationType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.PaymentRequestStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.sending.BaseEmailSendTest;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.mail.LocalizedEmailTaskExecutor;
import ru.yandex.chemodan.app.psbilling.core.tasks.policies.TaskPreExecutionPoliciesHolder;
import ru.yandex.chemodan.app.psbilling.core.tasks.policies.mail.RegularEmailTaskPreExecutionPolicy;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.commune.bazinga.scheduler.ExecutionContext;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;

import static org.mockito.Mockito.mock;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.FAN_MAIL_LIMIT_FEATURE_CODE;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;
import static ru.yandex.misc.test.Assert.assertContains;

public class B2BAdminAbandonedPaymentTaskTest extends BaseEmailSendTest {
    private static final PassportUid UID = new PassportUid(123);
    private static final long CLIENT_ID = 42L;
    private Group group;
    private UserProductEntity userProduct;

    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private GroupTrustPaymentRequestDao groupTrustPaymentRequestDao;
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private GroupProductDao groupProductDao;
    @Autowired
    private TaskPreExecutionPoliciesHolder taskPreExecutionPoliciesHolder;
    @Autowired
    private LocalizedEmailTaskExecutor localizedEmailTaskExecutor;
    @Autowired
    private EmailHelper emailHelper;

    @Before
    public void init() {
        userProduct = psBillingProductsFactory.createUserProduct();
        psBillingProductsFactory.createProductFeature(userProduct.getId(),
                FeatureEntity.builder().type(FeatureType.ADDITIVE).build(), x -> x.code(FAN_MAIL_LIMIT_FEATURE_CODE)
                        .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_1234").getId())));
        psBillingProductsFactory.createProductFeature(userProduct.getId(),
                FeatureEntity.builder().type(FeatureType.ADDITIVE).build(), x -> x.code(MPFS_SPACE_FEATURE_CODE)
                        .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_3_gb").getId())));
        featureFlags.getB2bAdminAbandonedPaymentEmailEnabled().setValue("true");
        group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(CLIENT_ID, UID)));

        String emailKey = "b2b_admin_abandoned_payment";
        ListF<String> args = Cf.list("tariff_name", "disk_space_product", "fan_mail_limit_product",
                "mail_archive_text_product", "uid");
        emailHelper.createTemplateWithRuLocalization(emailKey, args);
    }

    @Test
    public void successStory() {
        taskScheduler.scheduleB2bAdminAbandonedPaymentTask(UID, CLIENT_ID, userProduct.getId(), group.getId());
        bazingaTaskManagerStub.executeTasks(applicationContext);

        HttpPost request = mailSenderMockConfig.verifyEmailSent();
        assertContains(request.getURI().toString(), UID.toString());
    }

    @Test
    public void shouldDelayEmailIfThereIsInitPayment() {
        // create payment in status INIT
        groupTrustPaymentRequestDao.insert(GroupTrustPaymentRequestDao.InsertData.builder()
                .requestId("requestId")
                .operatorUid(UID.toString())
                .status(PaymentRequestStatus.INIT)
                .transactionId(Option.of("transactionId"))
                .paymentInitiationType(PaymentInitiationType.USER)
                .money(new Money(new BigDecimal(8), Currency.getInstance("RUB")))
                .clientId(CLIENT_ID)
                .build());

        Instant now = Instant.now();
        MailContext mailContext = MailContext.builder()
                .to(UID)
                .language(Option.of(Language.RUSSIAN))
                .userProductId(Option.of(userProduct.getId()))
                .build();
        B2BAdminAbandonedPaymentTask.Parameters parameters = new B2BAdminAbandonedPaymentTask.Parameters(CLIENT_ID,
                group.getId(), Duration.standardDays(1), "b2b_admin_abandoned_payment", mailContext,
                RegularEmailTaskPreExecutionPolicy.KEY, LocalizedEmailTaskExecutor.KEY, Option.empty());
        B2BAdminAbandonedPaymentTask task = new B2BAdminAbandonedPaymentTask(taskPreExecutionPoliciesHolder,
                Cf.list(localizedEmailTaskExecutor), taskScheduler, groupTrustPaymentRequestDao, groupServicesManager,
                groupProductDao);

        task.execute(parameters, mock(ExecutionContext.class));

        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BAdminAbandonedPaymentTask.class), 1);
        BazingaTaskManagerMock.TaskParams taskParams =
                bazingaTaskManagerStub.findTasks(B2BAdminAbandonedPaymentTask.class).first()._2;
        Assert.assertEquals(taskParams.getDate(), now.plus(Duration.standardHours(2)));
        mailSenderMockConfig.verifyNoEmailSent();
    }

    @Test
    public void shouldCancelEmailIfThereIsPaidSubscription() {
        // Create group service with positive cost
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct(
                x -> x.paymentType(GroupPaymentType.POSTPAID).pricePerUserInMonth(BigDecimal.ONE));
        psBillingGroupsFactory.createGroupService(group, groupProduct);

        MailContext mailContext = MailContext.builder()
                .to(UID)
                .language(Option.of(Language.RUSSIAN))
                .userProductId(Option.of(userProduct.getId()))
                .build();
        B2BAdminAbandonedPaymentTask.Parameters parameters = new B2BAdminAbandonedPaymentTask.Parameters(CLIENT_ID,
                group.getId(), Duration.standardDays(1), "b2b_admin_abandoned_payment", mailContext,
                RegularEmailTaskPreExecutionPolicy.KEY, LocalizedEmailTaskExecutor.KEY, Option.empty());
        B2BAdminAbandonedPaymentTask task = new B2BAdminAbandonedPaymentTask(taskPreExecutionPoliciesHolder,
                Cf.list(localizedEmailTaskExecutor), taskScheduler, groupTrustPaymentRequestDao, groupServicesManager,
                groupProductDao);

        task.execute(parameters, mock(ExecutionContext.class));

        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BAdminAbandonedPaymentTask.class), 0);
        mailSenderMockConfig.verifyNoEmailSent();
    }
}
