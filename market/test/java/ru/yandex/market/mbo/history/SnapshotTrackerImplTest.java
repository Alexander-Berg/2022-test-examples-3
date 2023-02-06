package ru.yandex.market.mbo.history;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcOperations;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.mbo.history.model.Snapshot;
import ru.yandex.market.mbo.history.model.ValueType;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 21.11.2017
 */
@SuppressWarnings("checkstyle:magicNumber")
public class SnapshotTrackerImplTest {

    private SnapshotTrackerImpl tracker;

    private static final long EMPTY_SNAPSHOT_ID = 1L;
    private JdbcOperations jdbcOperations;

    @Before
    public void before() {
        MultiIdGenerator idGenerator = new MultiIdGenerator() {
            private final AtomicLong source = new AtomicLong(100);

            @Override
            public List<Long> getIds(int count) {
                return Stream.generate(this::getId)
                        .limit(count)
                        .collect(Collectors.toList());
            }

            @Override
            public long getId() {
                return source.getAndIncrement();
            }
        };

        jdbcOperations = mock(JdbcOperations.class);
        tracker = new SnapshotTrackerImpl(jdbcOperations, idGenerator);
    }

    @Test
    public void saveEmpty() throws Exception {
        assertThat(tracker.save(snapshot()), is(EMPTY_SNAPSHOT_ID));
    }

    @Test
    public void saveNonEmpty() throws Exception {
        assertThat(tracker.save(snapshot("a", "b")), is(100L));
        assertThat(tracker.save(snapshot("b", "c")), is(101L));
    }

    @Test
    public void saveBatch() throws Exception {
        assertThat(tracker.save(Arrays.asList(
                    snapshot("aaa", "bbb"),
                    snapshot("bbb", "ccc"),
                    snapshot("ccc", "ddd")
                )),
                is(Arrays.asList(100L, 101L, 102L))
        );
    }

    @Test
    public void saveError() throws Exception {
        when(jdbcOperations.batchUpdate(anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class)))
                .thenThrow(mock(DataAccessException.class));

        assertThat(tracker.save(snapshot("a1", "b1")), is(0L));
    }

    @Test
    public void saveBatchError() throws Exception {
        when(jdbcOperations.batchUpdate(anyString(), ArgumentMatchers.any(BatchPreparedStatementSetter.class)))
                .thenThrow(mock(DataAccessException.class));

        assertThat(tracker.save(Arrays.asList(snapshot("a1", "b1"), snapshot("a2", "b2"))), is(Arrays.asList(0L, 0L)));
    }

    @Test
    public void saveBatchWithEmpty() throws Exception {
        assertThat(tracker.save(Arrays.asList(
                    snapshot("aaa", "bbb"),
                    snapshot(),
                    snapshot("ccc", "ddd")
                )),
                is(Arrays.asList(100L, EMPTY_SNAPSHOT_ID, 101L))
        );
    }

    @Test
    public void saveBatchMainlyEmpty() throws Exception {
        assertThat(tracker.save(Arrays.asList(
                snapshot(),
                snapshot(),
                snapshot(),
                snapshot("aaa", "bbb"),
                snapshot(),
                snapshot(),
                snapshot("ccc", "ddd"),
                snapshot(),
                snapshot(),
                snapshot(),
                snapshot()
                )),
                is(Arrays.asList(
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID,
                        100L,
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID,
                        101L,
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID,
                        EMPTY_SNAPSHOT_ID
                        )
                )
        );
    }

    private static Snapshot snapshot(String... keyValue) {
        assertThat(keyValue.length % 2, is(0));

        Snapshot snapshot = new Snapshot();
        for (int i = 0; i < keyValue.length; i += 2) {
            snapshot.put(keyValue[i], ValueType.STRING, keyValue[i + 1]);
        }
        return snapshot;
    }

}
