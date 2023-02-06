package ru.yandex.market.partner.mvc.controller.wizard;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для шага wizard'a "Юридическая информация".
 * См {@link ru.yandex.market.core.wizard.step.LegalStepStatusCalculator}
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerLegalFunctionalTest extends AbstractWizardControllerFunctionalTest {

    /**
     * Проверить что юридическая информация не заполнена.
     */
    @Test
    void testLegalStepWithoutLegal() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.LEGAL);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    /**
     * Проверить что юридическая информация заполнена.
     */
    @Test
    @DbUnitDataSet(before = "csv/testLegalStepFilled.before.csv")
    void testLegalStepFilled() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.LEGAL);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    @Test
    @DisplayName("Шаг недоступен для ДСБС магазина")
    void testNotAvailableForDropshipBySeller() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DSBS_CAMPAIGN_ID, WizardStepType.LEGAL)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }


    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.LEGAL)
                .withStatus(status)
                .build();
    }
}
