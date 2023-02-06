package ru.yandex.market.transferact.entity.item.transfer.log

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.operation.OperationType
import java.time.Instant

class ItemTransferLogRepositoryTest : AbstractTest() {
    @Autowired
    lateinit var itemTransferLogRepository: ItemTransferLogRepository

    @Test
    fun `When findByExternalId then return Item`() {
        val id = ItemTransferLogViewNaturalKey(1L, 1L, 1L, OperationType.PROVIDE)
        val itemTransfer = ItemTransferLogEntity(
            id, "null.pdf", Instant.now(), "actorType", "actorExternalId", "itemExternalId"
        )

        itemTransferLogRepository.save(itemTransfer)

        val foundItem = itemTransferLogRepository.findById(id)

        Assertions.assertThat(foundItem).isNotNull
        Assertions.assertThat(foundItem.get()).isEqualTo(itemTransfer)
    }

    @Test
    fun `When findByExternalId then return null`() {
        val id = ItemTransferLogViewNaturalKey(1L, 1L, 1L, OperationType.PROVIDE)
        val itemTransfer = ItemTransferLogEntity(
            id, "null.pdf", Instant.now(), "actorType", "actorExternalId", "itemExternalId"
        )

        itemTransferLogRepository.save(itemTransfer)

        val foundItem =
            itemTransferLogRepository.findById(ItemTransferLogViewNaturalKey(2L, 1L, 1L, OperationType.PROVIDE))

        Assertions.assertThat(foundItem.isEmpty).isTrue
    }
}
