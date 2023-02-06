package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractStartEventBuilder;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.StartEvent;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;

public class CrmStartEventBuilder extends AbstractStartEventBuilder<CrmStartEventBuilder> implements CrmAbstractFlowBuilderMixin {

    CrmStartEventBuilder(BpmnModelInstance modelInstance, StartEvent element) {
        super(modelInstance, element, CrmStartEventBuilder.class);
    }

    @Override
    public <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
        return super.createTarget(typeClass, identifier);
    }

    @Override
    public BpmnModelInstance getModelInstance() {
        return modelInstance;
    }

    public CrmStartEventBuilder segment(String segmentId) {
        if (null != segmentId) {
            CustomAttributesHelper.setAttribute(
                    element,
                    CustomAttributesNames.SEGMENT_ID,
                    segmentId
            );
        }
        return this;
    }
}
