package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

public class SendSmsTaskBuilder extends AbstractSendTaskBuilder<SendSmsTaskBuilder> {
    public SendSmsTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element) {
        super(modelInstance, element, SendSmsTaskBuilder.class);
        camundaDelegateExpression("${sendSmsTrigger}");
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.CUSTOM_TYPE,
                CustomElementTypes.SMS
        );
    }

    public SendSmsTaskBuilder notShortenText() {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.NOT_SHORTEN_TEXT,
                String.valueOf(true)
        );
        return this;
    }
}
