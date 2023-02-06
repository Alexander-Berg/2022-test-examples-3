package ru.yandex.chemodan.app.psbilling.core.billing.groups;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingBalanceFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.billing.ClientBalanceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.TrialDefinitionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.ClientBalanceEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.chemodan.balanceclient.model.response.GetPartnerBalanceContractResponseItem;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class UpdateGroupServiceTest extends AbstractPsBillingCoreTest {
    @Autowired
    private GroupServiceDao groupServiceDao;
    @Autowired
    private PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    private GroupBillingService groupBillingService;
    @Autowired
    private PsBillingTransactionsFactory psBillingTransactionsFactory;
    @Autowired
    private ClientBalanceDao clientBalanceDao;
    @Autowired
    private UserServiceDao userServiceDao;

    private Group group;
    private PassportUid uid = PassportUid.cons(1234L);
    private long clientId = 1;

    @Before
    public void setup() {
        DateUtils.freezeTime(new DateTime(2021, 11, 1, 0, 0, 0));
        psBillingTransactionsFactory.setupMocks();
        group = psBillingGroupsFactory.createGroup(x ->
                x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
    }

    @Test
    public void free_bdate() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.pricePerUserInMonth(BigDecimal.ZERO));

        GroupService groupService1 = createService(groupProduct);
        GroupService groupService2 = createService(groupProduct);
        assertBdate(groupService1, Option.empty());
        assertBdate(groupService2, Option.empty());

        groupBillingService.updateUidGroupsBillingStatus(uid);

        groupService1 = assertBdate(groupService1, Option.empty());
        groupService2 = assertBdate(groupService2, Option.empty());
        Assert.equals(Target.ENABLED, groupService1.getTarget());
        Assert.equals(Target.ENABLED, groupService2.getTarget());
    }

    @Test
    public void postPaid_bdate_active() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.TEN));
        GroupService groupService = createService(groupProduct);
        assertBdate(groupService, Instant.now());

        groupBillingService.updateUidGroupsBillingStatus(uid);

        groupService = assertBdate(groupService, Instant.now().plus(group.getGracePeriod().get()));
        Assert.equals(Target.ENABLED, groupService.getTarget());
    }

    @Test
    public void postPaid_bdate_paymentRequired() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withFirstDebtAmount(BigDecimal.valueOf(100)).withFirstDebtPaymentTermDT(DateUtils.futureDate()));

        GroupService groupService = createService(group, groupProduct, 1);
        ;
        assertBdate(groupService, Instant.now());

        groupBillingService.updateUidGroupsBillingStatus(uid);

        groupService = assertBdate(groupService, DateUtils.futureDate());
        Assert.equals(Target.ENABLED, groupService.getTarget());
    }

    @Test
    public void postPaid_bdate_debtExist() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withExpiredDebtAmount(BigDecimal.TEN).withExpiredDT(DateUtils.pastDate()));
        GroupService groupService = createService(group, groupProduct, 1);
        assertBdate(groupService, Instant.now());

        groupBillingService.updateUidGroupsBillingStatus(uid);

        groupService = assertBdate(groupService, Instant.now());
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_bdate_active_balancePositive() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(300)));
        GroupService groupService = createService(groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, new DateTime(2021, 12, 1, 0, 0, 0).toInstant());

        // no changes cause transactions will be count
        DateUtils.shiftTime(Duration.standardHours(12));
        groupBillingService.updateUidGroupsBillingStatus(uid);
        assertBdate(groupService, groupService.getNextBillingDate().get());
        Assert.equals(Target.ENABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_bdate_active_balanceNegative() {
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(-1000)));
        GroupService groupService = createService(groupProduct);

        // should disable service on first check
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, Instant.now());
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_bdate_paymentRequired() {
        // not really possible case but should respect it
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(1000))
                        .withFirstDebtAmount(BigDecimal.valueOf(100))
                        .withFirstDebtPaymentTermDT(DateUtils.futureDate()));

        GroupService groupService = createService(groupProduct);

        // payment deadline < bdate
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, DateUtils.futureDate()); // payment deadline < bdate
        Assert.equals(Target.ENABLED, groupService.getTarget());

    }

    @Test
    public void prePaid_bdate_debtExist() {
        // not really possible case but should respect it
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID));
        GetClientContractsResponseItem contract = psBillingBalanceFactory.createContract(
                group.getPaymentInfo().get().getClientId(), 1, groupProduct.getPriceCurrency());
        psBillingBalanceFactory.createContractBalance(contract, x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(1000)).withExpiredDebtAmount(BigDecimal.TEN).withExpiredDT(DateUtils.pastDate()));

        GroupService groupService = createService(groupProduct);
        Assert.equals(Instant.now(), groupService.getNextBillingDate().get());

        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = reload(groupService);
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_bdate_disableService() {
        LocalDate now = LocalDate.now();
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(300)));
        GroupService groupService = createService(groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, new DateTime(2021, 12, 1, 0, 0, 0).toInstant());

        // no changes cause transactions will be count
        DateUtils.freezeTime(groupService.getNextBillingDate().get());
        psBillingTransactionsFactory.calculateTransactions(now, LocalDate.now());
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, groupService.getNextBillingDate().get());
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_bdate_update() {
        LocalDate now = LocalDate.now();
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300)));
        GetPartnerBalanceContractResponseItem contractBalance =
                psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                        x.withClientPaymentsSum(BigDecimal.valueOf(300)));
        GroupService groupService = createService(groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        Instant nextBdate = new DateTime(2021, 12, 1, 0, 0, 0).toInstant();
        groupService = assertBdate(groupService, nextBdate);

        contractBalance.withClientPaymentsSum(BigDecimal.valueOf(600));
        DateUtils.freezeTime(nextBdate);
        psBillingTransactionsFactory.calculateTransactions(now, LocalDate.now());
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, new DateTime(2022, 1, 1, 0, 0, 0).toInstant());
        Assert.equals(Target.ENABLED, groupService.getTarget());
    }

    @Test
    @Ignore
    // https://st.yandex-team.ru/CHEMODAN-81227
    public void prePaid_round_money_real() {
        DateUtils.freezeTime(new DateTime(2021, 12, 12, 0, 0, 0));
        LocalDate now = LocalDate.now();
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(699)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(244.89)));

        ListF<GroupService> services = Cf.arrayList();
        services.add(createService(group, groupProduct, 3));

        for (int i = 0; i < 9; i++) {
            Group group = psBillingGroupsFactory.createGroup(x ->
                    x.paymentInfo(new BalancePaymentInfo(1L, uid)));
            services.add(createService(group, groupProduct, 1));
        }

        groupBillingService.updateUidGroupsBillingStatus(uid);
        Instant expectedBdate = new DateTime(2021, 12, 12, 21, 43, 17).toInstant();
        for (GroupService gs : services) {
            assertBdate(gs, expectedBdate);
        }

        DateUtils.freezeTime(expectedBdate);
        groupBillingService.updateUidGroupsBillingStatus(uid);
        ClientBalanceEntity balance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();

        Assert.equals(BigDecimal.ZERO, balance.getBalanceAmount()); // actual -0.02
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81227
    public void prePaid_round_money_real_one_user() {
        DateUtils.freezeTime(new DateTime(2021, 12, 12, 0, 0, 0));
        LocalDate now = LocalDate.now();
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(699)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(244.89)));

        ListF<GroupService> services = Cf.arrayList();
        services.add(createService(group, groupProduct, 11));

        groupBillingService.updateUidGroupsBillingStatus(uid);
        ClientBalanceEntity balance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();
        DateUtils.freezeTime(balance.getBalanceVoidAt().get());
        groupBillingService.updateUidGroupsBillingStatus(uid);

        balance = clientBalanceDao.find(group.getPaymentInfo().get().getClientId(),
                groupProduct.getPriceCurrency()).get();

        Assert.equals(0, BigDecimal.ZERO.compareTo(balance.getBalanceAmount()));
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81277
    public void prePaid_round_money_real_81277() {
        DateUtils.freezeTime(new DateTime(2021, 12, 14, 0, 0, 0));
        LocalDate now = LocalDate.now();
        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(129)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(129 - 108.72)));

        GroupService service = createService(group, groupProduct, 10);
        groupBillingService.updateUidGroupsBillingStatus(uid);

        groupServiceDao.updateNextBillingDate(service.getId(),
                Option.of(new DateTime(2021, 12, 14, 5, 46, 30).toInstant()));
        DateUtils.freezeTime(new DateTime(2021, 12, 14, 5, 50, 9));
        groupBillingService.updateUidGroupsBillingStatus(uid);

        ClientBalanceEntity balance = assertBalance(clientId, groupProduct, 10.27);

        DateUtils.freezeTime(balance.getBalanceVoidAt().get());
        groupBillingService.updateUidGroupsBillingStatus(uid);
        assertBalance(clientId, groupProduct, 0);
    }

    @Test
    // https://st.yandex-team.ru/CHEMODAN-81310
    public void prePaid_round_money_real_81310() {
        DateUtils.freezeTime(new DateTime(2021, 12, 16, 0, 0, 0));

        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(699)));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(129 - 3.1 - 4.16)));

        GroupService service = createService(group, groupProduct, 17);
        ListF<UserServiceEntity> userServices = userServiceDao.findAll();
        Assert.equals(17, userServices.size());
        ListF<DateTime> usStartDates = Cf.list(
                new DateTime(2021, 12, 16, 0, 34, 0, 205),
                new DateTime(2021, 12, 16, 0, 34, 0, 357),
                new DateTime(2021, 12, 16, 0, 34, 0, 322),
                new DateTime(2021, 12, 16, 0, 33, 0, 264),
                new DateTime(2021, 12, 16, 0, 33, 0, 297),
                new DateTime(2021, 12, 16, 0, 33, 0, 195),
                new DateTime(2021, 12, 16, 0, 33, 0, 264),
                new DateTime(2021, 12, 16, 0, 33, 0, 297),
                new DateTime(2021, 12, 16, 0, 33, 0, 153),
                new DateTime(2021, 12, 16, 0, 33, 0, 195),
                new DateTime(2021, 12, 16, 0, 33, 0, 264),
                new DateTime(2021, 12, 16, 0, 33, 0, 297),
                new DateTime(2021, 12, 16, 0, 33, 0, 297),
                new DateTime(2021, 12, 16, 0, 33, 0, 195),
                new DateTime(2021, 12, 16, 0, 31, 0, 168),
                new DateTime(2021, 12, 16, 0, 31, 0, 322),
                new DateTime(2021, 12, 16, 0, 30, 0, 153));

        for (int i = 0; i < usStartDates.size(); i++) {
            Instant activatedAt = usStartDates.get(i).toInstant();
            DateUtils.freezeTime(activatedAt);
            userServiceDao.setStatusActual(Cf.list(userServices.get(i).getId()), Target.ENABLED);
        }

        DateUtils.freezeTime(usStartDates.max()); //2021-12-16T00:34:00.357+03:00

        groupServiceDao.updateNextBillingDate(service.getId(),
                Option.of(new DateTime(2021, 12, 16, 7, 37, 20).toInstant()));
        DateUtils.freezeTime(new DateTime(2021, 12, 16, 7, 40, 10));
        groupBillingService.updateUidGroupsBillingStatus(uid);

        ClientBalanceEntity balance = clientBalanceDao.find(clientId, groupProduct.getPriceCurrency()).get();
        DateUtils.freezeTime(balance.getBalanceVoidAt().get());
        groupBillingService.updateUidGroupsBillingStatus(uid);
        assertBalance(clientId, groupProduct, 0);
    }

    @Test
    public void prePaid_trial_zeroBalance() {
        DateUtils.freezeTime("2021-11-01");
        LocalDate now = LocalDate.now();

        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .duration(Option.of(15))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_DAY)));

        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300))
                        .trialDefinitionId(Option.of(trialDefinition.getId())));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(0)));
        GroupService groupService = createService(groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        Instant nextBdate = now.plusDays(15).toDateTimeAtStartOfDay().toInstant();
        groupService = assertBdate(groupService, nextBdate);
        Assert.equals(Target.ENABLED, groupService.getTarget());

        DateUtils.freezeTime(nextBdate);
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, nextBdate);
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    @Test
    public void prePaid_trial_notZeroBalance() {
        DateUtils.freezeTime("2021-11-01");
        LocalDate now = LocalDate.now();

        TrialDefinitionEntity trialDefinition = psBillingProductsFactory.createTrialDefinitionWithPeriod(x -> x
                .duration(Option.of(15))
                .durationMeasurement(Option.of(CustomPeriodUnit.ONE_DAY)));

        GroupProduct groupProduct =
                psBillingProductsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.PREPAID)
                        .pricePerUserInMonth(BigDecimal.valueOf(300))
                        .trialDefinitionId(Option.of(trialDefinition.getId())));
        psBillingBalanceFactory.createContractBalance(createContract(groupProduct), x ->
                x.withClientPaymentsSum(BigDecimal.valueOf(10))); // 1 day
        GroupService groupService = createService(groupProduct);

        groupBillingService.updateUidGroupsBillingStatus(uid);
        Instant nextBdate = now.plusDays(15 + 1).toDateTimeAtStartOfDay().toInstant();
        groupService = assertBdate(groupService, nextBdate);
        Assert.equals(Target.ENABLED, groupService.getTarget());

        DateUtils.freezeTime(nextBdate);
        psBillingTransactionsFactory.calculateTransactions(now, LocalDate.now());
        groupBillingService.updateUidGroupsBillingStatus(uid);
        groupService = assertBdate(groupService, nextBdate);
        Assert.equals(Target.DISABLED, groupService.getTarget());
    }

    private GetClientContractsResponseItem createContract(GroupProduct groupProduct) {
        return psBillingBalanceFactory.createContract(
                group.getPaymentInfo().get().getClientId(), 1, groupProduct.getPriceCurrency());
    }

    private GroupService reload(GroupService groupService) {
        return groupServiceDao.findById(groupService.getId());
    }

    private GroupService assertBdate(GroupService groupService, Instant date) {
        return assertBdate(groupService, Option.of(date));
    }

    private GroupService assertBdate(GroupService groupService, Option<Instant> date) {
        groupService = reload(groupService);
        Assert.equals(date, groupService.getNextBillingDate());
        return groupService;
    }

    private ClientBalanceEntity assertBalance(long clientId, GroupProduct groupProduct, double expected) {
        BigDecimal expectedBalance = BigDecimal.valueOf(expected);
        ClientBalanceEntity balance = clientBalanceDao.find(clientId, groupProduct.getPriceCurrency()).get();
        Assert.equals(0, expectedBalance.setScale(4, RoundingMode.DOWN)
                        .compareTo(balance.getBalanceAmount().setScale(4, RoundingMode.DOWN)),
                String.format("expected balance to be %s but found %s", expectedBalance, balance.getBalanceAmount()));
        return balance;
    }

    private GroupService createService(Group group, GroupProduct groupProduct, int userCount) {
        return psBillingTransactionsFactory.createService(group,
                new PsBillingTransactionsFactory.Service(groupProduct.getCode()), userCount);
    }

    private GroupService createService(GroupProduct groupProduct) {
        return createService(group, groupProduct, 1);
    }
}
