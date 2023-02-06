package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Функциональные тесты для шага wizard'a "Общая информация о партнере".
 *
 * @author natalokshina
 * @see ru.yandex.market.core.wizard.step.CommonInfoStepCalculator
 */
@DbUnitDataSet(before = "csv/commonWhiteWizardData.before.csv")
class WizardControllerCommonInfoStepFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Test
    void testCommonInfoFull() {
        var response = requestStep(DSBS_CAMPAIGN_ID, WizardStepType.COMMON_INFO);
        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.COMMON_INFO)
                .withStatus(Status.FULL)
                .withDetails(
                        Map.of(
                                "emails", List.of("mail1@test.ru"),
                                "isCpaPartnerInterface", false
                        )
                ).build());
    }

    @Test
    void testCommonInfo_forNonDsbs() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(CAMPAIGN_ID, WizardStepType.COMMON_INFO)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }
}
