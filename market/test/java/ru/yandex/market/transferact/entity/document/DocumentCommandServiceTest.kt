package ru.yandex.market.transferact.entity.document

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.common.util.exception.TransferActEntityNotFoundException
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.utils.TestOperationHelper

class DocumentCommandServiceTest : AbstractTest() {

    @Autowired
    lateinit var documentCommandService: DocumentCommandService

    @Autowired
    lateinit var documentRepository: DocumentRepository

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    fun `When create document and transfer not exist then throw exception`() {
        Assertions.assertThatThrownBy {
            documentCommandService.create(
                DocumentCommand.Create(
                    "",
                    1L,
                    DocumentType.TRANSFER_ACT,
                    1
                )
            )
        }.isInstanceOf(TransferActEntityNotFoundException::class.java)
    }

    @Test
    fun `When create document then return created document`() {
        val transfer = testOperationHelper.createTransfer()

        val actorCourier = testOperationHelper.createActor()
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124")

        testOperationHelper.createOperation(transfer = transfer, actor = actorCourier)

        testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf()
        )

        val createdDocument = documentCommandService.create(
            DocumentCommand.Create(
                "dev/null",
                transfer.id,
                DocumentType.TRANSFER_ACT,
                1
            )
        )

        val document = documentRepository.getByIdOrThrow(createdDocument.id)
        Assertions.assertThat(document.transfer).isEqualTo(transfer)
        Assertions.assertThat(document.link).isEqualTo("dev/null")
    }

    @Test
    fun `When create document with same link then return throw exception`() {

        val transfer = testOperationHelper.createTransfer()

        val actorCourier = testOperationHelper.createActor()
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124")

        testOperationHelper.createOperation(transfer = transfer, actor = actorCourier)

        testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf()
        )

        documentCommandService.create(
            DocumentCommand.Create(
                "dev/null",
                transfer.id,
                DocumentType.TRANSFER_ACT,
                1
            )
        )
        Assertions.assertThatThrownBy {
            documentCommandService.create(
                DocumentCommand.Create(
                    "dev/null",
                    transfer.id,
                    DocumentType.TRANSFER_ACT,
                    1
                )
            )
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
