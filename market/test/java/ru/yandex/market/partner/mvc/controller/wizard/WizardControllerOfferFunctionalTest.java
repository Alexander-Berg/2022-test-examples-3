package ru.yandex.market.partner.mvc.controller.wizard;

import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

/**
 * Функциональные тесты для шага wizard'a "Офферы".
 *
 * @author Vadim Lyalin
 * @see ru.yandex.market.core.wizard.step.OfferStepStatusCalculator
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerOfferFunctionalTest extends AbstractWizardControllerFunctionalTest {

    /**
     * Проверяет, что шаг OFFER в статусе FILLED.
     */
    @Test
    void testOfferStepFilled() {
        mockPartnerOffers(1);

        var response = requestStep(SMB_CAMPAIGN_ID, WizardStepType.OFFER);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверяет, что шаг OFFER в статусе EMPTY.
     */
    @Test
    void testOfferStepEmpty() {
        mockPartnerOffers(0);
        var response = requestStep(NO_OFFER_SMB_CAMPAIGN_ID, WizardStepType.OFFER);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.OFFER)
                .withStatus(status)
                .build();
    }
}
