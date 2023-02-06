package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.actor.name

class TransferRequestCommandServiceTest : AbstractTest() {

    @Autowired
    lateinit var transferRequestCommandService: TransferRequestCommandService

    @Autowired
    lateinit var transferRequestRepository: TransferRequestRepository

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Test
    fun `When create transfer request then return new entity`() {
        val transfer = TransferEntity(status = status)
        transferRepository.save(transfer)
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)

        val createTransferRequestProjection = transferRequestCommandService.create(TransferRequestCommand.Create(
            requestBody,
            idempotencyKey,
            transfer.id,
            actorType.id
        ))

        val createdTransferRequest = transferRequestRepository.getByIdOrThrow(createTransferRequestProjection.id)
        assertThat(createdTransferRequest.transfer).isEqualTo(transfer)
        assertThat(createdTransferRequest.actorType).isEqualTo(actorType)
        assertThat(createdTransferRequest.requestBody).isEqualTo(requestBody)
        assertThat(createdTransferRequest.idempotencyKey).isEqualTo(idempotencyKey)
    }

    @Test
    fun `Do not create transfer request if request with same idempotency key exist`() {
        val transfer = TransferEntity(status = status)
        transferRepository.save(transfer)
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)

        val createdEntity = transferRequestCommandService.create(TransferRequestCommand.Create(
            requestBody,
            idempotencyKey,
            transfer.id,
            actorType.id
        ))

        val projectionWithSameIdempotencyKey = transferRequestCommandService.create(TransferRequestCommand.Create(
            requestBody,
            idempotencyKey,
            transfer.id,
            actorType.id
        ))

        assertThat(createdEntity.created).isTrue
        assertThat(projectionWithSameIdempotencyKey.created).isFalse
    }
}
