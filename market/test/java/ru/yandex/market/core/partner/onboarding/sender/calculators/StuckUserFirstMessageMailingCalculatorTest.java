package ru.yandex.market.core.partner.onboarding.sender.calculators;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.partner.onboarding.sender.MailingInfo;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.assertTemplateContainsSections;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.getContainerByName;

class StuckUserFirstMessageMailingCalculatorTest {
    private StuckUserFirstMessageMailingCalculator mailing = new StuckUserFirstMessageMailingCalculator();

    @Test
    void testMailingSentIfUserHasAvailableSteps() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.COMMON_INFO,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now
                        )
                )
        );
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(StuckUserFirstMessageMailingCalculator.TEMPLATE_ID);

        NamedContainer availableSteps = getContainerByName(info.getNotificationData(), "available-steps");
        assertTemplateContainsSections(availableSteps, List.of("ASSORTMENT", "STOCK_UPDATE"));
    }

    @Test
    void testMailingNotSentIfThereIsNoText() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.COMMON_INFO,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FULL,
                                now
                        ),
                        //Шаг, для которого в данный момент не написан текст
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.CROSSBORDER_LEGAL,
                                Status.EMPTY,
                                now
                        )
                )
        );
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isFalse();
    }

    @Test
    void testMailingIsSentIfOneStepHasText() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.COMMON_INFO,
                                Status.FULL,
                                now
                        ),
                        //Для этого шага текст есть, поэтому письмо отправится все равно
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now
                        ),
                        //Шаг, для которого в данный момент не написан текст
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.MARKETPLACE,
                                Status.EMPTY,
                                now
                        )
                )
        );
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();

        NamedContainer availableSteps = getContainerByName(info.getNotificationData(), "available-steps");
        assertTemplateContainsSections(availableSteps, List.of("ASSORTMENT", "MARKETPLACE"));
    }


    /**
     * Если последний шаг завершился по нашей инициативе (напр. подтвердили заявление) - не шлем письмо, чтобы
     * не заваливать спамом
     */
    @Test
    void testMailNotSentAfterOurApprove() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now
                        )
                )
        );
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isFalse();
    }

    @Test
    void testNoMessageOnOurDelay() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(20L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(20L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FILLED,
                                now
                        )
                )
        );

        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isFalse();
    }

    /**
     * Для шагов самопроверки и настройки обработки есть что делать, даже если шаги сдвинулись с места
     */
    @Test
    void testFirstStepWithNotCompletedDeeds() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(2L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.FILLED,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ORDER_PROCESSING,
                                Status.FILLED,
                                now
                        )
                )
        );
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);

        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(StuckUserFirstMessageMailingCalculator.TEMPLATE_ID);

        NamedContainer availableSteps = getContainerByName(info.getNotificationData(), "available-steps");
        assertTemplateContainsSections(availableSteps, List.of("SELF_CHECK", "ORDER_PROCESSING"));
    }

}
