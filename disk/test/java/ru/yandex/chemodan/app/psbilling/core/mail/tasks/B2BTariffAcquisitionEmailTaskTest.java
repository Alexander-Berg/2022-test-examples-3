package ru.yandex.chemodan.app.psbilling.core.mail.tasks;

import org.apache.http.client.methods.HttpPost;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.sending.BaseEmailSendTest;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.inside.passport.PassportUid;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.FAN_MAIL_LIMIT_FEATURE_CODE;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;
import static ru.yandex.misc.test.Assert.assertContains;

public class B2BTariffAcquisitionEmailTaskTest extends BaseEmailSendTest {
    private static final PassportUid UID = new PassportUid(123);
    private static final long CLIENT_ID = 42L;
    private Group group;
    private UserProductEntity userProduct;

    @Autowired
    private TaskScheduler taskScheduler;
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
        group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(CLIENT_ID, UID)));

        ListF<String> args = Cf.list("tariff_name", "disk_space_product", "fan_mail_limit_product",
                "mail_archive_text_product", "uid");
        emailHelper.createTemplateWithRuLocalization("b2b_admin_abandoned_payment", args);

        args = Cf.list("uid");
        emailHelper.createTemplateWithRuLocalization("b2b_tariff_acquisition", args);
    }

    @Test
    public void successStory() {
        taskScheduler.scheduleB2bTariffAcquisitionEmailTask(UID);
        bazingaTaskManagerStub.executeTasks(applicationContext);

        HttpPost request = mailSenderMockConfig.verifyEmailSent();
        assertContains(request.getURI().toString(), UID.toString());
    }

    @Test
    public void shouldCancelEmailIfAbandonedPaymentEmailWasSent() {
        taskScheduler.scheduleB2bTariffAcquisitionEmailTask(UID);

        DateUtils.shiftTime(Duration.standardHours(1));
        taskScheduler.scheduleB2bAdminAbandonedPaymentTask(UID, CLIENT_ID, userProduct.getId(), group.getId());
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BTariffAcquisitionEmailTask.class), 1);
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BAdminAbandonedPaymentTask.class), 1);

        bazingaTaskManagerStub.executeTasks(B2BAdminAbandonedPaymentTask.class);
        HttpPost request = mailSenderMockConfig.verifyEmailSent();
        assertContains(request.getURI().toString(), UID.toString());
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BTariffAcquisitionEmailTask.class), 1);
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BAdminAbandonedPaymentTask.class), 0);

        mailSenderMockConfig.reset();
        bazingaTaskManagerStub.executeTasks(B2BTariffAcquisitionEmailTask.class);
        AssertHelper.assertSize(bazingaTaskManagerStub.findTasks(B2BTariffAcquisitionEmailTask.class), 0);
        mailSenderMockConfig.verifyNoEmailSent();
    }
}
