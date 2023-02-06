package ru.yandex.market.partner.status.mbi.partner.logbroker

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.core.util.DateTimes
import ru.yandex.market.mbi.data.GeneralData
import ru.yandex.market.mbi.data.PartnerDataOuterClass
import ru.yandex.market.partner.status.AbstractFunctionalTest
import java.time.Instant
import ru.yandex.market.mbi.data.PartnerDataOuterClass.PartnerType as ProtoPartnerType

/**
 * Тесты для [PartnerLogbrokerProcessor].
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class PartnerLogbrokerProcessorTest : AbstractFunctionalTest() {

    @Autowired
    private lateinit var partnerLogbrokerProcessor: PartnerLogbrokerProcessor

    @Test
    @DbUnitDataSet(after = ["PartnerLogbrokerProcessorTest/empty.after.csv"])
    fun `skip unsupported partner type`() {
        val message = mockMessage {
            partnerId = 100L
            type = ProtoPartnerType.TPL_PARTNER
            generalInfo {
                updatedAt = DateTimes.toTimestamp(Instant.now())
                actionType = GeneralData.ActionType.CREATE
            }
        }
        partnerLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(after = ["PartnerLogbrokerProcessorTest/new.after.csv"])
    fun `new partners`() {
        val message = mockMessage {
            partnerId = 100L
            type = ProtoPartnerType.SHOP
            generalInfo {
                updatedAt = DateTimes.toTimestamp(Instant.now())
                actionType = GeneralData.ActionType.CREATE
            }
        }
        partnerLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(after = ["PartnerLogbrokerProcessorTest/empty.after.csv"])
    fun `delete not existing partners`() {
        val message = mockMessage {
            partnerId = 100L
            type = ProtoPartnerType.SHOP
            generalInfo {
                updatedAt = DateTimes.toTimestamp(Instant.now())
                actionType = GeneralData.ActionType.DELETE
            }
        }
        partnerLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerLogbrokerProcessorTest/outdated.before.csv"],
        after = ["PartnerLogbrokerProcessorTest/deleted.after.csv"]
    )
    fun `delete partner`() {
        val message = mockMessage {
            partnerId = 100L
            type = ProtoPartnerType.SHOP
            generalInfo {
                updatedAt = DateTimes.toTimestamp(Instant.now())
                actionType = GeneralData.ActionType.DELETE
            }
        }
        partnerLogbrokerProcessor.process(message)
    }

    @Test
    @DbUnitDataSet(
        before = ["PartnerLogbrokerProcessorTest/outdated.before.csv"],
        after = ["PartnerLogbrokerProcessorTest/updated.after.csv"]
    )
    fun `update outdated`() {
        val message = mockMessage {
            partnerId = 100L
            type = ProtoPartnerType.SHOP
            generalInfo {
                updatedAt = DateTimes.toTimestamp(Instant.now())
                actionType = GeneralData.ActionType.UPDATE
            }
        }
        partnerLogbrokerProcessor.process(message)
    }

    private fun mockMessage(builder: PartnerDataOuterClass.PartnerData.Builder.() -> Unit): MessageBatch {
        val data = PartnerDataOuterClass.PartnerData.newBuilder().apply(builder).build()
            .let { MessageData(it.toByteArray(), 0, null) }
        return MessageBatch("", 0, listOf(data))
    }

    private fun PartnerDataOuterClass.PartnerData.Builder.generalInfo(builder: GeneralData.GeneralDataInfo.Builder.() -> Unit) {
        val result = GeneralData.GeneralDataInfo.newBuilder().apply(builder).build()
        this.generalInfo = result
    }
}
