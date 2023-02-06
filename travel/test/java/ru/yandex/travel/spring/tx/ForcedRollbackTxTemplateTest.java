package ru.yandex.travel.spring.tx;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import ru.yandex.travel.spring.tx.entities.TestEntity;
import ru.yandex.travel.spring.tx.repositories.TestEntityRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@Slf4j
public class ForcedRollbackTxTemplateTest {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Autowired
    private PersistenceExceptionTranslator exceptionTranslator;

    private final ExecutorService executorService =
            Executors.newFixedThreadPool(2, new ThreadFactoryBuilder().setDaemon(true).build());

    @Test
    public void testTransactionRolledBack() {
        ForcedRollbackTxManagerWrapper wrapper = new ForcedRollbackTxManagerWrapper(
                transactionManager, exceptionTranslator);
        ForcedRollbackTxTemplate forcedRollbackTxTemplate = new ForcedRollbackTxTemplate(
                wrapper, new DefaultTransactionDefinition());
        UUID rolledBackId = UUID.randomUUID();
        forcedRollbackTxTemplate.execute(ignored -> {
            TestEntity testEntity = new TestEntity();
            testEntity.setId(rolledBackId);
            testEntityRepository.save(testEntity);
            return null;
        });

        forcedRollbackTxTemplate.execute(ignored -> {
            Optional<TestEntity> maybeResult = testEntityRepository.findById(rolledBackId);
            assertThat(maybeResult.isEmpty()).isTrue();
            return null;
        });
    }

    @Test
    public void testTransactionCommitted() {
        ForcedRollbackTxManagerWrapper wrapper = new ForcedRollbackTxManagerWrapper(
                transactionManager, exceptionTranslator);
        ForcedRollbackTxTemplate forcedRollbackTxTemplate = new ForcedRollbackTxTemplate(
                wrapper, new DefaultTransactionDefinition());
        wrapper.resumeCommits();
        UUID committedId = UUID.randomUUID();
        forcedRollbackTxTemplate.execute(ignored -> {
            TestEntity testEntity = new TestEntity();
            testEntity.setId(committedId);
            testEntityRepository.save(testEntity);
            return null;
        });

        forcedRollbackTxTemplate.execute(ignored -> {
            Optional<TestEntity> committedResult = testEntityRepository.findById(committedId);
            assertThat(committedResult.isPresent()).isTrue();
            return null;
        });
    }

    @Test
    public void testMultipleThreadsCommitAndRollback() throws InterruptedException {
        ForcedRollbackTxManagerWrapper wrapper = new ForcedRollbackTxManagerWrapper(
                transactionManager, exceptionTranslator);
        ForcedRollbackTxTemplate forcedRollbackTxTemplate = new ForcedRollbackTxTemplate(
                wrapper, new DefaultTransactionDefinition());
        wrapper.resumeCommits();

        UUID rolledBackId = UUID.randomUUID();
        UUID committedId = UUID.randomUUID();

        CountDownLatch transactionInProgress = new CountDownLatch(1);
        CountDownLatch proceedLatch = new CountDownLatch(1);
        CountDownLatch transactionsFinishedLatch = new CountDownLatch(2);


        executorService.submit(() -> {
            forcedRollbackTxTemplate.execute(ignored -> {
                transactionInProgress.countDown();
                TestEntity testEntity = new TestEntity();
                testEntity.setId(rolledBackId);
                testEntityRepository.save(testEntity);
                try {
                    proceedLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return null;
            });
            transactionsFinishedLatch.countDown();
        });

        transactionInProgress.await();
        wrapper.pauseCommits();

        wrapper.resumeCommits();

        executorService.submit(() -> {
            forcedRollbackTxTemplate.execute(ignored -> {
                TestEntity testEntity = new TestEntity();
                testEntity.setId(committedId);
                testEntityRepository.save(testEntity);
                try {
                    proceedLatch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
                return null;
            });
            transactionsFinishedLatch.countDown();
        });

        proceedLatch.countDown();
        transactionsFinishedLatch.await();


        forcedRollbackTxTemplate.execute(ignored -> {
            Optional<TestEntity> committedResult = testEntityRepository.findById(committedId);
            assertThat(committedResult.isPresent()).isTrue();

            Optional<TestEntity> rolledBackResult = testEntityRepository.findById(rolledBackId);
            assertThat(rolledBackResult.isPresent()).isFalse();
            return null;
        });
    }

    @Test
    public void testExceptionTranslation() {
        ForcedRollbackTxManagerWrapper wrapper = new ForcedRollbackTxManagerWrapper(
                transactionManager, new HibernateJpaDialect());
        ForcedRollbackTxTemplate forcedRollbackTxTemplate = new ForcedRollbackTxTemplate(
                wrapper, new DefaultTransactionDefinition());
        wrapper.resumeCommits();

        assertThatThrownBy(() -> forcedRollbackTxTemplate.execute(ignored -> {
            // e.g. locketEntity.getSomething
            throw new org.hibernate.PessimisticLockException("could not extract ResultSet: " +
                    "Caused by: org.postgresql.util.PSQLException: " +
                    "ERROR: canceling statement due to lock timeout", null, null);
        })).isExactlyInstanceOf(org.springframework.dao.PessimisticLockingFailureException.class)
                .hasMessageContaining("canceling statement due to lock timeout");
    }
}
