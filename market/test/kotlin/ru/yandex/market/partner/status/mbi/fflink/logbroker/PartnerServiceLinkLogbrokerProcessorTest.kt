package ru.yandex.market.partner.status.mbi.fflink.logbroker

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.core.util.DateTimes
import ru.yandex.market.mbi.partner.service.link.PartnerServiceLinkOuterClass.PartnerServiceLink
import ru.yandex.market.mbi.partner.service.link.PartnerServiceLinkOuterClass.PartnerServiceLink.LinkUpdateType
import ru.yandex.market.partner.status.AbstractFunctionalTest
import java.time.Instant

/**
 * Тесты для [PartnerServiceLinkLogbrokerProcessor].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerServiceLinkLogbrokerProcessorTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var partnerServiceLinkLogbrokerProcessor: PartnerServiceLinkLogbrokerProcessor

    @Test
    @DbUnitDataSet(after = ["PartnerServiceLinkLogbrokerProcessorTest/new.after.csv"])
    fun `new ff link`() {
        val message = mockMessage({
            partnerId = 100L
            serviceId = 200L
            updatedAt = DateTimes.toTimestamp(Instant.now())
            updateType = LinkUpdateType.UPDATE_TYPE_UPDATE
        })
        partnerServiceLinkLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(after = ["PartnerServiceLinkLogbrokerProcessorTest/empty.after.csv"])
    fun `delete not existing link`() {
        val message = mockMessage({
            partnerId = 100L
            serviceId = 200L
            updatedAt = DateTimes.toTimestamp(Instant.now())
            updateType = LinkUpdateType.UPDATE_TYPE_DELETE
        })
        partnerServiceLinkLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(after = ["PartnerServiceLinkLogbrokerProcessorTest/new.after.csv"])
    fun `two events in single message`() {
        val now = Instant.now()
        val message = mockMessage(
            {
                partnerId = 100L
                serviceId = 200L
                updatedAt = DateTimes.toTimestamp(now)
                updateType = LinkUpdateType.UPDATE_TYPE_UPDATE
            },
            // Ивент с удалением линки идет после создания, но таймстемп удаления старше
            // Поэтому должны только создать линк и не делать ничего больше
            {
                partnerId = 100L
                serviceId = 200L
                updatedAt = DateTimes.toTimestamp(now.minusSeconds(1000L))
                updateType = LinkUpdateType.UPDATE_TYPE_DELETE
            }
        )
        partnerServiceLinkLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerServiceLinkLogbrokerProcessorTest/outdated.before.csv"],
        after = ["PartnerServiceLinkLogbrokerProcessorTest/deleted.after.csv"]
    )
    fun `delete link`() {
        val message = mockMessage({
            partnerId = 100L
            serviceId = 200L
            updatedAt = DateTimes.toTimestamp(Instant.now())
            updateType = LinkUpdateType.UPDATE_TYPE_DELETE
        })
        partnerServiceLinkLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerServiceLinkLogbrokerProcessorTest/outdated.before.csv"],
        after = ["PartnerServiceLinkLogbrokerProcessorTest/updated.after.csv"]
    )
    fun `update outdated`() {
        val message = mockMessage({
            partnerId = 100L
            serviceId = 200L
            updatedAt = DateTimes.toTimestamp(Instant.now())
            updateType = LinkUpdateType.UPDATE_TYPE_UPDATE
        })
        partnerServiceLinkLogbrokerProcessor.process(message)
    }

    private fun mockMessage(vararg builders: PartnerServiceLink.Builder.() -> Unit): MessageBatch {
        val links = builders.map { PartnerServiceLink.newBuilder().apply(it).build() }
            .map { MessageData(it.toByteArray(), 0, null) }
        return MessageBatch("", 0, links)
    }
}
