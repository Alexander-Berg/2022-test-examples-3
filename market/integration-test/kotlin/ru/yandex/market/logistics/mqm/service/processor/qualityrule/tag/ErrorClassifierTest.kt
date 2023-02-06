package ru.yandex.market.logistics.mqm.service.processor.qualityrule.tag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvFileSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.Cause
import ru.yandex.market.logistics.mqm.entity.enums.Subcause

class ErrorClassifierTest : AbstractContextualTest() {

    @Autowired
    private lateinit var classifier: ErrorClassifier

    @Test
    fun badGatewayClassificationRuleTest() {
        assertThat(classifier.classify(null, "code 1000: 502 Bad Gateway"))
            .isEqualTo(ClassificationResult(Cause.TECH_ERROR, Subcause.NETWORK_UNREACHABLE))
    }

    @Test
    fun barcodeIsAlreadyInUseClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "code 9999: Штрихкод отправления уже используется другим заказчиком"
            )
        ).isEqualTo(ClassificationResult(Cause.VALIDATION_ERROR))
    }

    @Test
    fun incorrectCityClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "Нет указаного города или город указан неверно."
            )
        ).isEqualTo(ClassificationResult(Cause.BUSINESS_ERROR, Subcause.METHOD_API))
    }

    @Test
    fun incorrectFromRegionClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "code 9999: Нельзя оформить заказ с отправлением из региона: Москва."
            )
        ).isEqualTo(ClassificationResult(Cause.BUSINESS_ERROR, Subcause.LOGIC_ERROR))
    }

    @Test
    fun incorrectPickupCodeClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "Неверный код пункта Выдачи; "
            )
        ).isEqualTo(ClassificationResult(Cause.BUSINESS_ERROR))
    }

    @Test
    fun incorrectSenderPhoneNumberClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "code 9999: Некорректный телефон отправителя"
            )
        ).isEqualTo(ClassificationResult(Cause.BUSINESS_ERROR, Subcause.FRONTEND_ERROR))
    }

    @Test
    fun incorrectShipmentDateClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null, "code 9999: Marschroute error message [Невозможно отгрузить заказ в выбранную дату], "
                        + "Marschroute error code [218]"
            )
        ).isEqualTo(ClassificationResult(Cause.VALIDATION_ERROR))
    }

    @Test
    fun incorrectTokenClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "Токен не действителен"
            )
        ).isEqualTo(ClassificationResult(Cause.TECH_ERROR))
    }

    @Test
    fun internalErrorClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "org.springframework.web.client.HttpServerErrorException\$InternalServerError: 500 "
            )
        ).isEqualTo(ClassificationResult(Cause.TECH_ERROR, Subcause.NETWORK_UNREACHABLE))
    }

    @Test
    fun requestProcessingErrorClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "code 9999: Ошибка обработки запроса"
            )
        ).isEqualTo(ClassificationResult(Cause.TECH_ERROR, Subcause.PARTNER_SYSTEM_ERROR))
    }

    @Test
    fun resourceAccessExceptionClassificationRuleTest() {
        assertThat(
            classifier.classify(
                null,
                "\"org.springframework.web.client.ResourceAccessException: "
                        + "I/O error on POST request for \\\"\"https://wstest.dpd.ru/rest/yandex/createOrder\\\"\": "
                        + "The target server failed to respond; nested exception is org.apache.http.NoHttpResponseException: "
                        + "The target server failed to respond\""
            )
        ).isEqualTo(ClassificationResult(Cause.TECH_ERROR, Subcause.NETWORK_UNREACHABLE))
    }

    @Test
    fun techErrorCodeClassificationRuleTest() {
        assertThat(classifier.classify(404, null))
            .isEqualTo(ClassificationResult(Cause.TECH_ERROR))
    }

    @Test
    fun nonTechErrorCodeClassificationRuleTest() {
        assertThat(classifier.classify(123, null))
            .isEqualTo(ClassificationResult(Cause.VALIDATION_ERROR))
    }

    @ParameterizedTest(name = "{1},{2} для {0}")
    @CsvFileSource(
        resources = ["/service/processor/qualityrule/tag/errors_to_classify.csv"],
        numLinesToSkip = 1,
        delimiter = ';'
    )
    fun csvMessagesSnapshotTest(errorMessage: String, expectedCause: String, expectedSubcause: String?) {
        val expectedResult = ClassificationResult(
            Cause.valueOf(expectedCause),
            if (expectedSubcause != null) Subcause.valueOf(expectedSubcause) else null
        )
        assertThat(classifier.classify(null, errorMessage)).isEqualTo(expectedResult).withFailMessage { errorMessage }
    }
}
