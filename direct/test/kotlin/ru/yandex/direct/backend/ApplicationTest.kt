package ru.yandex.direct.backend

import io.kotest.core.spec.style.FunSpec
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.kotest.matchers.shouldBe
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import main.kotlin.ru.yandex.direct.backend.plugins.configureRouting

class RouteTests : FunSpec({
    test("GET") {
        testApplication {
            application {
                configureRouting()
            }

            val response = client.get("/")

            response.bodyAsText() shouldBe """Hello world"""
            response.status shouldBe HttpStatusCode.OK
        }
    }
})

