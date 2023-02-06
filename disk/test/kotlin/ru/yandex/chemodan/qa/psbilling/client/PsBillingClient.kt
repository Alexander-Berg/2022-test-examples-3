package ru.yandex.chemodan.qa.psbilling.client

import io.qameta.allure.Step
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.Response
import ru.yandex.chemodan.qa.psbilling.commonAssured
import ru.yandex.chemodan.qa.psbilling.config.GlobalTestData

object PsBillingClient {

    @Step("Подписываемся на {price}")
    fun v1UsersSubscribe(
        price: String,
        uid: String,

        ): Response {
        return Given {
            spec(commonAssured)

            baseUri(GlobalTestData.PS_BILLING_URI)
            header("X-UID", uid)
            param("lang", "ru")

            param("price_id", price)
            param("disable_trust_header", "true")
            param("real_user_ip", "1.1.1.1")
            param("domain_suffix", "ru")
            param("return_path", "!ORDER_ID!")
            param("form_type", "desktop")
            param("login_id", "")
        } When {
            post("/v1/users/subscribe")
        } Then {
            statusCode(200)
        } Extract {
            response()
        }
    }

    @Step("Отписываемся от сервиса {serviceId}")
    fun v1UsersUnsubscribe(
        serviceId: String,
        uid: String,

        ): Response {
        return Given {
            spec(commonAssured)

            baseUri(GlobalTestData.PS_BILLING_URI)
            header("X-UID", uid)

            param("service_id", serviceId)
        } When {
            post("/v1/users/unsubscribe")
        } Then {
            statusCode(204)
        } Extract {
            response()
        }
    }

    @Step("Получаем продукты {productSet} для покупок")
    fun v3ProductSetKeyProduct(
        productSet: String,
        uid: String? = null
    ): Response {
        return Given {
            spec(commonAssured)

            uid?.let { header("X-UID", it) }

            baseUri(GlobalTestData.PS_BILLING_URI)
            param("lang", "ru")

            // param("packageName", null)
            // param("skipDisabledFeatures", null)
            // param("showOptInSubsFeature", null)
            // param("promoActivation", true)
            // param("currency", null)
            // param("payloadType", "web_tuning")
            // param("payloadVersion", 0)
        } When {
            get("/v3/productsets/$productSet/products")
        } Then {
            statusCode(200)
        } Extract {
            response()
        }
    }

    @Step("Получаем заказ по идентификатору {orderId}")
    fun v1OrdersKey(
        orderId: String,
        uid: String
    ): Response {
        return Given {
            spec(commonAssured)

            baseUri(GlobalTestData.PS_BILLING_URI)
            header("X-UID", uid)
        } When {
            get("/v1/orders/$orderId")
        } Then {
            statusCode(200)
        } Extract {
            response()
        }
    }

    @Step("Получаем список сервисов у пользователя")
    fun v1UsersServices(
        uid: String,
        productOwner: String? = null,
        lang: String? = null,
        showOptInSubsFeature: String? = null,
        status: String? = null
    ): Response {
        return Given {
            spec(commonAssured)

            productOwner?.let { param("product_owner", it) }
            lang?.let { param("lang", it) }
            showOptInSubsFeature?.let { param("show_opt_in_subs_feature", it) }
            status?.let { param("status", it) }

            baseUri(GlobalTestData.PS_BILLING_URI)
            header("X-UID", uid)
        } When {
            get("/v1/users/services")
        } Then {
            statusCode(200)
        } Extract {
            response()
        }
    }
}
