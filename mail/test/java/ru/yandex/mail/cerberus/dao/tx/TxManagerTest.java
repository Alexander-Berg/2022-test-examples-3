package ru.yandex.mail.cerberus.dao.tx;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.test.annotation.MicronautTest;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.TransientDataAccessException;
import ru.yandex.mail.cerberus.asyncdb.Repository;
import ru.yandex.mail.cerberus.dao.DaoRepositoriesFactory;
import ru.yandex.mail.cerberus.dao.DataSourceExecutorFactory;
import ru.yandex.mail.pglocal.junit_jupiter.InitDb;
import ru.yandex.mail.pglocal.junit_jupiter.PgLocalExtension;

import javax.inject.Inject;
import javax.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.yandex.mail.cerberus.dao.Constants.DB_NAME_PROPERTY;
import static ru.yandex.mail.cerberus.dao.Constants.MIGRATIONS;
import static ru.yandex.mail.cerberus.dao.DaoConstants.MASTER;
import static ru.yandex.mail.cerberus.dao.tx.TxManagerTest.DB_NAME;

interface TestRepository extends Repository {
    default String threadName() {
        return Thread.currentThread().getName();
    }
}

@ExtendWith(PgLocalExtension.class)
@InitDb(migration = MIGRATIONS, name = DB_NAME)
@MicronautTest(transactional = false)
@Property(name = DB_NAME_PROPERTY, value = DB_NAME)
class TxManagerTest {
    static final String DB_NAME = "tx_manager_db";

    private static final int RETRIES_COUNT = 3;

    @Value
    private static class ThreadNames {
        String queryThread;
        String continuationThread;
    }

    @Factory
    @Replaces(factory = DataSourceExecutorFactory.class)
    public static class DataSourceExecutorFactoryStub {
        @Singleton
        @EachBean(DatasourceConfiguration.class)
        public ExecutorService dataSourceExecutor(DatasourceConfiguration configuration) {
            return Executors.newFixedThreadPool(2, runnable -> new Thread(runnable, "db-thread"));
        }
    }

    @Inject
    private BeanLocator beanLocator;

    @Inject
    private DaoRepositoriesFactory repositoriesFactory;

    @Test
    @SneakyThrows
    @DisplayName("Verify that database request is executing within dedicated thread pool")
    void threadingTest() {
        val workingExecutor = Executors.newFixedThreadPool(2, runnable -> new Thread(runnable, "worker-thread"));

        val txManager = new DatasourceTxManager(MASTER, beanLocator, workingExecutor);
        val repo = repositoriesFactory.createRepository(TestRepository.class);

        val threadNames = txManager.executeAsync(repo::threadName)
            .thenApplyAsync(name -> new ThreadNames(name, Thread.currentThread().getName()))
            .get();

        assertThat(threadNames.queryThread).isEqualTo("db-thread");
        assertThat(threadNames.continuationThread).isEqualTo("worker-thread");
    }

    private static final class RetryableDatabaseErrorException extends TransientDataAccessException {
        RetryableDatabaseErrorException() {
            super("");
        }
    }

    @Test
    @DisplayName("Verify that database request will be retried on retryable error")
    void retryTest() {
        val executionCounter = new AtomicInteger(0);
        val txManager = beanLocator.getBean(TxManager.class, Qualifiers.byName(MASTER));

        assertThatThrownBy(() -> {
            txManager.execute(() -> {
                executionCounter.incrementAndGet();
                throw new RetryableDatabaseErrorException();
            });
        }).isInstanceOf(RetryableDatabaseErrorException.class);

        assertThat(executionCounter)
            .hasValue(1 + RETRIES_COUNT);
    }
}
