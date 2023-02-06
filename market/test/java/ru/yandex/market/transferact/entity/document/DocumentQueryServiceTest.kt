package ru.yandex.market.transferact.entity.document

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.item.transfer.log.ItemTransferLogEntity
import ru.yandex.market.transferact.entity.item.transfer.log.ItemTransferLogRepository
import ru.yandex.market.transferact.entity.item.transfer.log.ItemTransferLogViewNaturalKey
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.time.Instant

class DocumentQueryServiceTest : AbstractTest() {
    @Autowired
    lateinit var documentQueryService: DocumentQueryService

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Autowired
    lateinit var itemTransferLogRepository: ItemTransferLogRepository

    @Test
    fun `When getDocumentsByExternalId then return documents`() {
        val transfer = testOperationHelper.createTransfer(TransferStatus.CLOSED, closedAt = Instant.now())

        val actorCourier =
            testOperationHelper.createActor(name = "Кошелев Владимир Александрович", companyName = "ООО \"Воробушек\"")
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124", "ООО \"Интер\"")

        testOperationHelper.createOperation(
            transfer = transfer, actor = actorCourier
        )

        testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf()
        )

        val document1 = DocumentMapper.mapToProjection(
            testOperationHelper.createDocument(
                link = "document1.pdf",
                transfer = transfer
            )
        )
        val document2 = DocumentMapper.mapToProjection(
            testOperationHelper.createDocument(
                link = "document2.pdf",
                transfer = transfer
            )
        )

        itemTransferLogRepository.saveAll(
            listOf(
                ItemTransferLogEntity(
                    ItemTransferLogViewNaturalKey(1L, actorCourier.id, transfer.id, OperationType.PROVIDE),
                    document1.link,
                    transfer.closedAt,
                    actorCourier.actorId.actorType.name,
                    actorCourier.actorId.externalId,
                    "item2"
                ),
                ItemTransferLogEntity(
                    ItemTransferLogViewNaturalKey(2L, actorCourier.id, transfer.id, OperationType.PROVIDE),
                    document2.link,
                    transfer.closedAt,
                    actorCourier.actorId.actorType.name,
                    actorCourier.actorId.externalId,
                    "item2"
                )
            )
        )

        Assertions.assertThat(documentQueryService.getDocumentsByExternalId("item2"))
            .isEqualTo(setOf(document1.link, document2.link))
    }

    @Test
    fun `When getDocumentsByExternalId then return empty set`() {
        Assertions.assertThat(documentQueryService.getDocumentsByExternalId("123")).isEqualTo(setOf<String>())
    }
}
