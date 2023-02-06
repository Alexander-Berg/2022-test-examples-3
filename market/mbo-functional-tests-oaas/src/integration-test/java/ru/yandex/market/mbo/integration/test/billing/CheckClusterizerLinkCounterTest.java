package ru.yandex.market.mbo.integration.test.billing;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.billing.counter.tt.CheckClusterizerLinkCounter;
import ru.yandex.market.mbo.billing.tarif.TarifProvider;
import ru.yandex.market.mbo.gwt.models.gurulight.OfferData;
import ru.yandex.market.mbo.gwt.models.tt.OffersPair;
import ru.yandex.market.mbo.integration.test.BaseIntegrationTest;
import ru.yandex.market.mbo.integration.test.tt.TtTaskInitializer;
import ru.yandex.market.mbo.tt.TaskTracker;
import ru.yandex.market.mbo.tt.legacy.ClusterizerLinkService;
import ru.yandex.market.mbo.tt.model.TaskType;
import ru.yandex.market.mbo.tt.owner.OwnerManager;
import ru.yandex.market.mbo.tt.status.Status;
import ru.yandex.market.mbo.tt.status.StatusManager;
import ru.yandex.market.mbo.user.AutoUser;

import static org.apache.commons.lang3.RandomUtils.nextLong;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("checkstyle:MagicNumber")
public class CheckClusterizerLinkCounterTest extends BaseIntegrationTest {
    private static final long CATEGORY_ID = 5007938L;
    private static final Calendar FROM_DATE = Calendar.getInstance();
    private static final Calendar TO_DATE = Calendar.getInstance();
    private static final Pair<Calendar, Calendar> INTERVAL = new Pair<>(FROM_DATE, TO_DATE);

    static {
        FROM_DATE.add(Calendar.HOUR, -6);
        TO_DATE.add(Calendar.HOUR, 6);
    }

    @Autowired
    private CheckClusterizerLinkCounter counter;

    @Autowired
    private TaskTracker taskTracker;

    @Autowired
    private TarifProvider tarifProvider;

    @Autowired
    private StatusManager statusManager;

    @Autowired
    private OwnerManager ownerManager;

    @Autowired
    private ClusterizerLinkService linkService;

    @Autowired
    private AutoUser autoUser;

    @Resource(name = "siteCatalogJdbcTemplate")
    private JdbcTemplate siteCatalogJdbcTemplate;

    @Resource(name = "siteCatalogPgJdbcTemplate")
    private JdbcOperations siteCatalogPgJdbcTemplate;

    @Before
    public void setup() {
        TtTaskInitializer.create(siteCatalogPgJdbcTemplate);
        // Создадим три таск-листа. Два имеют допустимый тип, один - нет. В каждом таск-листе по две задачки.
        List<Long> fakeContent = Arrays.asList(1L, 2L);
        Pair<Long, List<Long>> simpleTaskList =
            taskTracker.createTaskListWithTasks(TaskType.CLUSTERIZER_LINK, CATEGORY_ID, fakeContent);
        Pair<Long, List<Long>> assessmentTaskList =
            taskTracker.createTaskListWithTasks(TaskType.CLUSTERIZER_LINK_ASSESSMENT, CATEGORY_ID, fakeContent);
        Pair<Long, List<Long>> wrongTaskList =
            taskTracker.createTaskListWithTasks(TaskType.CLASSIFY_LINK, CATEGORY_ID, fakeContent); // Недопустимый тип.

        // Для удобства извлечём ID заданий (то бишь таск-листов, это одно и то же).
        long simpleTaskListId = simpleTaskList.getFirst();
        long assessmentTaskListId = assessmentTaskList.getFirst();
        long wrongTaskListId = wrongTaskList.getFirst();

        // Посетим различные статусы заданиям. Допустим только TASK_LIST_CLOSED, остальные не обилливаются.
        statusManager.forceChangeTaskListStatus(autoUser.getId(), simpleTaskListId, Status.TASK_LIST_CLOSED);
        statusManager.forceChangeTaskListStatus(autoUser.getId(), assessmentTaskListId, Status.TASK_LIST_ACCEPTED);
        statusManager.forceChangeTaskListStatus(autoUser.getId(), wrongTaskListId, Status.TASK_LIST_CLOSED);
        // Итого непосредственно в биллинг попадёт только первый таск-лист. У второго плохой статус, у третьего - тип.

        // Посетим владельца тасок, чтобы на его имя зачислялись деньги.
        ownerManager.setOwner(autoUser.getId(), simpleTaskListId, autoUser.getId());

        // Создадим линки.
        List<Long> taskIds = simpleTaskList.getSecond();
        taskIds.forEach(taskId -> {
            linkService.createLinks(Arrays.asList(
                generateRandomOffersPair(),
                generateRandomOffersPair()),
                taskId);
        });
        // Итого по две линки на таску. То есть всего 4 линки. Далее некоторая странность: при каждом обилливании мы
        // от таски берём ИД её задания (листа). И по ИД листа считаем все линки. Линки считаются по всем таскам
        // задания. Внимательный читатель должен заметить, что в текущем тесте мы по такой логике два раза пройдёмся
        // по набору из четырёх линок, что в итоге даст восемь биллинговых записей. Странно, но жалоб не поступало.
    }

    @Test
    public void testClustLinksBilling() {
        // Биллим
        counter.doLoad(INTERVAL, tarifProvider);
        counter.getOperationsUpdater().flush();

        String paidEntriesQuery = "select * from ng_paid_operation_log where operation_id = " +
            PaidAction.CHECK_CLUSTERIZER_LINK.getId();
        siteCatalogJdbcTemplate.query(paidEntriesQuery, rch -> {
            long uid = rch.getLong("user_id");
            assertEquals(autoUser.getId(), uid);
        });
        String countPaidEntriesQuery = "select count(*) from ng_paid_operation_log where operation_id = " +
            PaidAction.CHECK_CLUSTERIZER_LINK.getId();
        int count = siteCatalogJdbcTemplate.queryForObject(countPaidEntriesQuery, Integer.class);
        assertEquals(8, count);
    }

    private OffersPair generateRandomOffersPair() {
        OfferData data1 = new OfferData();
        data1.setOfferId(UUID.randomUUID().toString().substring(0, 32));
        OfferData data2 = new OfferData();
        data2.setOfferId(UUID.randomUUID().toString().substring(0, 32));

        OffersPair pair = new OffersPair();
        pair.setHid(CATEGORY_ID);
        pair.setFirstOffer(data1);
        pair.setSecondOffer(data2);
        pair.setType(OffersPair.Type.GOOD);
        pair.setFirstClusterId(nextLong());
        pair.setSecondClusterId(nextLong());
        return pair;
    }
}
