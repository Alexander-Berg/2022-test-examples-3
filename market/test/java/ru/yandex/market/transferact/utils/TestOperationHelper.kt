package ru.yandex.market.transferact.utils

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import ru.yandex.market.transferact.entity.actor.ActorEntity
import ru.yandex.market.transferact.entity.actor.ActorId
import ru.yandex.market.transferact.entity.actor.ActorRepository
import ru.yandex.market.transferact.entity.actor.ActorTypeEntity
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.actor.actorExternalId
import ru.yandex.market.transferact.entity.document.DocumentEntity
import ru.yandex.market.transferact.entity.document.DocumentRepository
import ru.yandex.market.transferact.entity.document.DocumentType
import ru.yandex.market.transferact.entity.item.ItemEntity
import ru.yandex.market.transferact.entity.item.ItemIdentifier
import ru.yandex.market.transferact.entity.item.ItemRepository
import ru.yandex.market.transferact.entity.item.orderType
import ru.yandex.market.transferact.entity.operation.OperationEntity
import ru.yandex.market.transferact.entity.operation.OperationRepository
import ru.yandex.market.transferact.entity.operation.OperationStatus
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.operation.item.OperationItemEntity
import ru.yandex.market.transferact.entity.operation.item.OperationItemStatus
import ru.yandex.market.transferact.entity.signature.SignatureEntity
import ru.yandex.market.transferact.entity.signature.SignatureRepository
import ru.yandex.market.transferact.entity.transfer.TransferEntity
import ru.yandex.market.transferact.entity.transfer.TransferRepository
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import javax.persistence.EntityManager

@Component
class TestOperationHelper {

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
    lateinit var signatureRepository: SignatureRepository

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var entityManager: EntityManager

    fun createOperation(
        operationStatus: OperationStatus = OperationStatus.CREATED,
        operationType: OperationType = OperationType.RECEIVE,
        transfer: TransferEntity = createTransfer(),
        actor: ActorEntity = createActor(),
        items: Map<ItemEntity, OperationItemStatus> = mapOf(
            Pair(createItem("item1"), OperationItemStatus.SKIPPED),
            Pair(createItem("item2"), OperationItemStatus.RECEIVED)
        )
    ): OperationEntity {
        val operation = OperationEntity(status = operationStatus)
        operation.type = operationType
        operation.transfer = transfer
        operation.actor = actor
        val operationItems = ArrayList<OperationItemEntity>()
        items.forEach {
            val operationItem = OperationItemEntity()
            operationItem.operation = operation
            operationItem.item = it.key
            operationItem.status = it.value
            operationItems.add(operationItem)
        }
        if (operationItems.isNotEmpty())
            operation.operationItems = operationItems.toSet() as MutableSet<OperationItemEntity>
        return operationRepository.save(operation)
    }

    fun createActor(
        externalId: String = actorExternalId,
        actorTypeName: String = "MARKET_COURIER",
        apiKey: String = "apiKey",
        signatureRequestCallbackUrl: String = "",
        name: String = "",
        companyName: String = ""
    ): ActorEntity {
        val actorType = ActorTypeEntity(
            name = actorTypeName,
            apiKey = apiKey,
            signatureRequestCallbackUrl = signatureRequestCallbackUrl
        )
        actorTypeRepository.save(actorType)
        val actorId = ActorId(externalId)
        actorId.actorType = actorType
        val actor = ActorEntity(name = name, companyName = companyName)
        actor.actorId = actorId
        return actorRepository.save(actor)
    }

    fun createActorType(
        actorTypeName: String = "COURIER",
        apiKey: String = "apiKey",
        signatureRequestCallbackUrl: String? = null,
        tvmClientId: Int? = null
    ) {
        val actorType = ActorTypeEntity(
            name = actorTypeName,
            apiKey = apiKey,
            signatureRequestCallbackUrl = signatureRequestCallbackUrl,
            tvmClientId = tvmClientId
        )
        actorTypeRepository.save(actorType)
    }

    fun createTransfer(
        status: TransferStatus = TransferStatus.CREATED,
        localDate: LocalDate? = LocalDate.now(),
        closedAt: Instant? = null
    ): TransferEntity {
        val transfer = TransferEntity(status = status, localDate = localDate!!)
        transfer.closedAt = closedAt
        return transferRepository.save(transfer)
    }

    fun createItem(
        externalId: String,
        declaredCost: BigDecimal = BigDecimal.TEN,
        placeCount: Int = 1
    ): ItemEntity {
        val item = ItemEntity(
            itemIdentifier = ItemIdentifier(
                externalId = externalId,
                placeId = null
            ),
            type = orderType,
            declaredCost = declaredCost,
            placeCount = placeCount
        )
        return itemRepository.save(item)
    }

    fun createSignature(
        signerId: String = "",
        signerName: String = "",
        signatureDate: String = "",
        operation: OperationEntity = createOperation()
    ): SignatureEntity {
        val signature = SignatureEntity(signerId = signerId, signerName = signerName, signatureData = signatureDate)
        signature.operation = operation
        return signatureRepository.save(signature)
    }

    fun createDocument(link: String = "null.pdf", transfer: TransferEntity = createTransfer()): DocumentEntity {
        val document = DocumentEntity(link = link, type = DocumentType.TRANSFER_ACT)
        document.transfer = transfer
        return documentRepository.save(document)
    }
}
