package ru.yandex.chemodan.qa.psbilling.steps

import io.restassured.response.ValidatableResponse
import org.hamcrest.collection.IsEmptyCollection

object V3ProductsetKeyProductStep {
    fun emptyProducts(validatable: ValidatableResponse) {
        validatable.body("items", IsEmptyCollection.empty<Any>())
    }
}
