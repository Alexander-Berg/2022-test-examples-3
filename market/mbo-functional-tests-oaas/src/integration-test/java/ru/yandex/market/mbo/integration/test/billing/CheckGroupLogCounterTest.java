package ru.yandex.market.mbo.integration.test.billing;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.tt.CheckGroupLogCounter;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.integration.test.tt.TtTaskInitializer;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.owner.OwnerManager;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import static org.junit.Assert.assertEquals;

/**
 * Тест проверяет корректность обиливание заданий про проверке группового лога.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class CheckGroupLogCounterTest extends BaseIntegrationTest {

    private static final long CATEGORY_ID = 5007938L;
    private static final long CHECKER_UID = 9876513L;
    private static final double PRICE = 4.0;
    private static final Calendar FROM_DATE = Calendar.getInstance();
    private static final Calendar TO_DATE = Calendar.getInstance();
    private static final Pair<Calendar, Calendar> INTERVAL = new Pair<>(FROM_DATE, TO_DATE);

    static {
        FROM_DATE.add(Calendar.HOUR, -6);
        TO_DATE.add(Calendar.HOUR, 6);
    }

    @Autowired
    private CheckGroupLogCounter counter;

    @Autowired
    private TaskTracker taskTracker;

    @Autowired
    private TarifProvider tarifProvider;

    @Autowired
    private StatusManager statusManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private AutoUser autoUser;

    @Resource(name = "siteCatalogJdbcTemplate")
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "siteCatalogPgJdbcTemplate")
    private JdbcOperations siteCatalogPgJdbcTemplate;

    private long acceptedTaskListId;

    @Before
    public void setup() {
        TtTaskInitializer.create(siteCatalogPgJdbcTemplate);
        // Создадим два задания по обработке лога, 3 таски в каждом.
        Pair<Long, List<Long>> tasksInList1 =
            taskTracker.createTaskListWithTasks(TaskType.LOG, CATEGORY_ID, Arrays.asList(-1L, -2L, -3L));
        Pair<Long, List<Long>> tasksInList2 =
            taskTracker.createTaskListWithTasks(TaskType.LOG, CATEGORY_ID, Arrays.asList(1L, 2L, 3L));

        // Для удобства извлечём ID заданий и низлежащих тасок.
        acceptedTaskListId = tasksInList1.getFirst();
        long otherTaskListId = tasksInList2.getFirst();
        List<Long> logTaskIds1 = tasksInList1.getSecond();
        List<Long> logTaskIds2 = tasksInList2.getSecond();

        // Посетим два разных статуса таск-листам. Валидный только ACCEPTED. Второй обиллиться не должен.
        statusManager.forceChangeTaskListStatus(CHECKER_UID, acceptedTaskListId, Status.TASK_LIST_ACCEPTED);
        statusManager.forceChangeTaskListStatus(CHECKER_UID, otherTaskListId, Status.TASK_LIST_ACCEPTED_WITHOUT_CHECK);

        // А теперь создадим задание по проверке лога. Таски этого задания будут ссылать пользователя на проверку
        // вышесозданных заданий по обработке лога. Такая вот вложенность.
        taskTracker.createTaskList(TaskType.CHECK_GROUP_LOG, CATEGORY_ID,
            Arrays.asList(acceptedTaskListId, otherTaskListId));

        // Имитируем побиленность заданий по *обработке* логов. Не путать с *проверкой* логов, биллинг которой мы будем
        // проверять по-честному. Эта имитация необходима, ибо вместо фиксированного тарифа мы платим некоторую долю
        // от цены проверяемых заданий.
        imitateRegularLogPay(logTaskIds1);
        imitateRegularLogPay(logTaskIds2); // <- эти операции не будут учтены, ибо связаны с тасками в неверном статусе.
    }

    @Test
    public void testUserChecksThemselvesNotBilled() {
        // Пусть владельцем тасок по обработке логов будет тот же человек, что и проверяет выполнение этих задач.
        // В этом случае за "самопроверку" мы платить не будем.
        ownerManager.setOwner(CHECKER_UID, acceptedTaskListId, CHECKER_UID);

        // Биллим
        counter.doLoad(INTERVAL, tarifProvider);
        counter.getOperationsUpdater().flush();

        // Убеждаемся, что данный каунтер ничего не обиллил.
        String countPaidEntriesQuery = "select count(*) from ng_paid_operation_log where operation_id = " +
            PaidAction.CHECK_GROUP_LOG.getId();
        int count = siteCatalogJdbcTemplate.queryForObject(countPaidEntriesQuery, Integer.class);
        assertEquals(0, count);
    }

    @Test
    public void testGroupLogCheckBilled() {
        // Пусть теперь владельцем тасок по обработке логов будет некий рандомный автоюзер. Проверяющим же остаётся
        // тот же CHECKER_UID. Таким образом, исполнитель и проверяющий - разные люди. За проверку чужих тасок платим.
        ownerManager.setOwner(autoUser.getId(), acceptedTaskListId, autoUser.getId());

        // Биллим
        counter.doLoad(INTERVAL, tarifProvider);
        counter.getOperationsUpdater().flush();

        // Проверяем. Должны были побиллиться три таски стоимостью в 4.0 каждая. Так как мы умножаем на 0.5,
        // ожидаем получить три записи в биллинге со стоимостью 2.0 каждая.
        String paidEntriesQuery = "select * from ng_paid_operation_log where operation_id = " +
            PaidAction.CHECK_GROUP_LOG.getId();
        siteCatalogJdbcTemplate.query(paidEntriesQuery, rch -> {
            long uid = rch.getLong("user_id");
            double price = rch.getDouble("price");
            assertEquals(CHECKER_UID, uid);
            assertEquals(PRICE * 0.5, price, 1e-6);
        });
        String countPaidEntriesQuery = "select count(*) from ng_paid_operation_log where operation_id = " +
            PaidAction.CHECK_GROUP_LOG.getId();
        int count = siteCatalogJdbcTemplate.queryForObject(countPaidEntriesQuery, Integer.class);
        assertEquals(3, count);
    }

    private void imitateRegularLogPay(List<Long> taskIds) {
        String query = "INSERT INTO ng_paid_operation_log " +
            "(id, user_id, category_id, time, operation_id, price, source_id, count, audit_action_id) " +
            "VALUES (ng_paid_operation_log_id_seq.NEXTVAL, ?, ?, sysdate, 100500, ?, ?, ?, ?)";
        taskIds.forEach(taskId ->
            siteCatalogJdbcTemplate.update(query, autoUser.getId(), CATEGORY_ID, PRICE, taskId, 1, -1));
    }
}
