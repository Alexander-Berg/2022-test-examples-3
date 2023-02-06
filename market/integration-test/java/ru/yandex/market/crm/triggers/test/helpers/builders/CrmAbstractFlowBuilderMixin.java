package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

/**
 * Интерфейс предназначен для реализации model builder'ами элементов триггера (bpm процесса),
 * которые представляют собой flow элементы диаграммы
 * (наследники {@link org.camunda.bpm.model.bpmn.builder.AbstractFlowNodeBuilder},
 * вызовом методов которых можно создать следующий по пути выполнения элемент)
 *
 * Вариант с реализацией множественного наследования позволяет добавлять в нужные билдеры специфику триггерной платформы
 * Для простого добавления её к существующим элементам необходимо переопределять класс билдера и реализовать интерфейс
 */
public interface CrmAbstractFlowBuilderMixin {

    <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier);

    BpmnModelInstance getModelInstance();

    default SendPushTaskBuilder sendPushTask(String id) {
        return new SendPushTaskBuilder(
                getModelInstance(),
                createTarget(ServiceTask.class, id)
        );
    }

    default SendEmailTaskBuilder sendEmailTask(String id) {
        return new SendEmailTaskBuilder(
                getModelInstance(),
                createTarget(ServiceTask.class, id)
        );
    }

    default SendSmsTaskBuilder sendSmsTask(String id) {
        return new SendSmsTaskBuilder(
                getModelInstance(),
                createTarget(ServiceTask.class, id)
        );
    }

    default ExternalLogTriggerTaskBuilder triggerExternalLogTask(String id) {
        return new ExternalLogTriggerTaskBuilder(
                getModelInstance(),
                createTarget(ServiceTask.class, id)
        );
    }

    default UserVariantDistributorBuilder userVariantDistributor(String id) {
        return new UserVariantDistributorBuilder(
                getModelInstance(),
                createTarget(ExclusiveGateway.class, id)
        );
    }

    default IntermediateEventBuilder intermediateEvent(String id, String eventType) {
        return new IntermediateEventBuilder(
                getModelInstance(),
                createTarget(IntermediateCatchEvent.class, id),
                eventType
        );
    }

    default InjectorTaskBuilder injectTask(String id, String color) {
        return new InjectorTaskBuilder(
                getModelInstance(),
                createTarget(ServiceTask.class, id),
                color);
    }
}
