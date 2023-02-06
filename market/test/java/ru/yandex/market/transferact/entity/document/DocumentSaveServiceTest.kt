package ru.yandex.market.transferact.entity.document

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.transfer.TransferMapper
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset

class DocumentSaveServiceTest : AbstractTest() {
    @Autowired
    lateinit var documentSaveService: DocumentSaveService

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    fun `when saveDocument then return document`() {
        val localDateTime = LocalDateTime.of(2022, Month.FEBRUARY, 3, 10, 0)
        val transferEntity = testOperationHelper.createTransfer(
            status = TransferStatus.CLOSED,
            closedAt = localDateTime.toInstant(ZoneOffset.ofHours(+3)),
            localDate = localDateTime.toLocalDate()
        )

        val actorCourier =
            testOperationHelper.createActor(name = "Кошелев Владимир Александрович", companyName = "ООО \"Воробушек\"")
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124", "ООО \"Интер\"")

        val operationReceive = testOperationHelper.createOperation(
            transfer = transferEntity, actor = actorCourier, items = mapOf()
        )

        val operationProvide = testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transferEntity,
            actor = actorWarehouse

        )

        val transfer = TransferMapper.mapToProjection(transferEntity, operationProvide, operationReceive)

        var actual = documentSaveService.saveDocument(transfer, DocumentType.TRANSFER_ACT)
        var expected = Document(actual.id, actual.link, DocumentType.TRANSFER_ACT, 1)
        Assertions.assertThat(actual).isEqualTo(expected)
        Assertions.assertThat(actual.link).containsPattern("АПП_2022-02-03_1.+pdf")

        actual = documentSaveService.saveDocument(transfer, DocumentType.TRANSFER_ACT)
        expected = Document(actual.id, actual.link, DocumentType.TRANSFER_ACT, 2)
        Assertions.assertThat(actual).isEqualTo(expected)
        Assertions.assertThat(actual.link).containsPattern("АПП_2022-02-03_2.+pdf")

        actual = documentSaveService.saveDocument(transfer, DocumentType.DISCREPANCY_ACT)
        expected = Document(actual.id, actual.link, DocumentType.DISCREPANCY_ACT, 1)
        Assertions.assertThat(actual).isEqualTo(expected)
        Assertions.assertThat(actual.link).containsPattern("Акт_расхождений_2022-02-03_1.+xlsx")
    }
}
