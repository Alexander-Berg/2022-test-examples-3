package ru.yandex.market.core.partner.onboarding.listeners;

import java.time.Instant;
import java.util.List;

import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;

final class TestUtils {
    static final Instant SOME_TIME = Instant.now();

    static PartnerOnboardingState makeOnboardingState(
            long supplierId,
            Status stepStatus,
            boolean areCurrentlyAllStepsPassed) {
        return createTestDropshipState(
                PartnerId.supplierId(supplierId),
                SOME_TIME,
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepStatus.newBuilder()
                                        .withStatus(stepStatus)
                                        .withStep(WizardStepType.MARKETPLACE)
                                        .build(),
                                SOME_TIME
                        )
                ),
                areCurrentlyAllStepsPassed);
    }
}
