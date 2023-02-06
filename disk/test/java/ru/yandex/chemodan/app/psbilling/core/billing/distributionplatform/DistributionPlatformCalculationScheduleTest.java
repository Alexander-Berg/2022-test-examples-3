package ru.yandex.chemodan.app.psbilling.core.billing.distributionplatform;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.DistributionPlatformCalculationTask;
import ru.yandex.misc.test.Assert;

public class DistributionPlatformCalculationScheduleTest extends BaseDistributionPlatformTest {
    @Test
    // группа без сервисов не считается
    public void scheduleTasks_noTaskForEmptyGroup() {
        createGroup("clid", 1L, Cf.list());
        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    // группа с только бесплатным продуктом не считается
    public void scheduleTasks_noTaskForFreeProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("default").createdAt(now.minusMonths(1)).skipTransactionsExport(true)));
        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    // услуга выключена давно- не будем тратить время на подсчет отчислений
    public void scheduleTasks_noTaskForLongDisabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(3)).disabledAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    // услуга выключена в прошлом месяце - отчисления по неполному месяцу должны быть
    public void scheduleTasks_taskForLastMonthDisabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2)).disabledAt(now.minusMonths(1))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
        Assert.equals(1L, ((DistributionPlatformCalculationTask) bazingaTaskManagerStub.tasksWithParams.get(0)._1)
                .getParametersTyped().getClientId());
    }

    @Test
    // услуга выключена только что- отчисления по полному месяцу должны быть
    public void scheduleTasks_taskForRecentDisabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2)).disabledAt(now.minusDays(1))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
        Assert.equals(1L, ((DistributionPlatformCalculationTask) bazingaTaskManagerStub.tasksWithParams.get(0)._1)
                .getParametersTyped().getClientId());
    }

    @Test
    // услуга включена только что- отчислений за прошлый месяц не было- нет смысла считать
    public void scheduleTasks_taskForRecentEnabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusDays(1))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    // услуга включена давно - отчисления должны быть
    public void scheduleTasks_taskForLongEnabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
        Assert.equals(1L, ((DistributionPlatformCalculationTask) bazingaTaskManagerStub.tasksWithParams.get(0)._1)
                .getParametersTyped().getClientId());
    }

    @Test
    // реальный случай - дефолтный продукт и платный - отчисления должны быть
    public void scheduleTasks_taskForLongTwoEnabledProduct() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("default").createdAt(now.minusMonths(2)).skipTransactionsExport(true),
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(1, bazingaTaskManagerStub.tasksWithParams.length());
        Assert.equals(1L, ((DistributionPlatformCalculationTask) bazingaTaskManagerStub.tasksWithParams.get(0)._1)
                .getParametersTyped().getClientId());
    }

    @Test
    // группа без клида не считается
    public void scheduleTasks_noTaskForGroupWithoutClid() {
        createGroup(null, 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(0, bazingaTaskManagerStub.tasksWithParams.length());
    }

    @Test
    // одна таска для одного клиента
    public void scheduleTasks_oneTaskForOneClient() {
        createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))));
        createGroup("clid2", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code2").createdAt(now.minusMonths(2))));
        createGroup("clid3", 2L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        ListF<Long> clientIds = bazingaTaskManagerStub.tasksWithParams
                .map(x -> ((DistributionPlatformCalculationTask) x._1).getParametersTyped().getClientId());
        Assert.equals(2, clientIds.length());
        Assert.assertContains(clientIds, 1L);
        Assert.assertContains(clientIds, 2L);
    }

    @Test
    // если поставить две таски - должно быть две таски - мы же запускаем перерасчет с нуля за месяц,
    // старые таски уже помечены в базке как obsolete
    public void scheduleTasks_doubleSchedule() {
        createGroup("clid", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code").createdAt(now.minusMonths(2))));

        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        bazingaTaskManagerStub.filterTaskQueue(x -> x instanceof DistributionPlatformCalculationTask);
        Assert.equals(2, bazingaTaskManagerStub.tasksWithParams.length());
    }
}
