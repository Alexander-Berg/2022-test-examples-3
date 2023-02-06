package ru.yandex.chemodan.qa.psbilling.client

import io.qameta.allure.Step
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import ru.yandex.chemodan.qa.psbilling.commonAssured
import ru.yandex.chemodan.qa.psbilling.config.GlobalTestData

class PaymentForm(val paymentUrl: String) {

    val purchaseToken = paymentUrl.substring(paymentUrl.indexOf('=') + 1)
    val xrfToken by lazy { openForm() }

    @Step("TODO. Проверка цена на форме")
    fun assertPrice(amount: Number) {
        Given {
            spec(commonAssured)

            baseUri(paymentUrl)

            param("purchase_token", purchaseToken)
        } When {
            get()
        } Then {
            statusCode(200)
        } Extract {
            //TODO check price
        }
    }

    @Step("Открываем форму оплаты")
    private fun openForm() =
        Given {
            spec(commonAssured)

            baseUri(GlobalTestData.PCI_URI)
            param("purchase_token", purchaseToken)
            param("template_tag", "modern/form")
            param("desktop", "1")
            param("lang", "ru")
        } When {
            get("/web/card_form")
        } Then {
            statusCode(200)
        } Extract {
            htmlPath()
                .getString("**.find { it.@name == 'xrf_token' }.@value")
        }

    @Step("Закрываем форму оплаты")
    fun cancel() {
        Given {
            spec(commonAssured)

            baseUri(GlobalTestData.TRUST_URI)
            param("purchase_token", purchaseToken)
        } When {
            post("/web/cancel_payment")
        } Then {
            statusCode(200)
        }
    }

    @Step("Оплата картой {card.number}")
    fun pay(card: PaymentCard) {
        Given {
            spec(commonAssured)

            baseUri(GlobalTestData.PCI_URI)
            formParam("payment_method", "new_card")
            formParam("drf_token", "")
            formParam("cardholder", "CARD HOLDER")
            formParam("template_tag", "modern/form")
            formParam("card_number", card.number)
            formParam("expiration_month", card.month)
            formParam("expiration_year", card.year)
            formParam("cvn", card.cvv)
            formParam("purchase_token", purchaseToken)
            formParam("xrf_token", xrfToken)
        } When {
            post("/web/start_payment")
        } Then {
            statusCode(200)
        }
    }
}

data class PaymentCard(
    val number: String,
    val month: String,
    val year: String,
    val cvv: String,
)
