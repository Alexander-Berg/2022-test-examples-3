package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

enum class OrderStatus(val value: String) {
    @JsonProperty("init")
    INIT("init"),

    @JsonProperty("paid")
    PAID("paid"),

    @JsonProperty("upgraded")
    UPGRADED("upgraded"),

    @JsonProperty("on_hold")
    ON_HOLD("on_hold"),

    @JsonProperty("payment_error")
    PAYMENT_ERROR("payment_error")
}
