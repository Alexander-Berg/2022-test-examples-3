package ru.yandex.travel.workflow.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.travel.test.fake.proto.TTestSomeEvent;
import ru.yandex.travel.workflow.EWorkflowState;
import ru.yandex.travel.workflow.entities.TestEntity;
import ru.yandex.travel.workflow.entities.Workflow;
import ru.yandex.travel.workflow.entities.WorkflowEvent;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@ActiveProfiles("test")
public class WorkflowRepositoryTest {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowEventRepository workflowEventRepository;

    @Autowired
    private TestEntityRepository testEntityRepository;

    @Autowired
    private EntityManager em;


    @Test
    public void testFindingSupervisedWorkflows() {
        Workflow parent = createWorkflowWithDummyTestEntity(null);

        Workflow child1 = createWorkflowWithDummyTestEntity(parent.getId());
        Workflow child2 = createWorkflowWithDummyTestEntity(parent.getId());

        Workflow descendant11 = createWorkflowWithDummyTestEntity(child1.getId());
        Workflow descendant21 = createWorkflowWithDummyTestEntity(child2.getId());

        // random workflow
        createWorkflowWithDummyTestEntity(null);

        em.flush(); // flushing all the changes

        List<UUID> rawIds = workflowRepository.findSupervisedRunningWorkflows(parent.getId());
        assertThat(rawIds).contains(child1.getId(), child2.getId(), descendant11.getId(), descendant21.getId());
    }

    @Test
    public void testPausingWorkflows() {
        Workflow parent = createWorkflowWithDummyTestEntity(null);

        Workflow child1 = createWorkflowWithDummyTestEntity(parent.getId());
        Workflow child2 = createWorkflowWithDummyTestEntity(parent.getId());

        workflowRepository.batchCompareAndSetWorkflowState(
                List.of(child1.getId(), child2.getId()),
                EWorkflowState.WS_RUNNING, EWorkflowState.WS_PAUSED);
        em.flush();

        em.clear();

        assertThat(workflowRepository.getOne(child1.getId()).getState()).isEqualTo(EWorkflowState.WS_PAUSED);
        assertThat(workflowRepository.getOne(child2.getId()).getState()).isEqualTo(EWorkflowState.WS_PAUSED);
    }


    @Test
    public void testFindWorkflowsToBeProcessedWithPool() {
        UUID workflowId1 = createWorkflowWithDummyTestEntityAndPoolId(1);
        UUID workflowId2 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId3 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId4 = createWorkflowWithDummyTestEntityAndPoolId(null);
        UUID workflowId5 = createWorkflowWithDummyTestEntityAndPoolId(3);
        createWorkflowWithDummyTestEntityAndPoolId(1);

        for (var workflowId : List.of(workflowId1, workflowId2, workflowId3, workflowId4, workflowId5)) {
            WorkflowEvent event = WorkflowEvent.createEventFor(workflowId, TTestSomeEvent.getDefaultInstance());
            workflowEventRepository.saveAndFlush(event);
        }

        assertThat(Set.copyOf(workflowRepository.findWorkflowsToBeScheduled(Set.of(), 1, 10)))
                .isEqualTo(Set.of(workflowId1));
        assertThat(Set.copyOf(workflowRepository.findWorkflowsToBeScheduled(Set.of(), 2, 10)))
                .isEqualTo(Set.of(workflowId2, workflowId3));

        em.createNativeQuery("update workflows set processing_pool_id = null where processing_pool_id = 3")
                .executeUpdate();

        Map<Integer, Integer> pendingCounters = workflowRepository.countWorkflowsToBeScheduledPerPoolId(Set.of());
        assertThat(pendingCounters).hasSize(4);
        assertThat(pendingCounters.get(null)).isEqualTo(1);
        assertThat(pendingCounters.get(-1)).isEqualTo(1);
        assertThat(pendingCounters.get(1)).isEqualTo(1);
        assertThat(pendingCounters.get(2)).isEqualTo(2);
    }

    @Test
    public void testFindWorkflowsToBeProcessedByPool() {
        UUID workflowId1 = createWorkflowWithDummyTestEntityAndPoolId(1);
        UUID workflowId2 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId3 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId4 = createWorkflowWithDummyTestEntityAndPoolId(null);
        UUID workflowId5 = createWorkflowWithDummyTestEntityAndPoolId(3);
        createWorkflowWithDummyTestEntityAndPoolId(1);

        for (var workflowId : List.of(workflowId1, workflowId2, workflowId3, workflowId4, workflowId5)) {
            WorkflowEvent event = WorkflowEvent.createEventFor(workflowId, TTestSomeEvent.getDefaultInstance());
            workflowEventRepository.saveAndFlush(event);
        }

        Map<Integer, Integer> poolLimits = Map.of(
                1, 10,
                2, 10,
                3, 10
        );

        Map<UUID, Integer> r = workflowRepository.findWorkflowsToBeScheduledByPools(Set.of(), 1, poolLimits);

        assertThat(r.get(workflowId1)).isEqualTo(1);
        assertThat(r.get(workflowId4)).isEqualTo(1);
        assertThat(r.get(workflowId2)).isEqualTo(2);
        assertThat(r.get(workflowId3)).isEqualTo(2);
    }

    @Test
    public void testFindWorkflowsToBeProcessedWithExcludedPools() {
        UUID workflowId1 = createWorkflowWithDummyTestEntityAndPoolId(1);
        UUID workflowId2 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId3 = createWorkflowWithDummyTestEntityAndPoolId(2);
        UUID workflowId4 = createWorkflowWithDummyTestEntityAndPoolId(null);
        createWorkflowWithDummyTestEntityAndPoolId(1);

        for (var workflowId : List.of(workflowId1, workflowId2, workflowId3, workflowId4)) {
            WorkflowEvent event = WorkflowEvent.createEventFor(workflowId, TTestSomeEvent.getDefaultInstance());
            workflowEventRepository.saveAndFlush(event);
        }

        assertThat(Set.copyOf(workflowRepository.findWorkflowsToBeScheduled(Set.of(), Set.of(1), 10)))
                .isEqualTo(Set.of(workflowId2, workflowId3, workflowId4));
        assertThat(Set.copyOf(workflowRepository.findWorkflowsToBeScheduled(Set.of(), Set.of(2), 10)))
                .isEqualTo(Set.of(workflowId1, workflowId4));
        assertThat(Set.copyOf(workflowRepository.findWorkflowsToBeScheduled(Set.of(), Set.of(1, 2), 10)))
                .isEqualTo(Set.of(workflowId4));
    }

    @Test
    public void automaticProcessingPoolIds() {
        Workflow w1 = createWorkflowWithDummyTestEntity(null, null);
        assertThat(w1.getProcessingPoolId()).isEqualTo(-1);

        Workflow w2 = createWorkflowWithDummyTestEntity(null, "another_entity");
        assertThat(w2.getProcessingPoolId()).isEqualTo(1);
    }

    private Workflow createWorkflowWithDummyTestEntity(UUID supervisorId) {
        return createWorkflowWithDummyTestEntity(supervisorId, null);
    }

    private Workflow createWorkflowWithDummyTestEntity(UUID supervisorId, String workflowEntityType) {
        TestEntity testEntity = new TestEntity();
        testEntity.setId(UUID.randomUUID());
        testEntity = testEntityRepository.save(testEntity);
        Workflow result = Workflow.createWorkflowForEntity(testEntity, supervisorId);
        if (workflowEntityType != null) {
            result.setEntityType(workflowEntityType);
        }
        return workflowRepository.save(result);
    }

    private UUID createWorkflowWithDummyTestEntityAndPoolId(Integer processingPoolId) {
        TestEntity testEntity = new TestEntity();
        testEntity.setId(UUID.randomUUID());
        testEntity = testEntityRepository.save(testEntity);

        Workflow workflow = Workflow.createWorkflowForEntity(testEntity);
        workflow.setProcessingPoolId(processingPoolId);
        workflowRepository.saveAndFlush(workflow);
        return workflow.getId();
    }

    @TestConfiguration
    static class CustomizedWorkflowRepositoryTestConfig {
        @Bean
        public WorkflowRepositoryProcessingPoolsConfig processingPoolsConfig() {
            Map<String, Integer> poolIdsMapping = Map.of("another_entity", 1);
            return new WorkflowRepositoryProcessingPoolsConfig(-1, poolIdsMapping);
        }
    }
}
