package ru.yandex.travel.orders.workflows;

import java.util.NoSuchElementException;
import java.util.UUID;

import com.google.protobuf.ProtocolMessageEnum;

import ru.yandex.travel.orders.entities.OrderItem;
import ru.yandex.travel.workflow.BasicMessagingContext;
import ru.yandex.travel.workflow.BasicStateMessagingContext;
import ru.yandex.travel.workflow.MessagingContext;
import ru.yandex.travel.workflow.entities.WorkflowEntity;

public class WorkflowTestUtils {
    public static <STATE extends ProtocolMessageEnum, ENTITY extends WorkflowEntity<STATE>>
    BasicStateMessagingContext<STATE, ENTITY> testMessagingContext(ENTITY entity) {
        return testMessagingContext(entity, 0);
    }

    public static <STATE extends ProtocolMessageEnum, ENTITY extends WorkflowEntity<STATE>>
    BasicStateMessagingContext<STATE, ENTITY> testMessagingContext(ENTITY entity, int attempt) {
        return BasicStateMessagingContext.fromMessagingContext(
                new BasicMessagingContext<>(UUID.randomUUID(), entity, attempt, 0));
    }

    public static <T> T getMessageFor(MessagingContext<?> context, OrderItem entity) {
        return getMessageFor(context, (WorkflowEntity<?>) entity);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getMessageFor(MessagingContext<?> context, WorkflowEntity<?> entity) {
        return context.getScheduledEvents().stream()
                .filter(p -> p.getRecipientWorkflowId().equals(entity.getWorkflow().getId()))
                .map(p -> (T) p.getMessage())
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "No message for " + entity.getEntityType() + ", id " + entity.getId()));
    }
}
