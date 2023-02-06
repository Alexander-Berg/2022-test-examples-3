package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

data class ProductPricePojo(
    @JsonProperty("price_id")
    val priceId: String,
    @JsonProperty("period")
    val period: String,
    @JsonProperty("amount")
    val amount: Double,
    @JsonProperty("currency")
    val currency: String
)
