package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

/**
 * @author apershukov
 */
public class SendEmailTaskBuilder extends AbstractSendTaskBuilder<SendEmailTaskBuilder> {

    public SendEmailTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element) {
        super(modelInstance, element, SendEmailTaskBuilder.class);
        camundaDelegateExpression("${sendEmailTrigger}");
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.CUSTOM_TYPE,
                CustomElementTypes.EMAIL
        );
    }

    public SendEmailTaskBuilder useDefaultEmail() {

        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.USE_BLACKBOX_EMAIL,
                String.valueOf(true)
        );

        return this;
    }
}
