package ru.yandex.market.transferact.entity.operation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.ActorEntity
import ru.yandex.market.transferact.entity.actor.ActorId
import ru.yandex.market.transferact.entity.actor.ActorRepository
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.actor.actorExternalId
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.actor.name
import ru.yandex.market.transferact.entity.item.ItemEntity
import ru.yandex.market.transferact.entity.item.ItemIdentifier
import ru.yandex.market.transferact.entity.item.ItemRepository
import ru.yandex.market.transferact.entity.item.orderType
import ru.yandex.market.transferact.entity.operation.item.OperationItemEntity
import ru.yandex.market.transferact.entity.transfer.TransferEntity
import ru.yandex.market.transferact.entity.transfer.TransferRepository
import ru.yandex.market.transferact.entity.transfer.status
import java.math.BigDecimal

class OperationRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var actorRepository: ActorRepository

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var itemRepository: ItemRepository

    @Autowired
    lateinit var operationRepository: OperationRepository

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    private lateinit var actor: ActorEntity
    private lateinit var transfer: TransferEntity
    private lateinit var item1: ItemEntity
    private lateinit var item2: ItemEntity

    @BeforeEach
    fun init() {
        actor = createActor()
        transfer = createTransfer()
        item1 = createItem("item1")
        item2 = createItem("item2")
    }

    @Test
    fun `When getById then return operation`() {
        var operation = OperationEntity()
        operation.transfer = transfer
        operation.type = OperationType.RECEIVE
        operation.actor = actor
        val operationItem1 = OperationItemEntity()
        operationItem1.operation = operation
        operationItem1.item = item1

        val operationItem2 = OperationItemEntity()
        operationItem2.operation = operation
        operationItem2.item = item2
        operation.operationItems = mutableSetOf(operationItem1, operationItem2)
        operation = operationRepository.save(operation)

        transactionTemplate.execute {
            val found = operationRepository.getById(operation.getId())

            assertThat(found).isNotNull
            assertThat(found?.actor).isEqualTo(actor)
            assertThat(found?.transfer).isEqualTo(transfer)
            assertThat(found?.operationItems).hasSize(2)
            assertThat(found?.operationItems?.map { operationItem -> operationItem.item }).containsExactlyInAnyOrder(
                item1,
                item2
            )
        }
    }

    private fun createActor(): ActorEntity {
        val actorType = ActorTypeEntity(name = name, apiKey = apiKey)
        actorTypeRepository.save(actorType)
        val actorId = ActorId(actorExternalId)
        actorId.actorType = actorType
        val actor = ActorEntity()
        actor.actorId = actorId
        return actorRepository.save(actor)
    }

    private fun createTransfer(): TransferEntity {
        val transfer = TransferEntity(status = status)
        return transferRepository.save(transfer)
    }

    private fun createItem(externalId: String): ItemEntity {
        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = externalId,
                placeId = null
            ),
            type = orderType,
            declaredCost = BigDecimal.TEN,
            placeCount = 1
        )
        return itemRepository.save(item)
    }
}
