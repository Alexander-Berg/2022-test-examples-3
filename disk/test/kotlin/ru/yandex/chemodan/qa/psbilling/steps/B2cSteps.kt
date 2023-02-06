package ru.yandex.chemodan.qa.psbilling.steps

import io.qameta.allure.Step
import ru.yandex.chemodan.qa.psbilling.client.PaymentForm
import ru.yandex.chemodan.qa.psbilling.client.PsBillingClient
import ru.yandex.chemodan.qa.psbilling.config.GlobalTestData
import ru.yandex.chemodan.qa.psbilling.extensions.assert
import ru.yandex.chemodan.qa.psbilling.model.psbilling.V1UsersSubscribeRs
import ru.yandex.chemodan.qa.psbilling.model.psbilling.utils.Product
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs

@Step("Покупка продукта {product.name}")
fun buyProduct(user: TusUserRs, product: Product) {
    val (paymentFormUrl, orderId) = PsBillingClient.v1UsersSubscribe(product.priceId, user.uid)
        .`as`(V1UsersSubscribeRs::class.java)

    PaymentForm(paymentFormUrl).apply {
        assertPrice(product.amount)
        pay(GlobalTestData.cardSuccessPay)
    }

    val orderRs = awaitOrderUntilIsNotInit(orderId, user)

    orderRs assert {
        V1OrderSteps.isStatusPaid(this)
    }
}

@Step("Отписываемся от подписки")
fun assertCancelSubscribe(user: TusUserRs) {
    val serviceId = PsBillingClient.v1UsersServices(uid = user.uid)
        .jsonPath()
        .getString("items[0].service_id")

    PsBillingClient.v1UsersUnsubscribe(serviceId, user.uid)

    PsBillingClient.v1UsersServices(uid = user.uid) assert {
        V1UserServicesSteps.serviceEqId(this, serviceId)
        V1UserServicesSteps.autoProlongEnabled(this)
    }
}

@Step("Проверка актуализации сервиса")
fun assertUserServiceActualization(user: TusUserRs, product: Product) {
    val v1UsersServicesRs = awaitUserServiceUntilIsActual(user)

    v1UsersServicesRs assert {
        V1UserServicesSteps.onlyOneService(this)
        V1UserServicesSteps.serviceEqProductPriceId(this, product.priceId)
    }
}


