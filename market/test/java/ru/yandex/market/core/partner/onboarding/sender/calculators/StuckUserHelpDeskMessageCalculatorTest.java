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


class StuckUserHelpDeskMessageCalculatorTest {
    private StuckUserHelpDeskMailingCalculator mailing = new StuckUserHelpDeskMailingCalculator();

    @Test
    void testMailingSentIfUserHasAvailableSteps() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(8L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.EMPTY,
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
        assertThat(info.getTemplateId()).isEqualTo(StuckUserHelpDeskMailingCalculator.TEMPLATE_ID);

        //По заявлению не информируем, т.к. оно на нашей проверке. Доступны каталог и настройка обработки заказов
        NamedContainer availableSteps = getContainerByName(info.getNotificationData(), "available-steps");
        assertTemplateContainsSections(availableSteps, List.of("ASSORTMENT", "ORDER_PROCESSING"));
    }
}
