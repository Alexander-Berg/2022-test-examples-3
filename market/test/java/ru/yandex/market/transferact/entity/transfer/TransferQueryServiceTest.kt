package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.common.util.exception.TransferActEntityNotFoundException
import ru.yandex.market.transferact.entity.actor.ActorEntity
import ru.yandex.market.transferact.entity.actor.ActorId
import ru.yandex.market.transferact.entity.actor.ActorRepository
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.actor.name
import ru.yandex.market.transferact.entity.item.ItemEntity
import ru.yandex.market.transferact.entity.item.ItemIdentifier
import ru.yandex.market.transferact.entity.item.ItemRepository
import ru.yandex.market.transferact.entity.operation.OperationEntity
import ru.yandex.market.transferact.entity.operation.OperationMapper
import ru.yandex.market.transferact.entity.operation.OperationRepository
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.operation.item.OperationItemEntity
import ru.yandex.market.transferact.entity.signature.SignatureEntity
import ru.yandex.market.transferact.entity.signature.SignatureRepository
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class TransferQueryServiceTest : AbstractTest() {
    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var transferQueryService: TransferQueryService

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var operationRepository: OperationRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var signatureRepository: SignatureRepository

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    fun `When getTransferById then return transfer`() {
        val transfer = TransferEntity(
            status = status,
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )
        transferRepository.save(transfer)

        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val actor = ActorEntity()
        val externalId = "123"
        val actorId = ActorId(externalId)
        actorId.actorType = actorType
        actor.actorId = actorId
        actorRepository.save(actor)

        val operation = OperationEntity()
        operation.type = OperationType.PROVIDE
        operation.transfer = transfer
        operation.actor = actor
        operationRepository.save(operation)

        val operation1 = OperationEntity()
        operation1.type = OperationType.RECEIVE
        operation1.transfer = transfer
        operation1.actor = actor
        operationRepository.save(operation1)

        val found = transferQueryService.getTransferById(transfer.id)

        val transferProjection = Transfer(
            transfer.id,
            transfer.status,
            OperationMapper.mapToProjection(operation),
            OperationMapper.mapToProjection(operation1),
            transfer.closedAt?.atOffset(ZoneOffset.of("+03:00")),
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )

        Assertions.assertThat(found).isNotNull
        Assertions.assertThat(found).isEqualTo(transferProjection)
    }

    @Test
    fun `When getActorTypeByApiKey throw exception`() {
        Assertions.assertThatThrownBy {
            transferQueryService.getTransferById(1)
        }.isInstanceOf(TransferActEntityNotFoundException::class.java)
    }

    @Test
    fun `When getActiveTransferForActorPair then return transfer`() {

        val transfer = TransferEntity(
            status = status,
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )
        transferRepository.save(transfer)

        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val actor = ActorEntity()
        val externalId = "123"
        val actorId = ActorId(externalId)
        actorId.actorType = actorType
        actor.actorId = actorId
        actorRepository.save(actor)

        val actor1 = ActorEntity()
        val actorId1 = ActorId("2")
        actorId1.actorType = actorType
        actor1.actorId = actorId1
        actorRepository.save(actor1)

        val operation = OperationEntity()
        operation.type = OperationType.PROVIDE
        operation.transfer = transfer
        operation.actor = actor
        operationRepository.save(operation)
        val signature = SignatureEntity(signerId = "", signerName = "", signatureData = "")
        signature.operation = operation
        signatureRepository.save(signature)
        operation.signatures.add(signature)

        val operation1 = OperationEntity()
        operation1.type = OperationType.RECEIVE
        operation1.transfer = transfer
        operation1.actor = actor1

        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = "12",
                placeId = null
            ),
        )
        itemRepository.save(item)
        val operationItemEntity = OperationItemEntity()
        operationItemEntity.item = item
        operationItemEntity.operation = operation1
        operation1.operationItems = mutableSetOf(operationItemEntity)
        operationRepository.save(operation1)
        val transferProjection =
            Transfer(
                transfer.id,
                transfer.status,
                OperationMapper.mapToProjection(operation),
                OperationMapper.mapToProjection(operation1),
                transfer.closedAt?.atOffset(ZoneOffset.of("+03:00")),
                localDate = LocalDate.of(2021, Month.DECEMBER, 3)
            )

        val found = transferQueryService.getActiveTransferForActorPair(actor.id, actor1.id)
        Assertions.assertThat(found).isNotNull
        Assertions.assertThat(found).isEqualTo(transferProjection)
    }

    @Test
    fun `When getActiveTransferForActorPair then throw exception`() {
        Assertions.assertThatThrownBy {
            transferQueryService.getActiveTransferForActorPair(1L, 2L)
        }.isInstanceOf(TransferActEntityNotFoundException::class.java)
    }

    @Test
    fun `When getActiveTransfersForClosing then return transfers`() {
        val transfer = testOperationHelper.createTransfer()

        val actorCourier =
            testOperationHelper.createActor(name = "Кошелев Владимир Александрович", companyName = "ООО \"Воробушек\"")
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124", "ООО \"Интер\"")

        val operationReceive = testOperationHelper.createOperation(
            transfer = transfer, actor = actorCourier
        )

        val operationProvide = testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf()
        )

        Assertions.assertThat(
            transferQueryService.getActiveTransfersByUpdatedAt(
                Instant.now().plus(10L, ChronoUnit.MINUTES)
            )
        ).isEqualTo(listOf(TransferMapper.mapToProjection(transfer, operationProvide, operationReceive)))
    }

    @Test
    fun `When findAllByTransportationId then return transfers`() {
        val transfer = TransferEntity(
            status = TransferStatus.CLOSED,
            localDate = LocalDate.of(2021, Month.DECEMBER, 3),
            transportationId = transportationId,
        )
        transferRepository.save(transfer)

        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val actor = ActorEntity()
        val externalId = "123"
        val actorId = ActorId(externalId)
        actorId.actorType = actorType
        actor.actorId = actorId
        actorRepository.save(actor)

        val operation = OperationEntity()
        operation.type = OperationType.PROVIDE
        operation.transfer = transfer
        operation.actor = actor
        operationRepository.save(operation)

        val operation1 = OperationEntity()
        operation1.type = OperationType.RECEIVE
        operation1.transfer = transfer
        operation1.actor = actor
        operationRepository.save(operation1)

        val found = transferQueryService.getTransfersByTransportationId(transportationId)

        val transferProjection = Transfer(
            transfer.id,
            transfer.status,
            OperationMapper.mapToProjection(operation),
            OperationMapper.mapToProjection(operation1),
            transfer.closedAt?.atOffset(ZoneOffset.of("+03:00")),
            localDate = LocalDate.of(2021, Month.DECEMBER, 3),
            transportationId = transportationId
        )

        Assertions.assertThat(found).containsExactly(transferProjection)
    }
}
