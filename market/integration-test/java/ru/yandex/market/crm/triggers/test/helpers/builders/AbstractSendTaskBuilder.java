package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractServiceTaskBuilder;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;

/**
 * @author apershukov
 */
abstract class AbstractSendTaskBuilder<T extends AbstractSendTaskBuilder<T>> extends AbstractServiceTaskBuilder<T>
    implements CrmAbstractFlowBuilderMixin {

    AbstractSendTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element, Class<?> selfType) {
        super(modelInstance, element, selfType);
    }

    public T templateId(String templateId) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.TEMPLATE_ID,
                templateId
        );
        return myself;
    }

    @Override
    public BpmnModelInstance getModelInstance() {
        return modelInstance;
    }

    @Override
    public <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
        return super.createTarget(typeClass, identifier);
    }

    public T globalControlEnabled(boolean enabled) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.GLOBAL_CONTROL_ENABLED,
                String.valueOf(enabled)
        );
        return myself;
    }

    public T setCommunicationId(String expression) {

        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.UID_EXPRESSION,
                expression
        );

        return myself;
    }
}
