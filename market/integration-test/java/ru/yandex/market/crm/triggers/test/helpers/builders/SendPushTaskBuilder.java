package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

/**
 * @author apershukov
 */
public class SendPushTaskBuilder extends AbstractSendTaskBuilder<SendPushTaskBuilder> {

    public SendPushTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element) {
        super(modelInstance, element, SendPushTaskBuilder.class);
        camundaDelegateExpression("${sendPushTrigger}");
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.CUSTOM_TYPE,
                CustomElementTypes.PUSH
        );
    }

    public SendPushTaskBuilder useSameDeviceOnly() {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.USE_SAME_DEVICE_ONLY,
                String.valueOf(true)
        );
        return this;
    }

    public SendPushTaskBuilder setSubscriptionType(long subscriptionType) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.SUBSCRIPTION_TYPE,
                String.valueOf(subscriptionType)
        );
        return myself;
    }

    public SendPushTaskBuilder setTimeToLive(String value) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.PUSH_TIME_TO_LIVE_LIMIT,
                value
        );
        return myself;
    }

    public SendPushTaskBuilder setTimeToLiveOnDevice(String value) {
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.PUSH_TIME_TO_LIVE_ON_DEVICE_LIMIT,
                value
        );
        return myself;
    }
}
