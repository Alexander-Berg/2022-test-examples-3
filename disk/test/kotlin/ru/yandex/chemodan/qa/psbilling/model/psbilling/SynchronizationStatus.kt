package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

enum class SynchronizationStatus(val value: String) {
    @JsonProperty("actual")
    ACTUAL("actual"),

    @JsonProperty("synchronizing")
    SYNCHRONIZING("synchronizing");
}
