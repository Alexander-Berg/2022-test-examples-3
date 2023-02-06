package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

data class V1OrdersKey(
    @JsonProperty("order_id")
    val orderId: String,
    @JsonProperty("status")
    val status: OrderStatus
)
