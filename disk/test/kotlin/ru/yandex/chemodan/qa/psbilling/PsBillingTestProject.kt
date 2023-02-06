package ru.yandex.chemodan.qa.psbilling

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.qameta.allure.restassured.AllureRestAssured
import io.restassured.RestAssured
import io.restassured.config.ObjectMapperConfig
import io.restassured.config.RestAssuredConfig
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.response.ValidatableResponse
import org.hamcrest.Matchers
import ru.yandex.chemodan.qa.psbilling.client.PaymentForm
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.config.GlobalTestData
import ru.yandex.chemodan.qa.psbilling.extensions.assert
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V1UsersSubscribeRs
import ru.yandex.chemodan.qa.psbilling.model.psbilling.utils.Product
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import ru.yandex.chemodan.qa.psbilling.steps.V1OrderSteps
import ru.yandex.chemodan.qa.psbilling.steps.V1UserServicesSteps
import ru.yandex.chemodan.qa.psbilling.steps.awaitOrderUntilIsNotInit
import ru.yandex.chemodan.qa.psbilling.steps.awaitUserServiceUntilIsActual

val mapper: ObjectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

val commonAssured = RestAssured.with()
    .filter(AllureRestAssured())
    .filter(RequestLoggingFilter())
    .filter(ResponseLoggingFilter())
    .config(
        RestAssuredConfig()
            .objectMapperConfig(
                ObjectMapperConfig().jackson2ObjectMapperFactory { _, _ -> mapper }
            )
    )
