package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository

class TransferRequestQueryServiceTest : AbstractTest() {
    @Autowired
    lateinit var transferRequestQueryService: TransferRequestQueryService

    @Autowired
    lateinit var transferRequestRepository: TransferRequestRepository

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Test
    fun `When getTransferRequestByIdempotencyKey then return transferRequestEntity`() {
        val idempotencyKey = "key"

        val transfer = TransferEntity(status = status)
        transferRepository.save(transfer)

        val actorType = ActorTypeEntity(name = "test", apiKey = "apiKey")
        actorTypeRepository.save(actorType)

        val transferRequestEntity = TransferRequestEntity(requestBody = "null", idempotencyKey = idempotencyKey)
        transferRequestEntity.transfer = transfer
        transferRequestEntity.actorType = actorType
        transferRequestRepository.save(transferRequestEntity)

        val found = transferRequestQueryService.getTransferRequestByIdempotencyKey(idempotencyKey)

        Assertions.assertThat(found).isNotNull
    }

    @Test
    fun `When getTransferRequestByIdempotencyKey throw exception`() {
        Assertions.assertThat(transferRequestQueryService
            .getTransferRequestByIdempotencyKey("123")).isNull()
    }
}
