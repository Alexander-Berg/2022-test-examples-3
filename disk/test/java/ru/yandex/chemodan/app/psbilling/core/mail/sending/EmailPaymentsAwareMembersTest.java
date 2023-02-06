package ru.yandex.chemodan.app.psbilling.core.mail.sending;

import java.util.Random;

import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.balance.BalanceService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.BaseEmailTask.Parameters;
import ru.yandex.chemodan.app.psbilling.core.mail.tasks.SendLocalizedEmailTask;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetClientPersonsResponseItem;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.test.Assert;

public class EmailPaymentsAwareMembersTest extends AbstractPsBillingCoreTest {

    @Autowired
    private TaskScheduler taskScheduler;
    @Autowired
    private BazingaTaskManagerMock bazingaTaskManagerMock;
    @Autowired
    private BalanceService balanceService;
    @Autowired
    private BalanceClientStub balanceClientStub;
    @Autowired
    protected Blackbox2MockConfiguration blackbox2MockConfig;

    @Test
    public void testSimple() {
        PassportUid uid = PassportUid.cons(333);
        Group group = createGroup(uid, uid, Option.empty());

        String templateKey = "templateKey";
        Duration cooldown = Duration.standardHours(1);
        MailContext context = MailContext.builder()
                .to(uid)
                .balanceVoidDate(Option.of(new LocalDate(2022, 2, 24)))
                .build();

        executeTask(group, templateKey, context, Option.of(cooldown));

        Parameters parameters = collectEmailParameters().single();
        Assert.equals(templateKey, parameters.getEmailKey());
        Assert.equals(context, parameters.getContext());
        Assert.some(cooldown, parameters.getCooldown());
    }

    @Test
    public void testFullHouse() {
        PassportUid owner = PassportUid.cons(336);
        PassportUid payer = PassportUid.cons(337);
        Email email = new Email("somebody@somewhere.com");

        executeTask(createGroup(owner, payer, Option.of(email)));

        ListF<Parameters> parameters = collectEmailParameters();
        Assert.hasSize(3, parameters);

        Assert.equals(owner, parameters.get(0).getUser());
        Assert.equals(payer, parameters.get(1).getUser());
        Assert.some(email, parameters.get(2).getContext().getEmail());
    }

    @Test
    public void testEmailMatch() {
        PassportUid owner = PassportUid.cons(340);
        PassportUid payer = PassportUid.cons(341);
        PassportUid slayer = PassportUid.cons(342);
        Email email = new Email("somebody@somewhere.com");

        blackbox2MockConfig.mockUserInfo(email, Blackbox2MockConfiguration.getUidResponse(owner));

        executeTask(createGroup(owner, payer, Option.of(email)));
        Assert.isEmpty(collectEmailParameters().filterMap(p -> p.getContext().getEmail()));

        bazingaTaskManagerMock.clearTasks();
        blackbox2MockConfig.mockUserInfo(email, Blackbox2MockConfiguration.getUidResponse(payer));

        executeTask(createGroup(owner, payer, Option.of(email)));
        Assert.isEmpty(collectEmailParameters().filterMap(p -> p.getContext().getEmail()));

        bazingaTaskManagerMock.clearTasks();
        blackbox2MockConfig.mockUserInfo(email, Blackbox2MockConfiguration.getUidResponse(slayer));

        executeTask(createGroup(owner, payer, Option.of(email)));
        Assert.equals(Cf.list(email), collectEmailParameters().filterMap(p -> p.getContext().getEmail()));
    }

    private void executeTask(Group group) {
        executeTask(group, "key", MailContext.builder().to(group.getOwnerUid()).build(), Option.empty());
    }

    private void executeTask(Group group, String templateKey, MailContext context, Option<Duration> cooldown) {
        taskScheduler.schedulePaymentsAwareMembersEmailTask(group, templateKey, context, cooldown);
        bazingaTaskManagerMock.executeTasks(applicationContext);
    }

    private Group createGroup(PassportUid owner, PassportUid payer, Option<Email> paymentEmail) {
        long clientId = new Random().nextLong();

        Group group = psBillingGroupsFactory.createGroup(b -> b
                .ownerUid(owner)
                .paymentInfo(new BalancePaymentInfo(clientId, payer)));

        if (paymentEmail.isPresent()) {
            GetClientContractsResponseItem contract = GetClientContractsResponseItem.buildActiveBalanceContract();
            contract.setId(clientId);
            contract.setPersonId(0L);
            contract.setServices(Cf.set(balanceService.getServiceId()));
            balanceClientStub.createOrReplaceClientContract(clientId, contract);

            GetClientPersonsResponseItem person = new GetClientPersonsResponseItem();
            person.setId(0L);
            person.setEmail(paymentEmail.get().getEmail());
            person.setType(GetClientPersonsResponseItem.LEGAL_PERSON_TYPE);
            balanceClientStub.setClientPersons(clientId, Cf.list(person));
        }
        return group;
    }

    private ListF<Parameters> collectEmailParameters() {
        return bazingaTaskManagerStub.tasksWithParams.map(x -> x._1)
                .filterByType(SendLocalizedEmailTask.class)
                .map(SendLocalizedEmailTask::getParameters)
                .map(Parameters.class::cast);
    }
}
