package ru.yandex.chemodan.qa.psbilling.steps

import io.qameta.allure.Step
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse
import org.awaitility.kotlin.await
import org.awaitility.kotlin.has
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollInterval
import org.hamcrest.Matchers
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.model.psbilling.OrderStatus
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V1OrdersKey
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

object V1OrderSteps {
    fun isStatusPaid(validatable: ValidatableResponse) {
        validatable.body("status", Matchers.equalTo(OrderStatus.PAID.value))
    }
}

@Step("Ожидание перехода статуса у заказа {orderId}")
fun awaitOrderUntilIsNotInit(orderId: String, user: TusUserRs): Response {
    return await
        .withPollInterval(5.seconds.toJavaDuration())
        .atMost(1.minutes.toJavaDuration())
        .untilCallTo { PsBillingClient.v1OrdersKey(orderId, user.uid) }
        .has {
            `as`(V1OrdersKey::class.java).status != OrderStatus.INIT
        }
}
