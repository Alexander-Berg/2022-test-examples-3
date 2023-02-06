package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractIntermediateCatchEventBuilder;
import org.camunda.bpm.model.bpmn.instance.IntermediateCatchEvent;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;

/**
 * @author apershukov
 */
public class IntermediateEventBuilder extends AbstractIntermediateCatchEventBuilder<IntermediateEventBuilder> {

    IntermediateEventBuilder(BpmnModelInstance modelInstance, IntermediateCatchEvent element, String messageType) {
        super(modelInstance, element, IntermediateEventBuilder.class);
        message(messageType);
    }

    public IntermediateEventBuilder attribute(String name, String value) {
        CustomAttributesHelper.setAttribute(element, name, value);
        return this;
    }
}
