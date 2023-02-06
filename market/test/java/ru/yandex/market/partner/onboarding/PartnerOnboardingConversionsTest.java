package ru.yandex.market.partner.onboarding;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.wizard.model.WizardStepType;

public class PartnerOnboardingConversionsTest {

    @Test
    void testFromModelToProto() {
        for (WizardStepType stepType : WizardStepType.values()) {
            Assertions.assertDoesNotThrow(() -> PartnerOnboardingConversions.convert(stepType));
        }
    }
}
