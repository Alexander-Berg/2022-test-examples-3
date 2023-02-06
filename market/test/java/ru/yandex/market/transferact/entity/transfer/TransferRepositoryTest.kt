package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest

val status = TransferStatus.CREATED
const val transportationId = "transportationId"

class TransferRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Test
    fun `When getById then return transfer`() {
        val entity = TransferEntity(status = status)

        val saved = transferRepository.save(entity)

        val found = transferRepository.getById(saved.id)
        assertThat(found).isNotNull
        assertThat(found?.getId()).isEqualTo(saved.getId())
        assertThat(found?.status).isEqualTo(status)
    }

}
