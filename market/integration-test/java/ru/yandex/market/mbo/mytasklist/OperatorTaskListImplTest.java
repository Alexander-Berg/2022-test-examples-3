package ru.yandex.market.mbo.mytasklist;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.mbo.configs.TestConfiguration;
import ru.yandex.market.mbo.mytasklist.dto.OperatorTask;
import ru.yandex.market.mbo.mytasklist.dto.OperatorTaskFilter;
import ru.yandex.market.mbo.mytasklist.impl.OperatorTaskListServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.common.util.db.SortingOrder.DESC;
import static ru.yandex.market.mbo.tt.model.TaskType.ROBOT_PAGE_EXTRACTION_LEARNING;
import static ru.yandex.market.mbo.tt.status.Status.TASK_LIST_IN_PROCESS;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConfiguration.class})
@SuppressWarnings("checkstyle:MagicNumber")
public class OperatorTaskListImplTest {

    public static final long OPERATOR_ID = 127_528_153L;

    @Autowired
    private OperatorTaskListServiceImpl operatorTaskListService;

    @Test
    public void countTest() {
        final int countOperatorTask = operatorTaskListService.getCountOperatorTask(new OperatorTaskFilter(OPERATOR_ID));
        assertTrue(countOperatorTask > 0);
    }

    @Test
    public void testWithoutFilters() {
        final List<OperatorTask> operatorTasks =
            operatorTaskListService.getOperatorTasks(new OperatorTaskFilter(OPERATOR_ID), OPERATOR_ID);
        assertTrue(operatorTasks.size() > 0);
    }

    @Ignore
    @Test
    public void testWithAllFilters() {
        final List<OperatorTask> operatorTasks =
            operatorTaskListService.getOperatorTasks(new OperatorTaskFilter(OPERATOR_ID)
                .setTaskType(ROBOT_PAGE_EXTRACTION_LEARNING)
                .setStatus(TASK_LIST_IN_PROCESS)
                .setCategoryHid(12438151L)
                .setStatusDate(LocalDate.of(2017, 1, 22))
                .setLimit(5)
                .setOffset(1), OPERATOR_ID);
        assertEquals(5, operatorTasks.size());
    }

    @Test
    public void testOrdering() {
        Arrays.stream(OperatorTaskFilter.OrderedColumn.values())
            .forEach(orderedColumn -> operatorTaskListService.getOperatorTasks(new OperatorTaskFilter(OPERATOR_ID)
                .setOrderedColumn(orderedColumn)
                .setOrder(DESC)
                .setLimit(5), OPERATOR_ID));

    }

}
