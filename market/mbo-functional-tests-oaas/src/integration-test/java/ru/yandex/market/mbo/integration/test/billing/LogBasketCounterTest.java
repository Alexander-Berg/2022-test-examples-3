package ru.yandex.market.mbo.integration.test.billing;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.junit.Test;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.tt.LogBasketCounter;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.core.matcher.model.UnsuccessfulLogType;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.integration.test.tt.TtTaskInitializer;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.legacy.LogTaskManager;
import ru.yandex.market.mbo.tt.model.Task;
import ru.yandex.market.mbo.tt.model.TaskList;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings({"checkstyle:magicnumber", "checkstyle:linelength"})
public class LogBasketCounterTest extends BaseIntegrationTest {

    private static final long CATEGORY_ID = 73404L;
    private static final String ACCEPTED_TO_BASKET = "6d5d5a81a7eebd6fa47cb04cea0a28ff";
    private static final String ACCEPTED_CANT_IMPROVE = "541ddba9c454f117b605c903e5874dc8";
    private static final String ACCEPTED_CANT_IMPROVE_MODIF = "508a361eabe0e7dbf8a214808bfe9017";
    private static final String NO_CHECK_TO_BASKET = "3e6bff907afda35bf3c10ba59277b0d8";
    private static final String NO_CHECK_CANT_IMPROVE = "47b85933a637b9457e130cd0c984aa02";
    private static final String NO_CHECK_CANT_IMPROVE_MODIF = "c860566f0919fe9d7109fa4003f123bc";
    private static final String NOT_ACCEPTED = "f01e711d322d7ffabbffe8efa3442fbc";
    private static final String ACCEPTED_OTHER_KIND = "d37be4853f55a8678dc77195e04cfbe7";
    private static final Calendar FROM_DATE = Calendar.getInstance();
    private static final Calendar TO_DATE = Calendar.getInstance();
    private static final Pair<Calendar, Calendar> INTERVAL = new Pair<>(FROM_DATE, TO_DATE);

    static {
        FROM_DATE.add(Calendar.HOUR, -6);
        TO_DATE.add(Calendar.HOUR, 6);
    }

    @Resource(name = "logBasketCounter")
    private LogBasketCounter counter;

    @Resource(name = "siteCatalogJdbcTemplate")
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "logTaskManager")
    private LogTaskManager logTaskManager;

    @Resource(name = "taskTracker")
    private TaskTracker taskTracker;

    @Resource(name = "tarifProvider")
    private TarifProvider tarifProvider;

    @Resource(name = "statusManager")
    private StatusManager statusManager;

    @Resource(name = "autoUser")
    private AutoUser autoUser;

    @Resource(name = "siteCatalogPgJdbcTemplate")
    private JdbcOperations siteCatalogPgJdbcTemplate;

    /**
     * Тест проверяет, что счётчик LogBasketCounter подсчитывает количество тасок по обработке логов и корректно их
     * биллит. А именно, подсчитываются все таски в статусе TASK_LIST_ACCEPTED или TASK_LIST_ACCEPTED_WITHOUT_CHECK
     * следующих типов: TYPE_TO_BASKET, TYPE_CANNOT_IMPROVE и TYPE_CANNOT_IMPROVE_TO_MODIFCATION.
     * Все остальные должны проигнорироваться этим счётчиком.
     */
    @Test
    public void testSimpleBilling() {
        TtTaskInitializer.create(siteCatalogPgJdbcTemplate);
        // Создадим множество таск листов с тасочкой под каждым из них. Первые шесть должны обиллиться, остальные нет.
        createTaskInTaskList(ACCEPTED_TO_BASKET,            Status.TASK_LIST_ACCEPTED,               UnsuccessfulLogType.TYPE_TO_BASKET);
        createTaskInTaskList(ACCEPTED_CANT_IMPROVE,         Status.TASK_LIST_ACCEPTED,               UnsuccessfulLogType.TYPE_CANNOT_IMPROVE);
        createTaskInTaskList(ACCEPTED_CANT_IMPROVE_MODIF,   Status.TASK_LIST_ACCEPTED,               UnsuccessfulLogType.TYPE_CANNOT_IMPROVE_TO_MODIFCATION);
        createTaskInTaskList(NO_CHECK_TO_BASKET,            Status.TASK_LIST_ACCEPTED_WITHOUT_CHECK, UnsuccessfulLogType.TYPE_TO_BASKET);
        createTaskInTaskList(NO_CHECK_CANT_IMPROVE,         Status.TASK_LIST_ACCEPTED_WITHOUT_CHECK, UnsuccessfulLogType.TYPE_CANNOT_IMPROVE);
        createTaskInTaskList(NO_CHECK_CANT_IMPROVE_MODIF,   Status.TASK_LIST_ACCEPTED_WITHOUT_CHECK, UnsuccessfulLogType.TYPE_CANNOT_IMPROVE_TO_MODIFCATION);
        createTaskInTaskList(NOT_ACCEPTED,                  Status.TASK_LIST_DELETED,                UnsuccessfulLogType.TYPE_TO_BASKET);
        createTaskInTaskList(ACCEPTED_OTHER_KIND,           Status.TASK_LIST_ACCEPTED,               UnsuccessfulLogType.TYPE_SUSPENDED);

        // Обиллим
        counter.doLoad(INTERVAL, tarifProvider);
        counter.getOperationsUpdater().flush();

        // По-честному сходим в базу и проверим, что же он туда положил в результате обилливания.
        BillingResultsGatherer resultsGatherer = new BillingResultsGatherer();
        siteCatalogJdbcTemplate.query("select * from ng_paid_operation_log", rch -> {
            int operationId = rch.getInt("operation_id");
            resultsGatherer.collectBillingOperation(operationId);
        });

        // По логике работы счётчика обиллиться должны только таски/офферы трёх различных типов действий:
        // TYPE_TO_BASKET, TYPE_CANNOT_IMPROVE, TYPE_CANNOT_IMPROVE_TO_MODIFCATION.
        assertEquals(3, resultsGatherer.getNumberOfDistinctActionTypes());

        // При этом в исходных данных выше каждый тип встречается два раза. Побиллиться должны в таком же количестве:
        assertEquals(2, resultsGatherer.getNumberOfBasketActions());
        assertEquals(2, resultsGatherer.getNumberOfNotImprovableActions());
        assertEquals(2, resultsGatherer.getNumberOfUnrecognizableActions());
    }

    private void createTaskInTaskList(String offerId, Status status, long type) {
        long taskListId = logTaskManager.createTaskList(CATEGORY_ID, Collections.singletonList(offerId));
        assertTrue(taskListId >= 0);

        List<Task> tasks = taskTracker.getTasks(taskListId);
        assertEquals(1, tasks.size());

        TaskList taskList = taskTracker.getTaskList(taskListId);
        assertNotNull(taskList);

        Task task = tasks.stream().findFirst().get();
        logTaskManager.changeOffersType(
            Collections.singletonList(offerId),
            taskList,
            task.getId(),
            type,
            CATEGORY_ID,
            false,
            autoUser.getId()
        );
        statusManager.changeTaskListStatus(autoUser.getId(), taskListId, status);
        dummyLogProcess(offerId, task.getId(), type);
    }

    /**
     * Имитируем работу NotProcessedLogProcessor по генерации данных в таблице с необработанными логами.
     */
    private void dummyLogProcess(String offerId, long taskId, long type) {
        int createdRows = siteCatalogJdbcTemplate.update("INSERT INTO TT_LOG (TASK_ID, OFFER_ID, LOG_NUM, " +
            "FEED_ID, DOC_ID, OFFER, DATASOURCE, PRICE, TYPE, CATEGORY_ID, GURU_CATEGORY_ID, VENDOR_ID, OFFER_HASH, " +
            "SHOP_ID, HIGHLIGHTED_OFFER, UPDATE_TIME, DATE_OF_SYNCHRONIZATION) " +
            "VALUES (?, ?, '', 0, 0, '', '', 1, ?, ?, ?, 0, '', 0, '', sysdate, sysdate)",
            taskId, offerId, type, CATEGORY_ID, CATEGORY_ID);
        assertTrue(createdRows > 0);
    }

    /**
     * Структурка для сбора результатов работы биллинга из БД, удобная для использования в ассерциях.
     */
    private static class BillingResultsGatherer {
        private Map<Integer, Integer> paidActionCounts = new HashMap<>();

        BillingResultsGatherer() {
            paidActionCounts.put(PaidAction.TO_BASKET.getId(), 0);
            paidActionCounts.put(PaidAction.TO_UNRECOGNIZABLE.getId(), 0);
            paidActionCounts.put(PaidAction.TO_NOTHING_TO_IMPROOVE.getId(), 0);
        }

        void collectBillingOperation(int operationId) {
            int countBefore = paidActionCounts.get(operationId);
            paidActionCounts.put(operationId, countBefore + 1);
        }

        int getNumberOfDistinctActionTypes() {
            return paidActionCounts.size();
        }

        int getNumberOfBasketActions() {
            return paidActionCounts.get(PaidAction.TO_BASKET.getId());
        }

        int getNumberOfUnrecognizableActions() {
            return paidActionCounts.get(PaidAction.TO_UNRECOGNIZABLE.getId());
        }

        int getNumberOfNotImprovableActions() {
            return paidActionCounts.get(PaidAction.TO_NOTHING_TO_IMPROOVE.getId());
        }
    }
}
