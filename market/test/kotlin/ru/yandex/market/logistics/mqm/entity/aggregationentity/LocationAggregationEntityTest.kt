package ru.yandex.market.logistics.mqm.entity.aggregationentity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LocationAggregationEntityTest {

    @Test
    @DisplayName("Проверка применимости, если заполнено все")
    fun toKey() {
        val entity = LocationAggregationEntity(
            region = "test_region",
            address= "test_address"
        )
        Assertions.assertThat(entity.toKey()).isEqualTo(
            "region:${entity.region.hashCode()};address:${entity.address.hashCode()};"
        )
    }

    @Test
    @DisplayName("Проверка применимости, если указан только регион")
    fun toKeyOnlyRegion() {
        val entity = LocationAggregationEntity(region = "test_region")
        Assertions.assertThat(entity.toKey()).isEqualTo("${entity.region.hashCode()}")
    }

    @Test
    @DisplayName("Проверка применимости, если указан только адрес")
    fun toKeyOnlyAddress() {
        val entity = LocationAggregationEntity(address = "test_address")
        Assertions.assertThat(entity.toKey()).isEqualTo("address:${entity.address.hashCode()};")
    }
}
