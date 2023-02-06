package ru.yandex.market.crm.triggers.test.helpers.builders;

import java.util.UUID;
import java.util.function.Function;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.builder.AbstractExclusiveGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.EndEventBuilder;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import ru.yandex.market.crm.core.services.bpm.CustomAttributesHelper;
import ru.yandex.market.crm.core.services.trigger.CustomAttributesNames;
import ru.yandex.market.crm.core.services.trigger.CustomElementTypes;

public class UserVariantDistributorBuilder extends AbstractExclusiveGatewayBuilder<UserVariantDistributorBuilder>
        implements CrmAbstractFlowBuilderMixin {
    public UserVariantDistributorBuilder(BpmnModelInstance modelInstance, ExclusiveGateway element) {
        super(modelInstance, element, UserVariantDistributorBuilder.class);
        camundaExecutionListenerDelegateExpression("start", "${userByVariantDistributor}");
    }

    @Override
    public BpmnModelInstance getModelInstance() {
        return modelInstance;
    }

    @Override
    public <T extends FlowNode> T createTarget(Class<T> typeClass, String identifier) {
        return super.createTarget(typeClass, identifier);
    }

    public UserVariantDistributorBuilder createVariantSequenceFlow(
            String variant,
            int percent,
            Function<UserVariantDistributorBuilder, EndEventBuilder> triggerBranchModifier
    ) {
        CustomAttributesHelper.setAttribute(element, CustomAttributesNames.CUSTOM_TYPE, CustomElementTypes.AB_GATEWAY);

        var expression = String.format("${abVariant == '%s'}", variant);
        var condition = this.condition(null, expression);
        var sfId = "SequenceFlow_" + UUID.randomUUID().toString();
        sequenceFlowId(sfId);

        triggerBranchModifier.apply(condition);

        this.element.getOutgoing().stream()
                .filter(sf -> sf.getId().equals(sfId))
                .findAny()
                .ifPresent(sf -> {
                    CustomAttributesHelper.setAttribute(sf, CustomAttributesNames.CUSTOM_TYPE, CustomElementTypes.AB_OUT_FLOW);
                    CustomAttributesHelper.setAttribute(sf, CustomAttributesNames.VARIANT, variant);
                    CustomAttributesHelper.setAttribute(sf, CustomAttributesNames.PERCENT, String.valueOf(percent));
                });

        return this;
    }
}
