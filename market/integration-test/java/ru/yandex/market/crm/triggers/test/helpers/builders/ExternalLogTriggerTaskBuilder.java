package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractServiceTaskBuilder;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

public class ExternalLogTriggerTaskBuilder extends AbstractServiceTaskBuilder<ExternalLogTriggerTaskBuilder>
        implements CrmAbstractFlowBuilderMixin {
    public ExternalLogTriggerTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element) {
        super(modelInstance, element, ExternalLogTriggerTaskBuilder.class);
        camundaDelegateExpression("${externalLogTrigger}");
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.CUSTOM_TYPE,
                CustomElementTypes.TRIGGER_LOGGING
        );
    }

    public ExternalLogTriggerTaskBuilder setLogProcessVariables(String processVariables) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.PROCESS_VARIABLES,
                processVariables
        );
        return this;
    }

    public ExternalLogTriggerTaskBuilder setLogEventType(String logEventType) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.LOG_EVENT_TYPE,
                logEventType
        );
        return this;
    }

    @Override
    public BpmnModelInstance getModelInstance() {
        return modelInstance;
    }

    @Override
    public <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
        return super.createTarget(typeClass, identifier);
    }
}
