package ru.yandex.market.partner.status.mbi.fflink.yt.mapper

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.partner.status.AbstractFunctionalTest
import ru.yandex.market.partner.status.mbi.fflink.db.PartnerServiceLink
import ru.yandex.market.partner.status.mbi.fflink.yt.PartnerServiceLinkYtEntry
import java.time.Instant

/**
 * Тесты для [PartnerServiceLinkYtMapper].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerServiceLinkYtMapperTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var partnerServiceLinkYtMapper: PartnerServiceLinkYtMapper

    @Test
    fun `without db id`() {
        val now = Instant.now()
        val actual = partnerServiceLinkYtMapper.map(
            PartnerServiceLinkYtEntry(partnerId = 100L, serviceId = 200L, updatedAt = now)
        )

        Assertions.assertThat(actual)
            .isEqualTo(PartnerServiceLink(0L, 100L, 200L, now, version = 0L))
    }
}
