package ru.yandex.market.logshatter.config.ddl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.clickhouse.ddl.ClickHouseTableDefinitionImpl;
import ru.yandex.market.clickhouse.ddl.DDL;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateShardDDLResult;
import ru.yandex.market.logshatter.config.ddl.shard.UpdateShardDDLTask;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 01.11.16
 */
public class UpdateDDLTaskExecutorTest {
    private static final int MIN_TIME_BETWEEN_RETRIES_MILLIS = 1;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void oneSuccessfulTaskPerShard_ShouldReturnImmediately() {
        UpdateDDLTaskExecutor sut = new UpdateDDLTaskExecutor((int) TimeUnit.MINUTES.toMillis(1), 10);
        sut.commitTask(task(success()));
        sut.commitTask(task(success()));

        sut.onResult(result -> {
            assertThat(result, instanceOf(UpdateDDLTaskExecutorResult.Success.class));
        });

        sut.allTasksCommitted();
    }

    @Test
    public void oneFailingTask_ShouldRetryUntilSuccess() {
        UpdateDDLTaskExecutor sut = new UpdateDDLTaskExecutor((int) TimeUnit.MINUTES.toMillis(1), 10);
        sut.setTimeBetweenRetriesMillis(MIN_TIME_BETWEEN_RETRIES_MILLIS);
        sut.commitTask(task(success()));

        UpdateShardDDLTask failingTask = Mockito.spy(
            changingTask(
                // shard number
                failure(),
                failure(),
                success()
            )
        );

        sut.commitTask(failingTask);

        Iterator<Class<? extends UpdateDDLTaskExecutorResult>> expectedResultIterator = Stream.of(
            UpdateDDLTaskExecutorResult.Failure.class,
            UpdateDDLTaskExecutorResult.Failure.class,
            UpdateDDLTaskExecutorResult.Success.class
        ).iterator();

        sut.onResult(result -> {
            assertThat(result, instanceOf(expectedResultIterator.next()));
        });

        sut.allTasksCommitted();

        Mockito.verify(failingTask, Mockito.times(3)).run();
    }

    @Test
    public void failingTaskOnShardWithAliveNodes_ShouldReportPartialSuccessThenSuccess() {
        UpdateDDLTaskExecutor sut = new UpdateDDLTaskExecutor((int) TimeUnit.MINUTES.toMillis(1), 10);
        sut.setTimeBetweenRetriesMillis(MIN_TIME_BETWEEN_RETRIES_MILLIS);

        sut.commitTask(
            task(
                // shard number
                success()
            )
        );

        UpdateShardDDLTask failingTask = Mockito.spy(
            changingTask(
                // shard number
                partialSuccess(),
                success()
            )
        );

        sut.commitTask(failingTask);

        List<Class<? extends UpdateDDLTaskExecutorResult>> expectedResults = Arrays.asList(
            UpdateDDLTaskExecutorResult.PartialSuccess.class,
            UpdateDDLTaskExecutorResult.Success.class
        );

        Iterator<Class<? extends UpdateDDLTaskExecutorResult>> expectedResultIterator = expectedResults.iterator();

        sut.onResult(result -> {
            assertThat(result, instanceOf(expectedResultIterator.next()));
        });

        sut.allTasksCommitted();
    }

    @Test
    public void manualDDLRequired_ShouldRetryUntilSuccess() {
        UpdateDDLTaskExecutor sut = new UpdateDDLTaskExecutor((int) TimeUnit.MINUTES.toMillis(1), 10);
        sut.setTimeBetweenRetriesMillis(MIN_TIME_BETWEEN_RETRIES_MILLIS);

        DDL ddl = new DDL(
            "host",
            new ClickHouseTableDefinitionImpl("test", "test", Collections.emptyList(), null)
        ) {
            @Override
            public boolean equals(Object obj) {
                return this == obj;
            }

            @Override
            public int hashCode() {
                return System.identityHashCode(this);
            }
        };

        sut.commitTask(
            changingTask(
                partialSuccess(),
                manualDDLRequired(ddl),
                manualDDLRequired(ddl),
                manualDDLRequired(ddl),
                success()
            )
        );

        List<? extends UpdateDDLTaskExecutorResult> expectedResults = Arrays.asList(
            new UpdateDDLTaskExecutorResult.PartialSuccess(),
            new UpdateDDLTaskExecutorResult.ManualDDLRequired(Collections.singletonList(ddl)),
            new UpdateDDLTaskExecutorResult.ManualDDLRequired(Collections.singletonList(ddl)),
            new UpdateDDLTaskExecutorResult.ManualDDLRequired(Collections.singletonList(ddl)),
            new UpdateDDLTaskExecutorResult.Success()
        );

        Iterator<? extends UpdateDDLTaskExecutorResult> expectedResultIterator = expectedResults.iterator();

        sut.onResult(result -> {
            assertEquals(result, expectedResultIterator.next());
        });

        sut.allTasksCommitted();
    }

    private UpdateShardDDLResultBuilder.Success success() {
        return new UpdateShardDDLResultBuilder.Success();
    }

    private UpdateShardDDLResultBuilder.ManualDDLRequired manualDDLRequired(DDL ddl) {
        return new UpdateShardDDLResultBuilder.ManualDDLRequired()
            .withManualDDLs(Arrays.asList(ddl));
    }

    private UpdateShardDDLResultBuilder.PartialSuccess partialSuccess() {
        return new UpdateShardDDLResultBuilder.PartialSuccess()
            .withExceptions(Arrays.asList(updateDDLException()));
    }

    private UpdateShardDDLResultBuilder failure() {
        return new UpdateShardDDLResultBuilder.Failure()
            .withExceptions(Arrays.asList(updateDDLException()));
    }

    private UpdateShardDDLTask changingTask(UpdateShardDDLResultBuilder... results) {
        Iterator<UpdateShardDDLResultBuilder> iterator = Arrays.asList(results).iterator();

        return new UpdateShardDDLTask() {
            @Override
            public UpdateShardDDLResult run() {
                UpdateShardDDLResultBuilder result = iterator.next();
                result.withTask(this);
                return result.build();
            }
        };
    }

    private UpdateDDLException updateDDLException() {
        return new UpdateDDLException(new Exception("fake error, never mind"), "host");
    }

    private UpdateShardDDLTask task(UpdateShardDDLResultBuilder result) {
        UpdateShardDDLTask task = result::build;
        result.withTask(task);
        return task;
    }

}
