package ru.yandex.market.crm.triggers.test.helpers.builders;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractServiceTaskBuilder;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

/**
 * @author apershukov
 */
public class InjectorTaskBuilder extends AbstractServiceTaskBuilder<InjectorTaskBuilder> {

    InjectorTaskBuilder(BpmnModelInstance modelInstance, ServiceTask element, String color) {
        super(modelInstance, element, InjectorTaskBuilder.class);
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.CUSTOM_TYPE,
                CustomElementTypes.ENRICH_MESSAGE_CONTEXT
        );
        CustomAttributesHelper.setAttribute(
                element,
                CustomAttributesNames.COLOR,
                color
        );
    }

    public InjectorTaskBuilder injector(String code) {
        camundaDelegateExpression(String.format("${%s}", code));
        return myself;
    }
}
