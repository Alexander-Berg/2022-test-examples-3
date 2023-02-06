package ru.yandex.travel.workflow.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.MoreExecutors;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.misc.ExceptionUtils;
import ru.yandex.travel.workflow.entities.TestEntity;
import ru.yandex.travel.workflow.entities.Workflow;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
public class WorkflowRepositoryOptimisticLockingTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    public void testThatOptimisticLockIsEnforcedEvenOnRead() throws InterruptedException {
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch firstTxComplete = new CountDownLatch(1);
        CountDownLatch second = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        UUID workflowId = transactionTemplate.execute(ignored -> {
            Workflow workflow = createWorkflowWithDummyTestEntity();
            return workflow.getId();
        });
        CompletableFuture<UUID> firstResult =
                CompletableFuture.supplyAsync(() -> {
                    UUID wid = transactionTemplate.execute(ignored -> {
                        Optional<Workflow> workflow = workflowRepository.getWorkflowWithOptimisticLockForced(workflowId);
                        try {
                            first.await();
                        } catch (InterruptedException e) {
                            throw ExceptionUtils.throwException(e);
                        }
                        return workflow.get().getId();
                    });
                    firstTxComplete.countDown();
                    return wid;
                }, executor);
        CompletableFuture<UUID> secondResult =
                CompletableFuture.supplyAsync(() -> transactionTemplate.execute(ignored -> {
                    Optional<Workflow> workflow = workflowRepository.getWorkflowWithOptimisticLockForced(workflowId);
                    try {
                        second.await();
                    } catch (InterruptedException e) {
                        throw ExceptionUtils.throwException(e);
                    }
                    return workflow.get().getId();
                }), executor);

        first.countDown();
        firstTxComplete.await();
        second.countDown();

        Assertions.assertThatCode(firstResult::join).doesNotThrowAnyException();
        assertThat(firstResult.join()).isEqualTo(workflowId);

        Throwable throwable = Assertions.catchThrowable(secondResult::join);
        assertThat(throwable.getCause()).isInstanceOf(ConcurrencyFailureException.class);
        MoreExecutors.shutdownAndAwaitTermination(executor, 1, TimeUnit.SECONDS);
    }

    private Workflow createWorkflowWithDummyTestEntity() {
        TestEntity testEntity = new TestEntity();
        testEntity.setId(UUID.randomUUID());
        testEntity = testEntityRepository.save(testEntity);
        Workflow result = Workflow.createWorkflowForEntity(testEntity);
        return workflowRepository.save(result);
    }
}
