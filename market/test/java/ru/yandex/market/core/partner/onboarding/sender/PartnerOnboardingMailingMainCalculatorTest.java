package ru.yandex.market.core.partner.onboarding.sender;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.core.partner.PartnerLinkService;
import ru.yandex.market.core.partner.onboarding.sender.calculators.OnboardingMailingCalculator;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.xml.impl.NamedContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.getContainerByName;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.simpleStateDelayCalculator;

class PartnerOnboardingMailingMainCalculatorTest {
    private PartnerOnboardingMainMailingCalculator calculator;
    private PartnerLinkService partnerLinkServiceMock = mock(PartnerLinkService.class);

    private static final int FIRST_STEP_TEMPLATE = 1;
    private static final int SECOND_STEP_TEMPLATE = 2;
    private static final int THIRD_STEP_TEMPLATE = 3;
    private static final int SUCCESS_TEMPLATE = 10;
    private static final int FAILURE_TEMPLATE = 11;

    @BeforeEach
    void init() {
        PartnerOnboardingNotificationConfigurator configurator = new PartnerOnboardingNotificationConfigurator(List.of(
                new PartnerOnboardingNotificationStep(0, List.of(
                        dummySuccessMailing()
                )),
                new PartnerOnboardingNotificationStep(1, List.of(
                        dummyFirstStepMailing()
                )),
                new PartnerOnboardingNotificationStep(4, List.of(
                        dummySecondStepMailing()
                )),
                new PartnerOnboardingNotificationStep(7, List.of(
                        dummyThirdStepMailing()
                )),
                new PartnerOnboardingNotificationStep(27, List.of(
                        dummyFailureMailing()
                ))
        ));

        calculator = new PartnerOnboardingMainMailingCalculator(
                configurator,
                simpleStateDelayCalculator(),
                partnerLinkServiceMock
        );
    }

    @Test
    void testNoMessageForProgramChange() {
        when(partnerLinkServiceMock.getDonorPartnerId(PARTNER_ID)).thenReturn(1L);

        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(5))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.CROSSBORDER_LEGAL,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(5))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        )
                )
        );
        MailingInfo info = calculator.calculateMailingForPartnerState(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isFalse();
    }

    @Test
    void testFirstStep() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(5))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.CROSSBORDER_LEGAL,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(5))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(1))
                        )
                )
        );
        MailingInfo info = calculator.calculateMailingForPartnerState(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(FIRST_STEP_TEMPLATE);

    }

    @Test
    void testSecondStepWithCorrectCondition() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.FULL,
                                now.minus(Duration.ofDays(8L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.CROSSBORDER_LEGAL,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(4L))
                        )
                )
        );
        MailingInfo info = calculator.calculateMailingForPartnerState(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(SECOND_STEP_TEMPLATE);
        NamedContainer container = getContainerByName(info.getNotificationData(), "some-param");
        assertThat(container.getContent()).isEqualTo("some-value");
    }

    @Test
    void testSecondStepWithIncorrectCondition() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_FEED,
                                Status.FULL,
                                now.minus(Duration.ofDays(4L))
                        )
                )
        );
        MailingInfo info = calculator.calculateMailingForPartnerState(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isFalse();
    }

    @Test
    void testThirdStep() {
        var now = Instant.now();
        PartnerOnboardingState partnerIdWithOnboardingState = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(10L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.CROSSBORDER_LEGAL,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(8L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(7L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SELF_CHECK,
                                Status.EMPTY,
                                now.minus(Duration.ofDays(7L))
                        )
                )
        );
        MailingInfo info = calculator.calculateMailingForPartnerState(partnerIdWithOnboardingState);
        assertThat(info.needSend()).isTrue();
        assertThat(info.getTemplateId()).isEqualTo(THIRD_STEP_TEMPLATE);
    }

    private OnboardingMailingCalculator dummyFirstStepMailing() {
        return state -> MailingInfo.singleMessage(FIRST_STEP_TEMPLATE, List.of());
    }

    private OnboardingMailingCalculator dummySecondStepMailing() {
        return state -> {
            if (state.getLastFullStep().isPresent()) {
                if (state.getLastFullStep().get().getStepType() == WizardStepType.SELF_CHECK) {
                    return MailingInfo.singleMessage(SECOND_STEP_TEMPLATE, List.of(
                            new NamedContainer("some-param", "some-value")
                    ));
                }
            }
            return MailingInfo.noMessage();
        };
    }

    private OnboardingMailingCalculator dummyThirdStepMailing() {
        return state -> MailingInfo.singleMessage(THIRD_STEP_TEMPLATE, List.of());
    }

    private OnboardingMailingCalculator dummySuccessMailing() {
        return state -> MailingInfo.singleMessage(SUCCESS_TEMPLATE, List.of());
    }

    private OnboardingMailingCalculator dummyFailureMailing() {
        return state -> MailingInfo.singleMessage(FAILURE_TEMPLATE, List.of());
    }
}
