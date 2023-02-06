package ru.yandex.travel.orders;

import com.google.common.base.Preconditions;
import com.google.protobuf.ProtocolMessageEnum;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;

import ru.yandex.travel.workflow.StateContext;
import ru.yandex.travel.workflow.WorkflowMessagePair;
import ru.yandex.travel.workflow.entities.WorkflowEntity;

public class StateContextTestHelpers {
    public static <S extends ProtocolMessageEnum, E extends WorkflowEntity<S>>
    void assertStateContextContainsScheduledMessageOfType(StateContext<S, E> ctx, Class messageType,
                                                          int numberOfMessages) {
        Preconditions.checkArgument(numberOfMessages >= 0, "Number of messages can't be negative");
        Preconditions.checkNotNull(messageType, "Message type must be provided");
        Preconditions.checkNotNull(ctx, "Valid context must be provided");
        Condition<WorkflowMessagePair> condition = new Condition<WorkflowMessagePair>() {
            @Override
            public boolean matches(WorkflowMessagePair value) {
                return value.getMessage().getClass() == messageType;
            }
        };
        Assertions.assertThat(ctx.getScheduledEvents()).filteredOn(condition).hasSize(numberOfMessages);
    }
}
