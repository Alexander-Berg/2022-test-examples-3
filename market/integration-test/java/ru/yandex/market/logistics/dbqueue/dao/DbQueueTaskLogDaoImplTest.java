package ru.yandex.market.logistics.dbqueue.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ru.yandex.market.logistics.dbqueue.AbstractContextualTest;
import ru.yandex.market.logistics.dbqueue.domain.DbQueueTaskLog;
import ru.yandex.market.logistics.dbqueue.domain.condition.Comparison;
import ru.yandex.market.logistics.dbqueue.domain.condition.SimpleSqlCondition;

@DatabaseSetup({
    "/tasks.xml",
    "/tasks_log.xml",
})
class DbQueueTaskLogDaoImplTest extends AbstractContextualTest {
    @Autowired
    private DbQueueTaskLogDaoImpl dao;

    @Test
    public void testListAll() {
        Page<DbQueueTaskLog> found = dao.list(Pageable.unpaged(), Collections.emptyList());
        softly.assertThat(found.getTotalElements()).isEqualTo(2L);
        softly.assertThat(found.getContent().size()).isEqualTo(2);
        softly.assertThat(found.getContent())
            .extracting(this::toCompare)
            .containsExactlyInAnyOrder(
                Arrays.asList(2L, "message.1", "abc"),
                Arrays.asList(2L, "message.2", "def")
            );
    }

    @Test
    public void testListFiltered() {
        Page<DbQueueTaskLog> found = dao.list(
            Pageable.unpaged(),
            Collections.singletonList(
                new SimpleSqlCondition<>("request_id", Comparison.EQ, "def")
            )
        );
        assertSingleElementFound(found, 1L, "message.2");
    }

    @Test
    public void testListPage1() {
        Page<DbQueueTaskLog> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("request_id"))),
            Collections.emptyList()
        );
        assertSingleElementFound(found, 2L, "message.1");
    }

    @Test
    public void testListPage2() {
        Page<DbQueueTaskLog> found = dao.list(
            PageRequest.of(1, 1, Sort.by(Sort.Order.asc("request_id"))),
            Collections.emptyList()
        );
        assertSingleElementFound(found, 2L, "message.2");
    }

    @Test
    public void testListPageWithCriteria() {
        Page<DbQueueTaskLog> found = dao.list(
            PageRequest.of(0, 1, Sort.by(Sort.Order.asc("request_id"))),
            Collections.singletonList(
                new SimpleSqlCondition<>("message", Comparison.EQ, "message.2")
            )
        );
        assertSingleElementFound(found, 1L, "message.2");
    }

    private void assertSingleElementFound(Page<DbQueueTaskLog> found, long totalElements, String message) {
        softly.assertThat(found.getTotalElements()).isEqualTo(totalElements);
        softly.assertThat(found.getContent())
            .extracting(DbQueueTaskLog::getMessage)
            .containsExactly(message);
    }

    private List<Object> toCompare(DbQueueTaskLog task) {
        return Arrays.asList(
            task.getTaskId(),
            task.getMessage(),
            task.getRequestId()
        );
    }
}
