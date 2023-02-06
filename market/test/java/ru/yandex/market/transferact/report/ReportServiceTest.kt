package ru.yandex.market.transferact.report

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.yandex.market.transferact.entity.actor.Actor
import ru.yandex.market.transferact.entity.actor.ActorType
import ru.yandex.market.transferact.entity.actor.apiKey
import ru.yandex.market.transferact.entity.document.Document
import ru.yandex.market.transferact.entity.document.DocumentType
import ru.yandex.market.transferact.entity.item.Item
import ru.yandex.market.transferact.entity.operation.Operation
import ru.yandex.market.transferact.entity.operation.OperationStatus
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.operation.item.OperationItemStatus
import ru.yandex.market.transferact.entity.signature.Signature
import ru.yandex.market.transferact.entity.transfer.Transfer
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ReportServiceTest {
    lateinit var reportService: ReportService

    @BeforeEach
    fun setUp() {
        reportService = ReportService()
    }

    @Disabled
    @Test
    fun `When getTransferActPdf then return report`() {

        val actorTypeCourier = ActorType(1, "MARKET_COURIER", apiKey, "q", 1)
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
            Item(
                id = 1,
                externalId = "7",
                declaredCost = BigDecimal.valueOf(1000),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "8",
                declaredCost = BigDecimal.valueOf(123),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "9",
                declaredCost = BigDecimal.valueOf(7459),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "10",
                declaredCost = BigDecimal.valueOf(92305),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "11",
                declaredCost = BigDecimal.valueOf(50),
                placeCount = 3,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "12",
                declaredCost = BigDecimal.valueOf(90),
                placeCount = 8,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "13",
                declaredCost = BigDecimal.valueOf(1000),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "14",
                declaredCost = BigDecimal.valueOf(123),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "15",
                declaredCost = BigDecimal.valueOf(7459),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "16",
                declaredCost = BigDecimal.valueOf(92305),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "17",
                declaredCost = BigDecimal.valueOf(50),
                placeCount = 3,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "18",
                declaredCost = BigDecimal.valueOf(90),
                placeCount = 8,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "19",
                declaredCost = BigDecimal.valueOf(1000),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "20",
                declaredCost = BigDecimal.valueOf(123),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "21",
                declaredCost = BigDecimal.valueOf(7459),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "22",
                declaredCost = BigDecimal.valueOf(92305),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "23",
                declaredCost = BigDecimal.valueOf(50),
                placeCount = 3,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "24",
                declaredCost = BigDecimal.valueOf(90),
                placeCount = 8,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "25",
                declaredCost = BigDecimal.valueOf(1000),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "26",
                declaredCost = BigDecimal.valueOf(123),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "27",
                declaredCost = BigDecimal.valueOf(7459),
                placeCount = 2,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "28",
                declaredCost = BigDecimal.valueOf(92305),
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "29",
                declaredCost = BigDecimal.valueOf(50),
                placeCount = 3,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "30",
                declaredCost = BigDecimal.valueOf(90),
                placeCount = 8,
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                externalId = "LOT_1",
                type = "LOT",
                placeCount = 1,
                status = OperationItemStatus.RECEIVED
            )
        )
        val signature = Signature(
            id = 1,
            signerId = "test",
            signerName = "Андреева Ксения Владимировна",
            signatureData = "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
            operationId = 1,
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
            signerName = "Никитин Егор Никитич",
            signatureData = "8d23cf6c86e834a7aa6eded54c26ce2bb2e74903538c61bdd5d2197997ab2f72",
            operationId = 2,
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
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )

        val document = Document(
            1,
            "null.pdf",
            DocumentType.TRANSFER_ACT,
            23780
        )

        val tmp: File = File.createTempFile("test", ".pdf")
        Files.write(tmp.toPath(), reportService.getTransferActPdf(transfer, document))
        println(tmp.toPath())
    }


    @Disabled
    @Test
    fun `When getDiscrepancyActXlsx then return report`() {
        val actorTypeSc = ActorType(1, "MARKET_SC", apiKey, "sc", 1)
        val actorTypeMagistral = ActorType(2, "MARKET_MAGISTRAL", "apiKey1", "m", 2)

        val actorSc1 = Actor(
            id = 1,
            name = "SC1",
            externalId = "111",
            actorType = actorTypeSc
        )
        val actorMagistral = Actor(id = 2, name = "MAGISTRAL", externalId = "222", actorType = actorTypeMagistral)
        val actorSc2 = Actor(
            id = 3,
            name = "SC2",
            externalId = "333",
            actorType = actorTypeSc
        )

        val magistralReceiveItems = mutableSetOf(
            Item(
                id = 1,
                placeId = "order-1-place-1",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                placeId = "order-1-place-2",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 3,
                placeId = "order-2-place-1",
                externalId = "order-2",
                status = OperationItemStatus.RECEIVED
            )
        )

        val outboundTransfer = createTransfer(1, actorFrom = actorSc1, actorTo = actorMagistral, magistralReceiveItems)

        val magistralProvideItems = mutableSetOf(
            Item(
                id = 1,
                placeId = "order-1-place-1",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 3,
                placeId = "order-2-place-1",
                externalId = "order-2",
                status = OperationItemStatus.RECEIVED
            )
        )

        val inboundTransfer = createTransfer(2, actorFrom = actorMagistral, actorTo = actorSc2, magistralProvideItems)

        Files.write(
            Paths.get("Discrepancy_act_test.xlsx"),
            reportService.getDiscrepancyActXlsx(inboundTransfer = inboundTransfer, outboundTransfer = outboundTransfer)
        )
    }

    @Disabled
    @Test
    fun `When getDiscrepancyActXlsx for shop then return report`() {
        val actorTypeSc = ActorType(1, "MARKET_SC", apiKey, "sc", 1)
        val actorTypeShop = ActorType(2, "MARKET_SHOP", "apiKey1", "s", 2)

        val actorSc = Actor(
            id = 1,
            name = "SC1",
            externalId = "111",
            actorType = actorTypeSc
        )
        val actorShop = Actor(id = 2, name = "дропшип", externalId = "222", actorType = actorTypeShop)
        val actorShop2 = Actor(id = 3, name = "дропшип", externalId = "222", actorType = actorTypeShop)

        val shopReceiveItems = mutableSetOf(
            Item(
                id = 1,
                placeId = "order-1-place-1",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 2,
                placeId = "order-1-place-2",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 3,
                placeId = "order-2-place-1",
                externalId = "order-2",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 3,
                placeId = "order-2-place-2",
                externalId = "order-2",
                status = OperationItemStatus.RECEIVED
            )
        )

        val outboundTransfer = createTransfer(1, actorFrom = actorShop, actorTo = actorShop2, shopReceiveItems)

        val shopProvideItems = mutableSetOf(
            Item(
                id = 1,
                placeId = "order-1-place-1",
                externalId = "order-1",
                status = OperationItemStatus.RECEIVED
            ),
            Item(
                id = 3,
                placeId = "order-2-place-1",
                externalId = "order-2",
                status = OperationItemStatus.RECEIVED
            )
        )

        val inboundTransfer = createTransfer(2, actorFrom = actorShop2, actorTo = actorSc, shopProvideItems)

        Files.write(
            Paths.get("Discrepancy_act_test.xlsx"),
            reportService.getDiscrepancyActXlsx(inboundTransfer = inboundTransfer, outboundTransfer = outboundTransfer)
        )
    }

    private fun createTransfer(id: Long, actorFrom: Actor, actorTo: Actor, items: Set<Item>): Transfer {
        val actorToSignature = Signature(
            id = id,
            signerId = actorTo.id.toString(),
            signerName = actorTo.name,
            signatureData = actorTo.externalId,
            operationId = 1,
        )
        val actorToReceiveOperation = Operation(
            id = id,
            status = OperationStatus.CREATED,
            type = OperationType.RECEIVE,
            actor = actorTo,
            operationItems = items,
            operationSignatures = listOf(actorToSignature)
        )

        val actorFromSignature = Signature(
            id = id + 2,
            signerId = actorFrom.id.toString(),
            signerName = actorFrom.name,
            signatureData = actorFrom.externalId,
            operationId = 2,
        )
        val actorFromProvideOperation = Operation(
            id = id + 2,
            status = OperationStatus.CREATED,
            type = OperationType.PROVIDE,
            actor = actorFrom,
            operationItems = items,
            operationSignatures = listOf(actorFromSignature)
        )

        val localDateTime: LocalDateTime = LocalDateTime.of(2022, Month.MARCH, 3, 12, 0, 0)

        return Transfer(
            id = id,
            status = TransferStatus.CLOSED,
            operationProvide = actorFromProvideOperation,
            operationReceive = actorToReceiveOperation,
            closedAt = localDateTime.atOffset(ZoneOffset.of("+03:00")),
            localDate = localDateTime.toLocalDate()
        )
    }
}
