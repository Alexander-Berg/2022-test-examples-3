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
 * Функциональные тесты для шага wizard'a "Общие настройки".
 * См {@link ru.yandex.market.core.wizard.step.SettingsStepStatusCalculator}
 *
 * @author Vladislav Bauer
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerSettingsFunctionalTest extends AbstractWizardControllerFunctionalTest {

    /**
     * Проверить что шаг визарда считается завершенным, если все "общие настройки" магазина заполнены.
     */
    @Test
    @DbUnitDataSet(before = "csv/testProgramStepCpcFilledWithPhone.before.csv")
    void testSettingsStepCompletelyFilled() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.SETTINGS);
        assertResponse(response, makeResponseStepStatus(Status.FILLED));
    }

    /**
     * Проверить что шаг визарда считается незавершенным, если у магазина есть незаполненные "общие настройки".
     */
    @DbUnitDataSet(before = "csv/testSettingsStepPartiallyFilled.csv")
    @Test
    void testSettingsStepPartiallyFilled() {
        final ResponseEntity<String> response = requestStep(CAMPAIGN_ID, WizardStepType.SETTINGS);
        assertResponse(response, makeResponseStepStatus(Status.EMPTY));
    }

    @Test
    @DisplayName("Шаг недоступен для ДСБС магазина")
    void testNotAvailableForDropshipBySeller() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DSBS_CAMPAIGN_ID, WizardStepType.SETTINGS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    private static WizardStepStatus makeResponseStepStatus(Status status) {
        return WizardStepStatus.newBuilder()
                .withStep(WizardStepType.SETTINGS)
                .withStatus(status)
                .build();
    }
}
