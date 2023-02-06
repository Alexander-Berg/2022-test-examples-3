package ru.yandex.market.core.partner.onboarding.sender.calculators;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.partner.onboarding.sender.MailingInfo;
import ru.yandex.market.core.partner.onboarding.state.PartnerOnboardingState;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.PARTNER_ID_ENTITY;
import static ru.yandex.market.core.partner.onboarding.sender.SenderTestUtils.createTestDropshipState;

@DbUnitDataSet(before = "../AssortmentCompletedOnboardingMailingCalculatorTest.before.csv")
public class AssortmentCompletedOnboardingMailingCalculatorTest extends FunctionalTest {

    private AssortmentCompletedOnboardingMailingCalculator mailing;

    //выпилить при выпиливании флага partnerOnboardingUseAssortmentCalculator
    @Autowired
    private Supplier<Boolean> partnerOnboardingUseAssortmentCalculator;

    @Autowired
    private BusinessService businessService;

    @BeforeEach
    void init() {
        mailing = new AssortmentCompletedOnboardingMailingCalculator(businessService,
                partnerOnboardingUseAssortmentCalculator);
    }

    @Test
    @DbUnitDataSet(before = "../AssortmentCompletedOnboardingMailingCalculatorTestFlagTrue.csv")
    void checkMailingFlagTrue() {
        mailingCall(getAssortmentState(Status.FULL), true);
    }

    @Test
    @DbUnitDataSet(before = "../AssortmentCompletedOnboardingMailingCalculatorTestFlagFalse.csv")
    void checkMailingFlagFalse() {
        mailingCall(getAssortmentState(Status.FULL), false);
    }

    @ParameterizedTest
    @MethodSource
    @DbUnitDataSet(before = "../AssortmentCompletedOnboardingMailingCalculatorTestFlagTrue.csv")
    void checkMailingCall(Status status, boolean expectedNeedSend) {
        mailingCall(getAssortmentState(status), expectedNeedSend);
    }

    public static Stream<Arguments> checkMailingCall() {
        return Stream.of(
                Arguments.of(Status.FULL, true),
                Arguments.of(Status.FAILED, false)
        );
    }

    private void mailingCall(PartnerOnboardingState partnerIdWithOnboardingState, boolean expectedNeedSend) {
        MailingInfo info = mailing.calculateMailing(partnerIdWithOnboardingState);
        if (expectedNeedSend) {
            assertTrue(info.needSend());
        } else {
            assertFalse(info.needSend());
        }
    }

    private PartnerOnboardingState getAssortmentState(Status status) {
        Instant now = Instant.now();
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
                                status,
                                now
                        )
                )
        );
        return partnerIdWithOnboardingState;
    }
}
