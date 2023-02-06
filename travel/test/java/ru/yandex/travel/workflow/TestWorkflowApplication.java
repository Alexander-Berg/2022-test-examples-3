package ru.yandex.travel.workflow;

import java.time.Clock;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

import ru.yandex.bolts.collection.Option;
import ru.yandex.travel.workflow.repository.TestEntityRepository;

@SpringBootApplication
public class TestWorkflowApplication {
    @Autowired
    private TestEntityRepository testEntityRepository;

    @Bean
    public MessagingContextFactory messagingContextFactory() {
        return (workflow, attempt) -> {
            if ("TEST_ENTITY".equals(workflow.getEntityType())) {
                return Option.of(
                        new BasicMessagingContext<>(workflow.getId(), testEntityRepository.getOne(workflow.getEntityId()), attempt, workflow.getWorkflowVersion())
                );
            }
            throw new RuntimeException("Unknown workflow entity type");
        };
    }

    @Bean
    public WorkflowEventRetryStrategy workflowEventRetryStrategy() {
        return new WorkflowEventRetryStrategy() {
            @Override
            public Duration getWaitDuration(int iteration, Exception ex) {
                return Duration.ofMillis(1000);
            }

            @Override
            public boolean shouldRetry(int iteration, Exception ex) {
                return iteration < 3;
            }
        };
    }

    @Bean
    public WorkflowEventHandlerMatcher workflowEventHandlerMatcher() {
        return (workflow, event) -> Option.empty();
    }

    @Bean
    @ConditionalOnMissingBean
    public Clock cLock() {
        return Clock.systemUTC();
    }

}
