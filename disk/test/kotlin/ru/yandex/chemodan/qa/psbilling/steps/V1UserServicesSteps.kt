package ru.yandex.chemodan.qa.psbilling.steps

import io.qameta.allure.Step
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollInterval
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.hasSize
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.model.psbilling.SynchronizationStatus
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V1UsersServicesRs
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object V1UserServicesSteps {

    fun autoProlongEnabled(validatable: ValidatableResponse){
        validatable.body("items[0].auto_prolong_enabled", Matchers.equalTo(false))
    }

    fun serviceEqId(validatable: ValidatableResponse, serviceId: String){
        validatable.body("items[0].service_id", Matchers.equalTo(serviceId))
    }

    fun onlyOneService(validatable: ValidatableResponse) {
        validatable.body("items", hasSize<Any>(1))
    }

    fun serviceEqProductPriceId(validatable: ValidatableResponse, productPriceId: String) {
        validatable.body("items.product.price.price_id", hasItem(productPriceId))
    }
}

@Step("Ожидание актуализации сервиса")
fun awaitUserServiceUntilIsActual(user: TusUserRs): Response {
    return await
        .withPollInterval(5.seconds.toJavaDuration())
        .atMost(1.minutes.toJavaDuration())
        .untilCallTo { PsBillingClient.v1UsersServices(uid = user.uid) }
        .has {
            `as`(V1UsersServicesRs::class.java)
                .items
                .first()
                .synchronizationStatus == SynchronizationStatus.ACTUAL
        }
}
