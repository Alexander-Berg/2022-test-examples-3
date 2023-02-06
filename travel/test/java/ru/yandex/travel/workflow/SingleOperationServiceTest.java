package ru.yandex.travel.workflow;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.bolts.collection.Option;
import ru.yandex.travel.workflow.entities.SingleOperation;
import ru.yandex.travel.workflow.repository.SingleOperationRepository;
import ru.yandex.travel.workflow.single_operation.SingleOperationRunner;
import ru.yandex.travel.workflow.single_operation.SingleOperationRunnerProvider;
import ru.yandex.travel.workflow.single_operation.SingleOperationService;
import ru.yandex.travel.workflow.single_operation.SingleOperationWorkflowHandler;
import ru.yandex.travel.workflow.single_operation.proto.ESingleOperationState;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "single-node.auto-start=true",
                "single-operation.enabled=true",
                "single-operation.schedule.pool-size=2",
                "single-operation.schedule.initial-start-delay=5ms",
                "single-operation.schedule.schedule-rate=100ms"
        }
)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class SingleOperationServiceTest {

    @Autowired
    private SingleOperationService singleOperationService;

    @MockBean
    private SingleOperationRunnerProvider singleOperationRunnerProvider;

    @Autowired
    private SingleOperationWorkflowHandler singleOperationWorkflowHandler;

    @MockBean
    private WorkflowEventHandlerMatcher workflowEventHandlerMatcher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SingleOperationRepository singleOperationRepository;

    @Before
    public void setUp() {
        when(workflowEventHandlerMatcher.findEventHandlerFor(any(), any()))
                .thenReturn(Option.of(singleOperationWorkflowHandler));
    }

    @Data
    private static final class InputData {
        private String data;
    }

    @Data
    private static final class OutputData {
        private String data;
    }

    @Test
    public void testSingleOperationRun() throws InterruptedException {

        final InputData in = new InputData();
        in.setData("foo");


        SingleOperationRunner<InputData, OutputData> runner = new SingleOperationRunner<>() {
            @Override
            public Class<InputData> getInputClass() {
                return InputData.class;
            }

            @Override
            public OutputData runOperation(InputData payload) {
                Assertions.assertThat(payload.getData()).isEqualTo("foo");
                OutputData out = new OutputData();
                out.setData("bar");
                return out;
            }
        };

        when(singleOperationRunnerProvider.runnerForOperationType("operationThunderbolt")).thenReturn(runner);

        UUID operationUUID = transactionTemplate.execute(ignored ->
                singleOperationService.runOperation("operation", "operationThunderbolt", in)
        );

        await().atMost(2, TimeUnit.SECONDS).until(() ->
                transactionTemplate.execute(
                        ignored -> singleOperationRepository.getOne(operationUUID).getState() ==
                                ESingleOperationState.ERS_SUCCESS)
        );

        transactionTemplate.execute(ignored -> {
            SingleOperation singleOperation = singleOperationRepository.getOne(operationUUID);
            Assertions.assertThat(singleOperation.getOutput()).isNotNull();
            return null;
        });
    }

    @Test
    public void testSingleOperationSchedule() throws InterruptedException {

        final InputData in = new InputData();
        in.setData("foo");


        SingleOperationRunner<InputData, OutputData> runner = new SingleOperationRunner<>() {
            @Override
            public Class<InputData> getInputClass() {
                return InputData.class;
            }

            @Override
            public OutputData runOperation(InputData payload) {
                Assertions.assertThat(payload.getData()).isEqualTo("foo");
                OutputData out = new OutputData();
                out.setData("bar");
                return out;
            }
        };

        when(singleOperationRunnerProvider.runnerForOperationType("operationThunderbolt")).thenReturn(runner);

        UUID operationUUID = transactionTemplate.execute(ignored ->
                singleOperationService.scheduleOperation("operation", "operationThunderbolt", in, Instant.now().plusSeconds(2))
        );

        await().atMost(4000, TimeUnit.SECONDS).until(() ->
                transactionTemplate.execute(
                        ignored -> singleOperationRepository.getOne(operationUUID).getState() ==
                                ESingleOperationState.ERS_SUCCESS)
        );

        transactionTemplate.execute(ignored -> {
            SingleOperation singleOperation = singleOperationRepository.getOne(operationUUID);
            Assertions.assertThat(singleOperation.getOutput()).isNotNull();
            return null;
        });
    }
    @TestConfiguration
    public static class TestConfig {

        @Autowired
        private SingleOperationRepository singleOperationRepository;

        @Bean
        MessagingContextFactory messagingContextFactory() {
            return (workflow, attempt) -> {
                SingleOperation op = singleOperationRepository.getOne(workflow.getEntityId());
                return Option.of(new BasicMessagingContext<>(workflow.getId(), op, attempt,
                        workflow.getWorkflowVersion()));
            };
        }
    }

}
