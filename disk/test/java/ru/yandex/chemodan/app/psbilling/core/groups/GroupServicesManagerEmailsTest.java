package ru.yandex.chemodan.app.psbilling.core.groups;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.Instant;
import org.joda.time.ReadableInstant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTextsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupBillingService;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.EmailPaymentsAwareMembersTask;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.EmailPaymentsAwareMembersTask.Parameters;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.EmailHelper;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class GroupServicesManagerEmailsTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServicesManager groupServicesManager;
    @Autowired
    private GroupBillingService groupBillingService;
    @Autowired
    private TankerKeyDao tankerKeyDao;
    @Autowired
    private PsBillingTextsFactory psBillingTextsFactory;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private EmailHelper emailHelper;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;

    @Test
    public void sendWelcomePaid() {
        PassportUid ownerUid = PassportUid.cons(123);
        Group group = psBillingGroupsFactory.createGroup(x -> x
                .ownerUid(ownerUid)
                .paymentInfo(new BalancePaymentInfo(1L, PassportUid.cons(456))));
        psBillingGroupsFactory.createCommonEmailFeatures(group);

        ListF<String> args = Cf.list("disk_space_service", "fan_mail_limit_service", "tariff_name",
                "mail_archive_text_service", "disk_space");
        emailHelper.checkSingleLocalizedEmailArgs(EmailPaymentsAwareMembersTask.class, "welcome_paid_360", args);
        EmailPaymentsAwareMembersTask.Parameters taskParameters =
                bazingaTaskManagerMock.findTasks(EmailPaymentsAwareMembersTask.class).first()._1.getParametersTyped();
        Assert.equals(ownerUid, taskParameters.getContext().getTo());
    }

    @Test
    public void sendPaymentReminderEmails() {
        DateTime now = MoscowTime.dateTime(2010, 1, 1, 12, 0);

        Cf.list(1, 3, 5, 7, 10, 15).forEach(day ->
                createPrepaidGroupServiceVoidAtDate(
                        PassportUid.cons(day), PassportUid.cons(day), now.plusDays(day)
                ));

        groupServicesManager.sendPaymentReminderEmails(now.toLocalDate());

        ListF<Parameters> emails = collectEmailParameters();

        Assert.equals(
                Cf.list("payment_reminder_3_days", "payment_reminder_7_days", "payment_reminder_10_days"),
                emails.map(Parameters::getEmailKey)
        );
        Assert.equals(
                Cf.list(3, 7, 10).map(PassportUid::cons),
                emails.map(e -> e.getContext().getTo())
        );
    }

    @Test
    public void deduplicateReminderEmails() {
        DateTime now = MoscowTime.dateTime(2010, 2, 1, 12, 0);

        PassportUid payer = PassportUid.cons(1);
        PassportUid owner = PassportUid.cons(2);

        createPrepaidGroupServiceVoidAtDate(owner, payer, now.plusDays(3));
        createPrepaidGroupServiceVoidAtDate(owner, payer, now.plusDays(3));
        createPrepaidGroupServiceVoidAtDate(owner, payer, now.plusDays(7));

        groupServicesManager.sendPaymentReminderEmails(now.toLocalDate());

        ListF<Parameters> emails = collectEmailParameters();

        Assert.equals(
                Cf.list("payment_reminder_3_days", "payment_reminder_3_days", "payment_reminder_7_days"),
                emails.map(Parameters::getEmailKey)
        );
        Assert.equals(emails.get(0).getContext(), emails.get(1).getContext());
    }

    @Test
    public void sendBalanceExhausted() {
        PassportUid owner = PassportUid.cons(223);
        PassportUid payer = PassportUid.cons(224);

        createPrepaidGroupServiceVoidAtDate(owner, payer, DateTime.now().minusDays(1));

        groupBillingService.updateUidGroupsBillingStatus(payer);

        ListF<Parameters> emails = collectEmailParameters();

        Assert.equals(Cf.list("balance_exhausted"), emails.map(Parameters::getEmailKey));
        Assert.equals(Cf.list(owner), emails.map(p -> p.getContext().getTo()));
    }

    @Test
    public void sendBalanceExhaustedLatelyEmail() {
        String emailKey = "b2b_balance_exhausted_lately_email";
        PassportUid owner = PassportUid.cons(70);
        PassportUid payer = PassportUid.cons(24);
        createPrepaidGroupServiceVoidAtDate(owner, payer, DateTime.now().minusDays(7).minusMinutes(30), Target.DISABLED, true);

        groupServicesManager.scheduleB2bBalanceExhaustedLatelyEmails(emailKey,
                DateTime.now().minusDays(7).toInstant());

        ListF<String> args = Cf.list("uid");
        emailHelper.checkMultipleLocalizedEmailArgs(SendLocalizedEmailTask.class, emailKey, args, 2);
    }

    private void createPrepaidGroupServiceVoidAtDate(PassportUid owner, PassportUid payer, ReadableInstant voidAt) {
        createPrepaidGroupServiceVoidAtDate(owner, payer, voidAt, Target.ENABLED, false);
    }

    private void createPrepaidGroupServiceVoidAtDate(
            PassportUid owner, PassportUid payer, ReadableInstant voidAt, Target target, boolean createTankerKey
    ) {
        long clientId = new Random().nextLong();

        clientBalanceDao.createOrUpdate(
                clientId, PsBillingProductsFactory.DEFAULT_CURRENCY,
                BigDecimal.ONE, Option.of(voidAt.toInstant())
        );
        Option<UUID> tankerKeyId = createTankerKey ? Option.of(psBillingTextsFactory.create().getId()) : Option.empty();
        if (createTankerKey) {
            tankerKeyDao.mergeTranslations(Cf.list(new TankerTranslationEntity(tankerKeyId.get(), "ru", "whatever")));
        }
        psBillingGroupsFactory.createGroupService(
                psBillingGroupsFactory.createGroup(b -> b
                        .ownerUid(owner)
                        .paymentInfo(new BalancePaymentInfo(clientId, payer))
                ),
                psBillingProductsFactory.createGroupProduct(b -> b
                        .paymentType(GroupPaymentType.PREPAID)
                        .titleTankerKeyId(tankerKeyId)
                ),
                target
        );
        bazingaTaskManagerStub.clearTasks();
    }

    @Test
    public void sendB2bUpsaleEmailSuccess() {
        GroupProduct currentGroupProduct = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.ONE, 100);
        GroupProduct nextGroupProduct = psBillingProductsFactory.createProductWithCommonEmailFeatures(BigDecimal.TEN, 1000);
        psBillingProductsFactory.createGroupProductLineWithSet(Cf.list(currentGroupProduct, nextGroupProduct));

        Group group = psBillingGroupsFactory.createGroup(b -> b.ownerUid(PassportUid.cons(1)));
        psBillingGroupsFactory.createGroupService(group, currentGroupProduct);

        groupServicesManager.sendB2bUpsaleEmails(Instant.now());

        ListF<String> args = Cf.list("tariff_name_old", "disk_space_product", "tariff_name", "fan_mail_limit_product");
        emailHelper.checkSingleLocalizedEmailArgs(SendLocalizedEmailTask.class, "b2b_upsale_email", args);
    }

    private ListF<Parameters> collectEmailParameters() {
        return bazingaTaskManagerStub.tasksWithParams.map(x -> x._1)
                .filterByType(EmailPaymentsAwareMembersTask.class)
                .map(EmailPaymentsAwareMembersTask::getParameters)
                .map(Parameters.class::cast);
    }
}
