package ru.yandex.travel.orders;

import com.google.protobuf.Message;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.travel.orders.workflow.order.proto.TStartReservation;
import ru.yandex.travel.workflow.entities.WorkflowEvent;
import ru.yandex.travel.workflow.repository.WorkflowEventRepository;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@ActiveProfiles("test")
//@TestExecutionListeners(listeners = TruncateDatabaseTestExecutionListener.class, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class ProtobufFieldMappingTest {

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private WorkflowEventRepository workflowEventRepository;

    @Test
    public void testProtobufFieldCorrectlySavedAndRestored() {
        Message savedMessage = TStartReservation.newBuilder().build();

        Long eventId = transactionTemplate.execute((tStatus) -> {
                    WorkflowEvent newWorkflowEvent = WorkflowEvent.createEventFor(
                            null,
                            savedMessage
                    );
                    newWorkflowEvent = workflowEventRepository.saveAndFlush(newWorkflowEvent);
                    return newWorkflowEvent.getId();
                }
        );

        Object restoredMessage = transactionTemplate.execute(
                (tStatus) -> workflowEventRepository.findById(eventId).get().getData()
        );
        assertThat(restoredMessage.getClass()).isEqualTo(TStartReservation.class);
        assertThat(restoredMessage).isEqualTo(savedMessage);
    }
}
