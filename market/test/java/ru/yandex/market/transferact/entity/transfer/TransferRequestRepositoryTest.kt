package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.actor.name

const val requestBody = "{\"field\": \"value\"}"
const val idempotencyKey = "idempotencyKey"

class TransferRequestRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var transferRequestRepository: TransferRequestRepository

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Test
    fun `when then`() {
        var transfer = TransferEntity(status = status)
        transfer = transferRepository.save(transfer)
        var actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorType = actorTypeRepository.save(actorType)
        var transferRequest = TransferRequestEntity(requestBody = requestBody, idempotencyKey = idempotencyKey)
        transferRequest.actorType = actorType
        transferRequest.transfer = transfer

        transferRequest = transferRequestRepository.save(transferRequest)

        val found = transferRequestRepository.getById(transferRequest.getId())
        assertThat(found).isNotNull
        assertThat(found?.idempotencyKey).isEqualTo(idempotencyKey)
        assertThat(found?.requestBody).isEqualTo(requestBody)
        assertThat(found?.actorType).isNotNull
        assertThat(found?.actorType).isEqualTo(actorType)
        assertThat(found?.transfer).isNotNull
        assertThat(found?.transfer).isEqualTo(transfer)
    }

}
