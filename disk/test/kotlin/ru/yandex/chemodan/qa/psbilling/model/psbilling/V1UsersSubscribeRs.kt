package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

data class V1UsersSubscribeRs(
    @JsonProperty("payment_form_url")
    val paymentFormUrl: String,
    @JsonProperty("order_id")
    val orderId: String
)
