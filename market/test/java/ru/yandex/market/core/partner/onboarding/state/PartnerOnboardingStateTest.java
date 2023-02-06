package ru.yandex.market.core.partner.onboarding.state;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;

/**
 * Тест правила "не посылать после нашего подтверждения"
 */
class PartnerOnboardingStateTest {

    @Test
    void testSupplierInfoApprovedByUs() {
        var now = Instant.now();
        PartnerOnboardingState state = createTestDropshipState(
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
        assertThat(state.lastStepIsApprovedByYandex()).isTrue();
    }

    @Test
    void testAssortmentApprovedByUs() {
        var now = Instant.now();
        PartnerOnboardingState state = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(2L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCK_UPDATE,
                                Status.EMPTY,
                                now
                        )
                )
        );
        assertThat(state.lastStepIsApprovedByYandex()).isTrue();
    }

    @Test
    void testCheckOrderApprovedByUs() {
        var now = Instant.now();
        PartnerOnboardingState state = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(2L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FILLED,
                                now.minus(Duration.ofDays(1L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.TEST_ORDER,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCKS,
                                Status.EMPTY,
                                now
                        )
                )
        );
        assertThat(state.lastStepIsApprovedByYandex()).isTrue();
    }

    @Test
    void testSupplyApprovedByUs() {
        var now = Instant.now();
        PartnerOnboardingState state = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.ENABLING,
                                now.minus(Duration.ofDays(2L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.ASSORTMENT,
                                Status.FILLED,
                                now.minus(Duration.ofDays(1L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.MARKETPLACE,
                                Status.FULL,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCKS,
                                Status.EMPTY,
                                now
                        )
                )
        );
        assertThat(state.lastStepIsApprovedByYandex()).isTrue();
    }

    @Test
    void testStocksNotApprovedByUs() {
        var now = Instant.now();
        PartnerOnboardingState state = createTestDropshipState(
                PARTNER_ID_ENTITY,
                now.minus(Duration.ofDays(10L)),
                List.of(
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.SUPPLIER_INFO,
                                Status.FULL,
                                now.minus(Duration.ofDays(2L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCKS,
                                Status.FULL,
                                now.minus(Duration.ofDays(1L))
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.MARKETPLACE,
                                Status.EMPTY,
                                now
                        ),
                        new PartnerOnboardingState.WizardStepData(
                                WizardStepType.STOCKS,
                                Status.EMPTY,
                                now
                        )
                )
        );
        assertThat(state.lastStepIsApprovedByYandex()).isFalse();
    }
}
