package ru.yandex.market.partner.mvc.controller.wizard;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.core.wizard.experiment.WizardExperimentService;
import ru.yandex.market.core.wizard.experiment.WizardExperimentsConfig;
import ru.yandex.market.core.wizard.model.WizardStepType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Функциональные тесты для шага wizard'a "Возвраты и контакты".
 * См {@link ru.yandex.market.core.wizard.step.ReturnAndFeedbackContactsStepStatusCalculator}
 */
@DbUnitDataSet(before = {"csv/commonWizardData.before.csv"})
public class WizardControllerReturnAndFeedbackContactsStepFunctionalTest
        extends AbstractWizardControllerFunctionalTest {

    @Autowired
    private WizardExperimentService returnContactsExperiment;

    @BeforeEach
    void setUp() {
        environmentService.setValue(WizardExperimentsConfig.RETURN_CONTACTS_VAR, "1");
        returnContactsExperiment.close();
    }

    @DisplayName("Проверка статуса шага заполнения контактов для возвратов")
    @ParameterizedTest
    @MethodSource
    void ffReturnContactsTest(String name, long campaignId, Status responseStatus) {
        var response = requestStep(campaignId, WizardStepType.RETURN_CONTACTS);
        assertResponse(response, makeResponseStepStatus(WizardStepType.RETURN_CONTACTS, responseStatus));
    }

    @DisplayName("Шаг доступен FULFILLMENT, FBS, DROPSHIP_BY_SELLER")
    @ParameterizedTest
    @MethodSource
    void testAvailableForAllModel(long campaignId) {
        assertThat(requestStep(campaignId, WizardStepType.RETURN_CONTACTS).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private static Stream<Arguments> testAvailableForAllModel() {
        return Stream.of(
                Arguments.of(FULFILLMENT_CAMPAIGN_ID),
                Arguments.of(DROPSHIP_SUPPLIER_CAMPAIGN_ID),
                Arguments.of(14004L)
        );
    }

    private static Stream<Arguments> ffReturnContactsTest() {
        return Stream.of(
                Arguments.of(
                        "Проверка контактов для возвратов для FULFILLMENT - статус незаполнено", 12000L, Status.EMPTY
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FULFILLMENT - частично заполнено", 12100L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FULFILLMENT - заполнены не все поля", 12101L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FULFILLMENT - заполнены + FEEDBACK из заявки", 12102L, Status.FULL
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FBS - статус незаполнено", 12501L, Status.EMPTY
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FBS - частично заполнено", 12502L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FBS - заполнены не все поля", 12503L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для FBS - заполнены", 12504L, Status.FULL
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для DBS - статус незаполнено", 14004L, Status.EMPTY
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для DBS - частично заполнено", 14005L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для DBS - заполнены не все поля", 14006L, Status.FILLED
                ),
                Arguments.of(
                        "Проверка контактов для возвратов для DBS - заполнены + FEEDBACK из заявки", 14007L, Status.FULL
                )
        );
    }
}
