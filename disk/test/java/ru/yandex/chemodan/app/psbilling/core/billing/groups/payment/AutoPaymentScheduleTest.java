package ru.yandex.chemodan.app.psbilling.core.billing.groups.payment;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.AutoPayManager;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.tasks.GroupAutoBillingTask;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.misc.test.Assert;

public class AutoPaymentScheduleTest extends BaseAutoPayTest {
    @Autowired
    private AutoPayManager autoPayManager;

    @Test
    public void noTaskForGroupWithoutClientBalance() {
        createGroupWithoutClientBalance();

        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noTaskForGroupWithoutServices() {
        createGroupWithoutServices();

        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForGroupWithPostpaidServices() {
        createGroupWithPostpaidServices();

        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForFarVoidBalance() {
        createGroupWithFarVoidBalance();

        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(0);
    }

    @Test
    public void noChargeForGroupWithoutEnabledAutoPay() {
        createGroupWithoutEnabledAutoPay();

        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(0);
    }

    @Test
    public void successWithDefaultProduct() {
        createGroupWithDefaultProduct();
        autoPayManager.scheduleAutoPayment();
        verifyTaskCount(1);
    }

    @Test
    public void schedule() {
        setupCorrectData(1L);
        setupCorrectData(2L);
        autoPayManager.scheduleAutoPayment();

        verifyTaskCount(2);
    }

    @Test
    public void doubleSchedule() {
        setupCorrectData(1L);
        autoPayManager.scheduleAutoPayment();
        autoPayManager.scheduleAutoPayment();

        verifyTaskCount(1);
    }

    public void before(){
        super.before();
        bazingaTaskManagerStub.setAllowedTasks(Cf.list(GroupAutoBillingTask.class));
        bazingaTaskManagerStub.clearTasks();
    }

    protected GroupService setupCorrectData(long clientId) {
        return autoPaymentUtils.createDataForAutoPay(clientId);
    }

    protected GroupService setupCorrectData(long clientId, GroupProduct product) {
        return autoPaymentUtils.createDataForAutoPay(clientId, product);
    }

    private void verifyTaskCount(int count) {
        ListF<GroupAutoBillingTask> tasks = bazingaTaskManagerStub.findTasks(GroupAutoBillingTask.class).map(x -> x._1);
        Assert.equals(count, tasks.length());
    }
}
