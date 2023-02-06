package ru.yandex.market.core.partner.onboarding.sender.calculators;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.partner.onboarding.sender.MailingInfo;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;

class FailedOnboardingMessageCalculatorTest {
    private FailedOnboardingMailingCalculator mailing = new FailedOnboardingMailingCalculator();

    @Test
    void testMailingSentIfUserHasAvailableSteps() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(20L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(20L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now
                        )
                )
        );

        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(FailedOnboardingMailingCalculator.TEMPLATE_ID);
    }

    @Test
    void testMailNotSentOnCompletedOnboarding() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(20L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(20L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FULL,
                                now
                        )
                )
        );

        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isFalse();
    }
}
