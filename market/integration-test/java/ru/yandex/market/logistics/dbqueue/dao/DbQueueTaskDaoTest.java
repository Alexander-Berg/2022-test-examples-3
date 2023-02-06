package ru.yandex.market.logistics.dbqueue.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.logistics.dbqueue.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.domain.DbQueueTask;
import ru.yandex.market.logistics.dbqueue.domain.condition.Comparison;
import ru.yandex.market.logistics.dbqueue.domain.condition.SimpleSqlCondition;

@DatabaseSetup("/tasks.xml")
public class DbQueueTaskDaoTest extends AbstractContextualTest {
    @Autowired
    private DbQueueTaskDao dao;

    @Test
    public void testListAll() {
        Page<DbQueueTask> found = dao.list(Pageable.unpaged(), Collections.emptyList());
        softly.assertThat(found.getTotalElements()).isEqualTo(2L);
        softly.assertThat(found.getContent().size()).isEqualTo(2);
        softly.assertThat(found.getContent())
            .extracting(this::toCompare)
            .containsExactlyInAnyOrder(
                Arrays.asList("test.queue.1", "{\"a\":1, \"b\":2}", 2L, 0L, 2L),
                Arrays.asList("test.queue.2", "{\"a\":2}", 3L, 0L, 3L)
            );
    }

    @Test
    public void testListFiltered() {
        Page<DbQueueTask> found = dao.list(
            Pageable.unpaged(),
            Collections.singletonList(
                new SimpleSqlCondition<>("queue_name", Comparison.EQ, "test.queue.2")
            )
        );
        assertSingleElementFound(found, 1L, "test.queue.2");
    }

    @Test
    public void testListPage1() {
        Page<DbQueueTask> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("queue_name"))),
            Collections.emptyList()
        );
        assertSingleElementFound(found, 2L, "test.queue.1");
    }

    @Test
    public void testListPage2() {
        Page<DbQueueTask> found = dao.list(
            PageRequest.of(1, 1, Sort.by(Sort.Order.asc("queue_name"))),
            Collections.emptyList()
        );
        assertSingleElementFound(found, 2L, "test.queue.2");
    }

    @Test
    public void testListPageWithCriteria() {
        Page<DbQueueTask> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("queue_name"))),
            Collections.singletonList(
                new SimpleSqlCondition<>("queue_name", Comparison.EQ, "test.queue.2")
            )
        );
        assertSingleElementFound(found, 1L, "test.queue.2");
    }

    @Test
    public void testListPageWithCompletedFiltering() {
        Page<DbQueueTask> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("queue_name"))),
            Collections.singletonList(
                new SimpleSqlCondition<>("retry_completed", Comparison.EQ, true)
            )
        );
        assertSingleElementFound(found, 1L, "test.queue.1");
    }

    @Test
    public void testListPageWithNotCompletedFiltering() {
        Page<DbQueueTask> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("queue_name"))),
            Collections.singletonList(
                new SimpleSqlCondition<>("retry_completed", Comparison.EQ, false)
            )
        );
        assertSingleElementFound(found, 1L, "test.queue.2");
    }

    @Test
    public void testGetById() {
        Optional<DbQueueTask> found = dao.getById(1L);
        softly.assertThat(found)
            .map(this::toCompare)
            .contains(Arrays.asList("test.queue.1", "{\"a\":1, \"b\":2}", 2L, 0L, 2L));
    }

    @Test
    public void testGetByIdEmpty() {
        Optional<DbQueueTask> found = dao.getById(3L);
        softly.assertThat(found).isEmpty();
    }

    private void assertSingleElementFound(Page<DbQueueTask> found, long totalElements, String queueName) {
        softly.assertThat(found.getTotalElements()).isEqualTo(totalElements);
        softly.assertThat(found.getContent())
            .extracting(DbQueueTask::getQueueName)
            .containsExactly(queueName);
    }

    private List<Object> toCompare(DbQueueTask task) {
        return Arrays.asList(
            task.getQueueName(),
            task.getPayload(),
            task.getAttempt(),
            task.getReenqueueAttempt(),
            task.getTotalAttempt()
        );
    }
}
