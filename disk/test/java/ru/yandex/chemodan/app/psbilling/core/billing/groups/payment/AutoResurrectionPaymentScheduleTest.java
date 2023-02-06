package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import java.util.Comparator;
import java.util.Currency;

import org.joda.time.Duration;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.AutoResurrectionPayManager;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.tasks.GroupAutoReBillingTask;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardPurpose;
import ru.yandex.chemodan.app.psbilling.core.entities.cards.CardStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.GroupTrustPaymentRequest;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.utils.AssertHelper;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.CheckRequestPaymentResponse;
import ru.yandex.misc.test.Assert;

public class AutoResurrectionPaymentScheduleTest extends BaseAutoPayTest {
    @Autowired
    private AutoResurrectionPayManager autoPayManager;
    @Autowired
    private AutoPaymentUtils autoPaymentUtils;

    @Test
    public void noTaskForGroupWithoutDisabledServices() {
        autoPaymentUtils.createDataForAutoPay(clientId);

        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForGroupWithPostpaidServices() {
        GroupService service = createGroupWithPostpaidServices();
        autoPaymentUtils.voidBalanceAndDisableService(clientId, service);
        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForFarVoidBalance() {
        autoPaymentUtils.createCorrectDataForResurrection(clientId);
        DateUtils.shiftTime(settings.getAutoResurrectMaxResurrectTimeMinutes().plus(1));

        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForGroupWithoutEnabledAutoPay() {
        autoPaymentUtils.createCorrectDataForResurrection(clientId);
        groupDao.setAutoBillingForClient(clientId, false);

        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForGroupWithoutPrimaryCard() {
        autoPaymentUtils.createCorrectDataForResurrection(clientId);
        CardEntity card = cardDao.findB2BPrimary(uid).get();
        cardDao.updatePurpose(uid, card.getExternalId(), CardPurpose.B2C);
        paymentFactory.insertCard(uid, CardPurpose.B2B_PRIMARY, x->x.status(CardStatus.DISABLED));

        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);
    }

    @Test
    public void successWithDefaultProduct() {
        GroupService service = autoPaymentUtils.createCorrectDataForResurrection(clientId);
        Group group = groupDao.findById(service.getGroupId());
        psBillingGroupsFactory.createDefaultProductService(group);

        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTasks(Cf.list(Tuple2.tuple(clientId, rub)));
    }

    @Test
    public void noChargeForToSoonAttempt() {
        DateUtils.unfreezeTime(); // to correct get last payment
        setupCorrectData(clientId);
        DateUtils.unfreezeTime(); // to correct get last payment
        GroupTrustPaymentRequest request = null;
        for (int i = 0; i < settings.getAutoPayChargePlan().size(); i++) {
            autoPayManager.scheduleAutoResurrectionPayment();
            verifyTaskCount(1);
            bazingaTaskManagerStub.executeTasks();
            request = checkLastPaymentRequest(CheckRequestPaymentResponse.ERROR_NOT_ENOUGH_FUNDS);
        }

        // before border
        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);

        // border
        DateUtils.freezeTime(request.getCreatedAt());
        DateUtils.shiftTime(settings.getAutoResurrectMinRetryIntervalMinutes());
        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTaskCount(0);

        // after border
        DateUtils.shiftTime(Duration.standardSeconds(1));
        autoPayManager.scheduleAutoResurrectionPayment();
        verifyTasks(Cf.list(Tuple2.tuple(clientId, rub)));
    }

    @Test
    public void schedule() {
        autoPaymentUtils.createCorrectDataForResurrection(1L);
        autoPaymentUtils.createCorrectDataForResurrection(2L);
        autoPayManager.scheduleAutoResurrectionPayment();

        verifyTasks(Cf.list(Tuple2.tuple(1L, rub), Tuple2.tuple(2L, rub)));
    }

    @Test
    public void scheduleDifferentCurrency() {
        GroupProduct rubProduct = psBillingProductsFactory.createPrepaidProduct(rub);
        GroupProduct usdProduct = psBillingProductsFactory.createPrepaidProduct(usd);
        autoPaymentUtils.createCorrectDataForResurrection(1L, rubProduct);
        autoPaymentUtils.createCorrectDataForResurrection(1L, usdProduct);
        autoPayManager.scheduleAutoResurrectionPayment();

        verifyTasks(Cf.list(Tuple2.tuple(1L, rub), Tuple2.tuple(1L, usd)));
    }

    @Test
    public void doubleSchedule() {
        autoPaymentUtils.createCorrectDataForResurrection(1L);
        autoPayManager.scheduleAutoResurrectionPayment();
        autoPayManager.scheduleAutoResurrectionPayment();

        verifyTasks(Cf.list(Tuple2.tuple(1L, rub)));
    }

    private void verifyTaskCount(int count) {
        ListF<GroupAutoReBillingTask> tasks = bazingaTaskManagerStub.findTasks(GroupAutoReBillingTask.class).map(x
                -> x._1);
        AssertHelper.assertSize(tasks, count);
    }

    private void verifyTasks(ListF<Tuple2<Long, Currency>> expectedData) {
        ListF<GroupAutoReBillingTask> tasks =
                bazingaTaskManagerStub.findTasks(GroupAutoReBillingTask.class).map(x -> x._1);

        Comparator<Tuple2<Long, String>> comparator =
                (o1, o2) -> o1._1.compareTo(o2._1) == 0
                        ? o1._2.compareTo(o2._2)
                        : o1._1.compareTo(o2._1);

        ListF<Tuple2<Long, String>> expected =
                expectedData.map(x -> Tuple2.tuple(x._1, x._2.getCurrencyCode())).sorted(comparator);
        ListF<Tuple2<Long, String>> actual = tasks.map(t ->
                Tuple2.tuple(t.getParametersTyped().getClientId(), t.getParametersTyped().getCurrency()))
                .sorted(comparator);
        Assert.assertListsEqual(expected, actual);
    }

    public void before() {
        super.before();
        bazingaTaskManagerStub.setAllowedTasks(Cf.list(GroupAutoReBillingTask.class));
        bazingaTaskManagerStub.clearTasks();
        featureFlags.getAutoResurrectionPayAutoOn().setValue("true");
    }

    protected GroupService setupCorrectData(long clientId) {
        return autoPaymentUtils.createCorrectDataForResurrection(clientId);
    }

    protected GroupService setupCorrectData(long clientId, GroupProduct product) {
        return autoPaymentUtils.createCorrectDataForResurrection(clientId, product);
    }
}
