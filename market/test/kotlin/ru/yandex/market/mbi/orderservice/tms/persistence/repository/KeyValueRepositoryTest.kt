package ru.yandex.market.mbi.orderservice.tms.persistence.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.orderservice.common.model.pg.KeyValueEntity
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.KeyValueRepository

class KeyValueRepositoryTest : FunctionalTest() {

    @Autowired
    lateinit var keyValueRepository: KeyValueRepository

    @Test
    fun `test crud operations`() {
        val affectedRows = keyValueRepository.upsert(KeyValueEntity("a", "1"))
        assertThat(affectedRows).isEqualTo(1)
        val keyValue1 = keyValueRepository.findByKey("a")
        assertThat(keyValue1).isNotNull.isEqualTo(KeyValueEntity("a", "1"))
        keyValueRepository.upsert(KeyValueEntity("a", "2"))
        val keyValue2 = keyValueRepository.findByKey("a")
        assertThat(keyValue2).isNotNull.isEqualTo(KeyValueEntity("a", "2"))
    }
}
