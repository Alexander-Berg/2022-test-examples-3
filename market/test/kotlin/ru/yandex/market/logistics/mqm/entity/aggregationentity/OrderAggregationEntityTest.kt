package ru.yandex.market.logistics.mqm.entity.aggregationentity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OrderAggregationEntityTest {

    @Test
    @DisplayName("Проверка применимости, если заполнено все")
    fun toKey() {
        val entity = OrderAggregationEntity(
            id = 1,
            barcode = "ignored",
            directFlow = true,
        )
        Assertions.assertThat(entity.toKey()).isEqualTo(
            "id:${entity.id};directFlow:${entity.directFlow};"
        )
    }

    @Test
    @DisplayName("Проверка применимости, если указан только id")
    fun toKeyOnlyId() {
        val entity = OrderAggregationEntity(id = 1)
        Assertions.assertThat(entity.toKey()).isEqualTo("${entity.id}")
    }

    @Test
    @DisplayName("Проверка применимости, если указан только directFlow")
    fun toKeyOnlyDirectFlow() {
        val entity = OrderAggregationEntity(directFlow = true)
        Assertions.assertThat(entity.toKey()).isEqualTo("directFlow:${entity.directFlow};")
    }
}
