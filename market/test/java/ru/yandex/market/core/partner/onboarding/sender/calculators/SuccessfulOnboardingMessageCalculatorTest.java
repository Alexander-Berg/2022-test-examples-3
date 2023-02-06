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
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_MARKET_URL;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestCrossdockState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestFulfillmentState;

class SuccessfulOnboardingMessageCalculatorTest {
    private SuccessfulOnboardingMailingCalculator mailing = new SuccessfulOnboardingMailingCalculator(PARTNER_MARKET_URL);

    @Test
    void testMailingIsSentOnCrossdockSuccess() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestCrossdockState(
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

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(SuccessfulOnboardingMailingCalculator.FBY_PLUS_TEMPLATE_ID);
    }

    @Test
    void testMailingIsSentOnFulfillmentSuccess() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestFulfillmentState(
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

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(SuccessfulOnboardingMailingCalculator.FBY_TEMPLATE_ID);
    }

    /**
     * Для дропшипов письмо отправляется в другом месте, поэтому здесь отправляться не должно
     *
     * @see 1600000010.xsl
     */
    @Test
    void testMailingIsNotSentOnDropshipSuccess() {
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

    @Test
    void testNoMailingOnAvailableSteps() {
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

        assertThat(info.needSend()).isFalse();
    }

}
