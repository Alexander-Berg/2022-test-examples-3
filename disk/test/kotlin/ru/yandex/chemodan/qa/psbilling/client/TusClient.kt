package ru.yandex.chemodan.qa.psbilling.client

import io.qameta.allure.Step
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import ru.yandex.chemodan.qa.psbilling.commonAssured
import ru.yandex.chemodan.qa.psbilling.config.GlobalTestData
import ru.yandex.chemodan.qa.psbilling.config.TusSecret
import ru.yandex.chemodan.qa.psbilling.model.tus.TusUserRs

object TusClient {

    @Step("Создаём пользователя")
    fun createUser(): TusUserRs {
        return Given {
            spec(commonAssured)

            baseUri(GlobalTestData.TUS_URI)
            header("Authorization", "OAuth ${TusSecret.instance.tvmSecret}")
        } When {
            post("/1/create_account/portal/")
        } Then {
            statusCode(200)
        } Extract {
            jsonPath()
                .getObject("account", TusUserRs::class.java)
        }
    }
}
