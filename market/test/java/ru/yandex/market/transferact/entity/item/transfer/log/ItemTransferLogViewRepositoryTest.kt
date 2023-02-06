package ru.yandex.market.transferact.entity.item.transfer.log

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.operation.item.OperationItemStatus
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.market.transferact.utils.TestOperationHelper
import java.math.BigDecimal
import java.time.Instant

class ItemTransferLogViewRepositoryTest : AbstractTest() {

    @Autowired
    lateinit var itemTransferLogViewRepository: ItemTransferLogViewRepository

    @Autowired
    lateinit var testOperationHelper: TestOperationHelper

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `When findAllByTransferId then return ItemTransferLogs`() {
        val transfer = testOperationHelper.createTransfer(TransferStatus.CLOSED, closedAt = Instant.now())

        val actorCourier =
            testOperationHelper.createActor(name = "Кошелев Владимир Александрович", companyName = "ООО \"Воробушек\"")
        val actorWarehouse = testOperationHelper.createActor("12", "WAREHOUSE", "124", "ООО \"Интер\"")

        val operationReceive = testOperationHelper.createOperation(
            transfer = transfer, actor = actorCourier, items = mapOf(
                Pair(testOperationHelper.createItem(
                    externalId = "1",
                    declaredCost = BigDecimal.valueOf(1000),
                    placeCount = 2
                ), OperationItemStatus.RECEIVED),
                Pair(testOperationHelper.createItem(
                    externalId = "2",
                    declaredCost = BigDecimal.valueOf(123),
                    placeCount = 1
                ), OperationItemStatus.RECEIVED),
                Pair(testOperationHelper.createItem(
                    externalId = "3",
                    declaredCost = BigDecimal.valueOf(7459),
                    placeCount = 2
                ), OperationItemStatus.RECEIVED),
                Pair(testOperationHelper.createItem(
                    externalId = "4",
                    declaredCost = BigDecimal.valueOf(92305),
                    placeCount = 1
                ), OperationItemStatus.SKIPPED),
                Pair(testOperationHelper.createItem(
                    externalId = "5",
                    declaredCost = BigDecimal.valueOf(50),
                    placeCount = 3
                ), OperationItemStatus.SKIPPED),
                Pair(testOperationHelper.createItem(
                    externalId = "6",
                    declaredCost = BigDecimal.valueOf(90),
                    placeCount = 8
                ), OperationItemStatus.RECEIVED)
            )
        )

        val operationProvide = testOperationHelper.createOperation(
            operationType = OperationType.PROVIDE,
            transfer = transfer,
            actor = actorWarehouse,
            items = mapOf(
                Pair(testOperationHelper.createItem(
                    externalId = "7",
                    declaredCost = BigDecimal.valueOf(1000),
                    placeCount = 2
                ), OperationItemStatus.RECEIVED),
                Pair(testOperationHelper.createItem(
                    externalId = "8",
                    declaredCost = BigDecimal.valueOf(123),
                    placeCount = 1
                ), OperationItemStatus.SKIPPED)
            )
        )

        testOperationHelper.createSignature(
            "test",
            "test",
            "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3",
            operationReceive
        )

        testOperationHelper.createSignature(
            "test",
            "test",
            "8d23cf6c86e834a7aa6eded54c26ce2bb2e74903538c61bdd5d2197997ab2f72",
            operationProvide
        )

        testOperationHelper.createDocument(transfer = transfer)

        val result = itemTransferLogViewRepository.findAllByTransferId(transfer.id)

        Assertions.assertThat(result.map { it.itemExternalId }.toSet())
            .isEqualTo(hashSetOf("1", "2", "3", "6"))
        Assertions.assertThat(result.size).isEqualTo(8)
    }
}
