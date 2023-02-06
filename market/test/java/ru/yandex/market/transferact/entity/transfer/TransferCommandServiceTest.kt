package ru.yandex.market.transferact.entity.transfer

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.dbqueue.QueueType
import ru.yandex.market.transferact.entity.actor.ActorTypeRepository
import ru.yandex.market.transferact.entity.operation.OperationRepository
import ru.yandex.market.transferact.entity.operation.item.OperationItemEntity
import ru.yandex.market.transferact.entity.operation.item.OperationItemStatus
import ru.yandex.market.transferact.entity.signature.SignatureRepository
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.Month

class TransferCommandServiceTest : AbstractTest() {

    companion object {
        const val signerId = "signerId"
        const val signerName = "signerName"
        const val signatureData = "signatureData"
    }

    @Autowired
    lateinit var transferRepository: TransferRepository

    @Autowired
    lateinit var actorTypeRepository: ActorTypeRepository

    @Autowired
    lateinit var transferCommandService: TransferCommandService

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Autowired
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var operationRepository: OperationRepository

    @Autowired
    lateinit var signatureRepository: SignatureRepository

    @Autowired
    lateinit var dbQueueTestUtil: DbQueueTestUtil

    private val instant: Instant = Instant.ofEpochMilli(1640167604)

    @Test
    fun `create transfer happy path`() {
        val actorFrom = testOperationHelper.createActor("actorFrom", "SC", "apiKey1")
        val actorTo = testOperationHelper.createActor("actorTo", "COURIER", "apiKey2")

        val item1 = testOperationHelper.createItem("item1")
        val item2 = testOperationHelper.createItem("item2")

        val transferProjection = transferCommandService.create(TransferCommand.Create(
            actorFrom.id,
            actorTo.id,
            mapOf(Pair(item1.id, OperationItemStatus.SKIPPED), Pair(item2.id, OperationItemStatus.RECEIVED)),
            localDate = LocalDate.of(2021, Month.DECEMBER,  3),
            null
        ))

        transactionTemplate.execute {
            val transfer = transferRepository.getByIdOrThrow(transferProjection.id)
            assertThat(transfer.status).isEqualTo(TransferStatus.CREATED)
            val operations = transfer.operations
            assertThat(operations).hasSize(2)
            assertThat(operations.elementAt(0).operationItems.map(OperationItemEntity::item))
                .containsExactlyInAnyOrder(item1, item2)
            assertThat(operations.elementAt(1).operationItems.map(OperationItemEntity::item))
                .containsExactlyInAnyOrder(item1, item2)
        }
    }

    @Test
    fun `When create signature without operation then throw exception`() {
        Assertions.assertThatThrownBy {
            transferCommandService.createSignature(
                TransferCommand.CreateSignature(
                    1L,
                    1L,
                    signerId,
                    signerName,
                    signatureData,
                    instant
                )
            )
        }
    }

    @Test
    fun `When create signature then return created signature`() {
        var operation = testOperationHelper.createOperation()

        transferCommandService.createSignature(
            TransferCommand.CreateSignature(
                operation.transfer.id,
                operation.id,
                signerId,
                signerName,
                signatureData,
                instant
            )
        )

        operation = operationRepository.getByIdOrThrow(operation.id)
        val signatureEntity = operation.signatures.first()
        assertThat(signatureEntity.signerId).isEqualTo(signerId)
        assertThat(signatureEntity.signerName).isEqualTo(signerName)
        assertThat(signatureEntity.signatureData).isEqualTo(signatureData)
        assertThat(signatureEntity.operation).isEqualTo(operation)
        assertThat(signatureRepository.getById(signatureEntity.id)).isNotNull
    }

    @Test
    fun `Delete signature happy path`() {
        var operation = testOperationHelper.createOperation()
        transferCommandService.createSignature(
            TransferCommand.CreateSignature(
                operation.transfer.id,
                operation.id,
                signerId,
                signerName,
                signatureData,
                instant
            )
        )
        operation = operationRepository.getByIdOrThrow(operation.id)
        val signatureEntity = operation.signatures.first()

        transferCommandService.deleteSignature(
            TransferCommand.DeleteSignature(
                operation.transfer.id,
                operation.id,
                signatureEntity.id
            )
        )

        assertThat(signatureRepository.getById(signatureEntity.id)).isNull()
    }

    @Test
    @Disabled
    fun `Delete signature idempotency`() {
        var operation = testOperationHelper.createOperation()
        transferCommandService.createSignature(
            TransferCommand.CreateSignature(
                operation.transfer.id,
                operation.id,
                signerId,
                signerName,
                signatureData,
                instant
            )
        )
        operation = operationRepository.getByIdOrThrow(operation.id)
        val signatureEntity = operation.signatures.first()

        transferCommandService.deleteSignature(
            TransferCommand.DeleteSignature(
                operation.transfer.id,
                operation.id,
                signatureEntity.id
            )
        )

        transferCommandService.deleteSignature(
            TransferCommand.DeleteSignature(
                operation.transfer.id,
                operation.id,
                signatureEntity.id
            )
        )
    }

    @Test
    fun `When cancel transfer then change transfer status`() {
        val transfer = createTransfer()

        transferCommandService.cancel(TransferCommand.Cancel(transfer.id))

        val entity = transferRepository.getByIdOrThrow(transfer.id)
        assertThat(entity.status).isEqualTo(TransferStatus.CANCELLED)
        dbQueueTestUtil.assertQueueHasSize(QueueType.TRANSFER_CALLBACK, 4)
    }

    private fun createTransfer(): Transfer {
        val actorFrom = testOperationHelper.createActor("actorFrom", "SC", "apiKey1")
        val actorTo = testOperationHelper.createActor("actorTo", "COURIER", "apiKey2")

        val item1 = testOperationHelper.createItem("item1")
        val item2 = testOperationHelper.createItem("item2")

        return transferCommandService.create(TransferCommand.Create(
            actorFrom.id,
            actorTo.id,
            mapOf(Pair(item1.id, OperationItemStatus.SKIPPED), Pair(item2.id, OperationItemStatus.RECEIVED)),
            localDate = LocalDate.now(),
            null
        ))
    }
}
