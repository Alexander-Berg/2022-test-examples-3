package ru.yandex.market.partner.status.logistics.segment.yt.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.logistics.segment.db.CombinatorActiveSegment
import ru.yandex.market.partner.status.logistics.segment.yt.CombinatorActiveSegmentYtEntry
import ru.yandex.market.partner.status.logistics.segment.yt.CombinatorDeliveryServiceYtEntryType
import ru.yandex.market.partner.status.wizard.model.partner.DeliveryServiceType
import java.time.Instant

/**
 * Тесты для [CombinatorActiveSegmentYtMapper].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class CombinatorActiveSegmentYtMapperTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var combinatorActiveSegmentYtMapper: CombinatorActiveSegmentYtMapper

    @Test
    fun `new entity`() {
        val updatedAt = Instant.now()
        val entry = CombinatorActiveSegmentYtEntry(100L, CombinatorDeliveryServiceYtEntryType.DELIVERY, updatedAt)
        val result = combinatorActiveSegmentYtMapper.map(entry)

        Assertions.assertThat(result)
            .isEqualTo(CombinatorActiveSegment(
                id = 0L,
                serviceId = 100L,
                serviceType = DeliveryServiceType.CARRIER,
                updatedAt = updatedAt,
                version = 0L
            ))
    }

    @Test
    fun `check all delivery services types`() {
        CombinatorDeliveryServiceYtEntryType.values().forEach {
            val entry = CombinatorActiveSegmentYtEntry(100L, it, Instant.now())
            val result = combinatorActiveSegmentYtMapper.map(entry)
            Assertions.assertThat(result)
                .isNotNull
        }
    }
}
