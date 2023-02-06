package ru.yandex.market.partner.mvc.controller.wizard;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.PlacementModelStepCalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для шага wizard'a "Модель подключения партнера".
 *
 * @see PlacementModelStepCalculator
 */
@DbUnitDataSet(before = "csv/commonBlueWizardData.before.csv")
public class WizardControllerPlacementModelFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Test
    void testCrossdock() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(CROSSDOCK_SUPPLIER_CAMPAIGN_ID, WizardStepType.PLACEMENT_MODEL)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testFulfillment() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(FULFILLMENT_CAMPAIGN_ID, WizardStepType.PLACEMENT_MODEL)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void testDropship() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(DROPSHIP_SUPPLIER_CAMPAIGN_ID, WizardStepType.PLACEMENT_MODEL)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
