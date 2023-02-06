package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.model.WizardStepStatus;
import ru.yandex.market.core.wizard.model.WizardStepType;
import ru.yandex.market.core.wizard.step.ApiDropshipBySellerStepCalculator;
import ru.yandex.market.mbi.environment.EnvironmentService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Тест шага настройки ПИ/АПИ для Dropship by seller.
 *
 * @see ApiDropshipBySellerStepCalculator
 */
@DbUnitDataSet(before = {"csv/commonWhiteWizardData.before.csv", "csv/testDsbsApiStep.before.csv"})
class WizardControllerApiDSBSStepFunctionalTest extends AbstractWizardControllerFunctionalTest {

    @Autowired
    EnvironmentService environmentService;

    @BeforeEach
    void before() {
        environmentService.setValue("select.latest.completed.request", "false");
    }

    @Test
    @DisplayName("Не DSBS. Ошибка, если шаг недоступен")
    void testNotDropshipBySeller() {
        HttpClientErrorException exception = assertThrows(
                HttpClientErrorException.class,
                () -> requestStep(CAMPAIGN_ID, WizardStepType.API_PI_SETTINGS)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }


    @ParameterizedTest(name = "{0}")
    @MethodSource("getParametersForApi")
    void testStatusFullAPI(String testName, long campaignId, Status expectedStatus) {
        ResponseEntity<String> response = requestStep(campaignId, WizardStepType.API_PI_SETTINGS);

        assertResponse(response, WizardStepStatus.newBuilder()
                .withStep(WizardStepType.API_PI_SETTINGS)
                .withStatus(expectedStatus)
                .build(), JSONCompareMode.LENIENT);
    }

    private static Stream<Arguments> getParametersForApi() {
        return Stream.of(
                Arguments.of("Статус FULL. Настроено АПИ.", 14004L, Status.FULL),
                Arguments.of("Статус FULL. Настроено через ПИ.", 14005L, Status.FULL),
                Arguments.of("Статус EMPTY. Не настроено АПИ.", 14003L, Status.EMPTY),
                Arguments.of("Статус EMPTY. Нет выхода на модерацию, АПИ ненастроено", 14002L, Status.EMPTY),
                Arguments.of("Статус NONE. Заявка не отправлена в АБО", 14001L, Status.NONE)
        );
    }

}
