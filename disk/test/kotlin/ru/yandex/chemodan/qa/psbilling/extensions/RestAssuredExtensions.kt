package ru.yandex.chemodan.qa.psbilling.extensions

import io.restassured.module.kotlin.extensions.Then
import io.restassured.response.Response
import io.restassured.response.ValidatableResponse

infix fun Response.assert(block: ValidatableResponse.() -> Unit): ValidatableResponse = Then(block)

