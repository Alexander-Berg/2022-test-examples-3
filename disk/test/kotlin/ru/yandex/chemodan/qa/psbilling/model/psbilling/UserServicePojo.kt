package ru.yandex.chemodan.qa.psbilling.model.psbilling

import com.fasterxml.jackson.annotation.JsonProperty

data class UserServicePojo(
    @JsonProperty("service_id")
    val serviceId: String,
    @JsonProperty("synchronization_status")
    val synchronizationStatus: SynchronizationStatus,
)
