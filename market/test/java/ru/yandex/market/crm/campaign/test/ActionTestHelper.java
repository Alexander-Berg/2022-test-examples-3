package ru.yandex.market.crm.campaign.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Supplier;

import ru.yandex.market.crm.campaign.domain.actions.ActionConfig;
import ru.yandex.market.crm.campaign.domain.actions.ActionVariant;
import ru.yandex.market.crm.campaign.domain.actions.status.StepStatus;
import ru.yandex.market.crm.campaign.domain.actions.steps.ActionStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.FoldByCryptaStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.IssueCoinsStep;
import ru.yandex.market.crm.campaign.domain.actions.steps.SendEmailsStep;
import ru.yandex.market.crm.campaign.domain.segment.TargetAudience;
import ru.yandex.market.crm.campaign.services.actions.preview.DefaultPreviewDataCustomizer;
import ru.yandex.market.crm.campaign.services.actions.strategies.StepStrategy;
import ru.yandex.market.crm.campaign.services.tasks.templates.PipelineTask;
import ru.yandex.market.crm.core.domain.segment.LinkingMode;

import static org.mockito.Mockito.mock;


/**
 * @author apershukov
 */
public final class ActionTestHelper {

    public static class StrategyMock<T extends ActionStep, C extends StepStatus<C>>
            extends StepStrategy<T, C> {

        public StrategyMock(Supplier<C> statusSupplier) {
            super(
                    mock(PipelineTask.class),
                    statusSupplier,
                    DefaultPreviewDataCustomizer.getInstance()
            );
        }
    }

    public static FoldByCryptaStep foldByCryptaStep() {
        FoldByCryptaStep step = new FoldByCryptaStep();
        step.setId("fold_by_crypta_" + UUID.randomUUID());
        return step;
    }

    public static SendEmailsStep sendEmailsStep() {
        return sendEmailsStep("send_email_step_" + UUID.randomUUID().toString());
    }

    public static SendEmailsStep sendEmailsStep(String stepId) {
        SendEmailsStep step = new SendEmailsStep();
        step.setId(stepId);
        return step;
    }

    public static IssueCoinsStep coinStep(String id, long promoId) {
        IssueCoinsStep step = new IssueCoinsStep();
        step.setId(id);
        step.setPromoId(promoId);
        return step;
    }

    public static IssueCoinsStep coinStep(String stepId) {
        return coinStep(stepId, 111);
    }

    public static IssueCoinsStep coinStep() {
        return coinStep(UUID.randomUUID().toString());
    }

    public static ActionVariant variant(String id, ActionStep... steps) {
        ActionVariant variant = new ActionVariant();
        variant.setId(id);
        variant.setPercent(50);
        variant.setSteps(Arrays.asList(steps));
        return variant;
    }

    public static ActionConfig config(ActionStep... steps) {
        ActionConfig config = new ActionConfig();
        config.setTarget(new TargetAudience(LinkingMode.ALL, "segment_id"));
        config.setVariants(Collections.singletonList(
                variant("variant_a", steps))
        );
        return config;
    }
}
