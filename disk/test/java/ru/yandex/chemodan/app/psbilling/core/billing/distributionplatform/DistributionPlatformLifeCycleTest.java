package ru.yandex.chemodan.app.psbilling.core.billing.distributionplatform;

import java.math.BigDecimal;
import java.util.Currency;

import org.joda.time.LocalDate;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.chemodan.app.psbilling.core.PsBillingTransactionsFactory;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.DistributionPlatformCalculationTask;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.DistributionPlatformCalculationDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.CalculationStatus;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionPlatformTransactionEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.billing.DistributionServiceTransactionCalculation;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.balanceclient.model.response.ClientActInfo;
import ru.yandex.chemodan.balanceclient.model.response.GetClientContractsResponseItem;
import ru.yandex.commune.bazinga.scheduler.OnetimeTask;
import ru.yandex.misc.test.Assert;

public class DistributionPlatformLifeCycleTest extends BaseDistributionPlatformTest {
    @Autowired
    public GroupDao groupDao;
    @Autowired
    public TaskScheduler taskScheduler;
    @Autowired
    protected DistributionPlatformCalculationDao distributionPlatformCalculationDao;

    @Test
    public void scheduleAndExecute() {
        bazingaTaskManagerStub.setAllowedTasks(Cf.list(DistributionPlatformCalculationTask.class));
        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        ListF<Tuple2<OnetimeTask, BazingaTaskManagerMock.TaskParams>> tasks =
                bazingaTaskManagerStub.tasksWithParams;
        Assert.equals(2, tasks.length());

        // таски ставим в очередь
        DistributionPlatformCalculationTask task1 = findByClientId(1L);
        DistributionPlatformCalculationTask task2 = findByClientId(2L);
        assertCalcStatus(task1, CalculationStatus.STARTING);
        assertCalcStatus(task2, CalculationStatus.STARTING);

        // выполняем одну
        bazingaTaskManagerStub.executeTask(applicationContext, task1);
        assertCalcStatus(task1, CalculationStatus.COMPLETED);
        assertCalcStatus(task2, CalculationStatus.STARTING);

        // таска выполнилась- есть расчеты
        ListF<DistributionPlatformTransactionEntity> transactions =
                distributionPlatformTransactionsDao.findTransactions(LocalDate.now().minusMonths(1), Option.empty(),
                        10000);
        Assert.equals(1, transactions.length());

        // не дожидаемся второй таски- стартуем новый расчет
        distributionPlatformCalculationService.scheduleDistributionPlatformCalculation();
        // старые таски не актуальны
        assertCalcStatus(task1, CalculationStatus.OBSOLETE);
        assertCalcStatus(task2, CalculationStatus.OBSOLETE);
        // расчеты потерли
        transactions = distributionPlatformTransactionsDao.findTransactions(LocalDate.now().minusMonths(1),
                Option.empty(), 10000);
        Assert.equals(0, transactions.length());

        // тут выполняется вторая таска - она должна упасть
        DistributionPlatformCalculationTask finalTask2 = task2;
        Assert.assertThrows(() -> bazingaTaskManagerStub.executeTask(applicationContext, finalTask2), Exception.class);

        // 2 новые таски в очереди
        task1 = findByClientId(1L);
        task2 = findByClientId(2L);
        assertCalcStatus(task1, CalculationStatus.STARTING);
        assertCalcStatus(task2, CalculationStatus.STARTING);

        // выполняем 2 новые таски
        bazingaTaskManagerStub.executeTasks(applicationContext);
        // таски выполнилась- есть расчеты
        transactions = distributionPlatformTransactionsDao.findTransactions(LocalDate.now().minusMonths(1),
                Option.empty(), 10000);
        Assert.equals(2, transactions.length());

        // таски в нужном статусе
        assertCalcStatus(task1, CalculationStatus.COMPLETED);
        assertCalcStatus(task2, CalculationStatus.COMPLETED);
    }

    @Override
    public void setup() {
        super.setup();
        bazingaTaskManagerStub.suppressNewTasksAdd();
        createGroup("clid1", 1L, Cf.list(
                new PsBillingTransactionsFactory.Service("code1").createdAt(now.minusMonths(2))), 1);
        createGroup("clid2", 2L, Cf.list(
                new PsBillingTransactionsFactory.Service("code2").createdAt(now.minusMonths(2))), 1);

        LocalDate calcMonth = now.minusMonths(1);

        GetClientContractsResponseItem contract1 = psBillingBalanceFactory.createContract(1L, 123L,
                Currency.getInstance("RUB"));
        GetClientContractsResponseItem contract2 = psBillingBalanceFactory.createContract(2L, 456L,
                Currency.getInstance("RUB"));

        LocalDate actDate = getActDt(calcMonth);
        BigDecimal client1Sum = getClientSum(calcMonth, 1L);
        BigDecimal client2Sum = getClientSum(calcMonth, 2L);

        ClientActInfo actInfo1 = createActInfo(actDate, client1Sum, client1Sum, contract1.getId());
        balanceClientStub.createOrReplaceClientActs(1L, Cf.list(actInfo1));

        ClientActInfo actInfo2 = createActInfo(actDate, client2Sum, client2Sum, contract2.getId());
        balanceClientStub.createOrReplaceClientActs(2L, Cf.list(actInfo2));

        bazingaTaskManagerStub.allowNewTasksAdd();
    }

    private DistributionPlatformCalculationTask findByClientId(Long clientId) {
        ListF<Tuple2<OnetimeTask, BazingaTaskManagerMock.TaskParams>> tasks = bazingaTaskManagerStub.tasksWithParams;
        return tasks.map(x -> (DistributionPlatformCalculationTask) x._1).find(task ->
                task.getParametersTyped().getClientId().equals(clientId)).get();
    }

    private BigDecimal getClientSum(LocalDate calcMonth, long clientId) {
        ListF<GroupServiceTransactionsDao.ExportRow> transactions =
                psBillingTransactionsFactory.calculateMonthTransactions(calcMonth);
        ListF<Group> groups = groupDao.findGroupsWithClid(1000, GroupType.ORGANIZATION, Option.empty(),
                Option.empty());
        ListF<Group> clientGroups = groups.filter(x -> x.getPaymentInfo().get().getClientId().equals(clientId));
        BigDecimal clientSum = BigDecimal.ZERO;
        for (Group group : clientGroups) {
            ListF<GroupServiceTransactionsDao.ExportRow> groupTransactions = findGroupTransactions(transactions, group);
            BigDecimal groupSum =
                    groupTransactions.map(x -> x.getGroupServiceTransaction().getAmount()).reduceLeft(BigDecimal::add);
            clientSum = clientSum.add(groupSum);
        }
        return clientSum;
    }

    private void assertCalcStatus(DistributionPlatformCalculationTask task, CalculationStatus status) {
        DistributionServiceTransactionCalculation job =
                distributionPlatformCalculationDao.find(task.getParametersTyped().getId());
        Assert.equals(status, job.getStatus());
    }
}
