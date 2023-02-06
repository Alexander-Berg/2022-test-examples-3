package ru.yandex.market.transferact.entity.document

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.Actor
import ru.yandex.market.transferact.entity.actor.ActorType
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.item.Item
import ru.yandex.market.transferact.entity.operation.Operation
import ru.yandex.market.transferact.entity.operation.OperationStatus
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.signature.Signature
import ru.yandex.market.transferact.entity.transfer.Transfer
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

class DocumentUploadServiceTest : AbstractTest() {
    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Autowired
    lateinit var documentUploadService: DocumentUploadService

    @Test
    @Disabled
    fun `When upload document then return document`() {
        val actorTypeCourier = ActorType(1, "COURIER", apiKey, "q", 1)
        val actorTypeWarehouse = ActorType(2, "WAREHOUSE", "apiKey1", "w", 2)
        val actorCourier = Actor(
            id = 1,
            name = "Кошелев Владимир Александрович",
            companyName = "ООО \"Воробушек\"",
            externalId = "123",
            actorType = actorTypeCourier
        )

        val actorWarehouse = Actor(id = 2, name = "ООО \"Интер\"", externalId = "321", actorType = actorTypeWarehouse)

        val itemsOperation = mutableSetOf(
            Item(id = 1, externalId = "7", declaredCost = BigDecimal.valueOf(1000), placeCount = 2),
            Item(id = 2, externalId = "8", declaredCost = BigDecimal.valueOf(123), placeCount = 1),
            Item(id = 2, externalId = "9", declaredCost = BigDecimal.valueOf(7459), placeCount = 2),
            Item(id = 2, externalId = "10", declaredCost = BigDecimal.valueOf(92305), placeCount = 1),
            Item(id = 2, externalId = "11", declaredCost = BigDecimal.valueOf(50), placeCount = 3),
            Item(id = 2, externalId = "12", declaredCost = BigDecimal.valueOf(90), placeCount = 8),
            Item(id = 2, externalId = "13", declaredCost = BigDecimal.valueOf(1000), placeCount = 2),
            Item(id = 2, externalId = "14", declaredCost = BigDecimal.valueOf(123), placeCount = 1),
            Item(id = 2, externalId = "15", declaredCost = BigDecimal.valueOf(7459), placeCount = 2),
            Item(id = 2, externalId = "16", declaredCost = BigDecimal.valueOf(92305), placeCount = 1),
            Item(id = 2, externalId = "17", declaredCost = BigDecimal.valueOf(50), placeCount = 3),
            Item(id = 2, externalId = "18", declaredCost = BigDecimal.valueOf(90), placeCount = 8),
            Item(id = 2, externalId = "19", declaredCost = BigDecimal.valueOf(1000), placeCount = 2),
            Item(id = 2, externalId = "20", declaredCost = BigDecimal.valueOf(123), placeCount = 1),
            Item(id = 2, externalId = "21", declaredCost = BigDecimal.valueOf(7459), placeCount = 2),
            Item(id = 2, externalId = "22", declaredCost = BigDecimal.valueOf(92305), placeCount = 1),
            Item(id = 2, externalId = "23", declaredCost = BigDecimal.valueOf(50), placeCount = 3),
            Item(id = 2, externalId = "24", declaredCost = BigDecimal.valueOf(90), placeCount = 8),
            Item(id = 2, externalId = "25", declaredCost = BigDecimal.valueOf(1000), placeCount = 2),
            Item(id = 2, externalId = "26", declaredCost = BigDecimal.valueOf(123), placeCount = 1),
            Item(id = 2, externalId = "27", declaredCost = BigDecimal.valueOf(7459), placeCount = 2),
            Item(id = 2, externalId = "28", declaredCost = BigDecimal.valueOf(92305), placeCount = 1),
            Item(id = 2, externalId = "29", declaredCost = BigDecimal.valueOf(50), placeCount = 3),
            Item(id = 2, externalId = "30", declaredCost = BigDecimal.valueOf(90), placeCount = 8)
        )
        val signature = Signature(
            id = 1,
            signerId = "test",
            signerName = "test",
            signatureData = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
            operationId = 1
        )
        val operation = Operation(
            id = 1,
            status = OperationStatus.CREATED,
            type = OperationType.RECEIVE,
            actor = actorCourier,
            operationItems = itemsOperation,
            operationSignatures = listOf(signature)
        )

        val signature1 = Signature(
            id = 2,
            signerId = "test",
            signerName = "test",
            signatureData = "8d23cf6c86e834a7aa6eded54c26ce2bb2e74903538c61bdd5d2197997ab2f72",
            operationId = 2
        )
        val operation1 = Operation(
            id = 2,
            status = OperationStatus.CREATED,
            type = OperationType.PROVIDE,
            actor = actorWarehouse,
            operationItems = setOf(),
            operationSignatures = listOf(signature1)
        )

        val transfer = Transfer(
            id = 1,
            status = TransferStatus.CLOSED,
            operationProvide = operation1,
            operationReceive = operation,
            closedAt = OffsetDateTime.of(LocalDateTime.now().plusHours(10), ZoneOffset.of("+04:00")),
            localDate = LocalDate.now()
        )

        val document = Document(
            1,
            "null.pdf",
            DocumentType.TRANSFER_ACT,
            23780
        )

        documentUploadService.uploadDocument(transfer, document)
    }
}
